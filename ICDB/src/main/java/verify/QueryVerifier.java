package verify;

import crypto.AlgorithmType;
import crypto.CodeGen;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import crypto.signer.RSASHA1Signer;
import io.DBConnection;
import io.source.DBSource;
import io.source.DataSource;
import main.ICDBTool;
import main.args.config.UserConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import parse.ICDBQuery;
import stats.RunStatistics;
import stats.Statistics;
import verify.serial.AbstractIcrl;
import verify.serial.Icrl;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *     Verifies a SQL query
 * </p>
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public abstract class QueryVerifier {

    private final DBConnection icdb;
    private final  UserConfig userConfig;
    protected final CodeGen codeGen;
    protected crypto.Key key;

    public final AbstractIcrl icrl = Icrl.Companion.getIcrl();

    protected final DSLContext icdbCreate;
    protected final StringBuilder errorStatus = new StringBuilder();

    public final Map<String, Double> columnComputedValue=new ConcurrentHashMap<String, Double>();
    public final Map<String, Integer> avgOperationCount=new ConcurrentHashMap<String, Integer>();

    protected final List<Integer> testTotal= new ArrayList<>();
    protected final int threads;
    private final DataSource.Fetch fetch;
    protected final RunStatistics statistics;

    private static final Logger logger = LogManager.getLogger();

    protected BigInteger message = BigInteger.valueOf(1);
    protected BigInteger sig = BigInteger.valueOf(1);

    public QueryVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch, RunStatistics statistics) {
        this.icdb = icdb;
        this.userConfig=dbConfig;
        this.codeGen = dbConfig.codeGen;
        this.threads = threads;
        this.fetch = fetch;
        this.statistics = statistics;
        key=codeGen.getKey();
        this.icdbCreate = icdb.getCreate();
    }

    /**
     * Executes and verifies a given query
     * @return true if the query is verified
     */
    public boolean verify(ICDBQuery icdbQuery) {
        logger.debug("Using fetch type: {}", fetch);

        Stopwatch totalQueryVerificationTime = Stopwatch.createStarted();

        logger.info("Verify Query: {}", icdbQuery.getVerifyQuery());

        Stopwatch queryFetchTime = Stopwatch.createStarted();
        Stream<Record> records = DBSource.stream(icdb, icdbQuery.getVerifyQuery(), fetch);

        statistics.setDataFetchTime(queryFetchTime.elapsed(ICDBTool.TIME_UNIT));
        logger.debug("Data fetch time: {}", statistics.getDataFetchTime());
        Stopwatch queryVerificationTime = Stopwatch.createStarted();

        boolean verified = verifyRecords(records,  icdbQuery);
        records.close();

        statistics.setVerificationTime(queryVerificationTime.elapsed(ICDBTool.TIME_UNIT));
        logger.debug("Data verification time: {}", statistics.getVerificationTime());
        logger.debug("Total query verification time: {}", totalQueryVerificationTime.elapsed(ICDBTool.TIME_UNIT));

        if (codeGen.getAlgorithm()== AlgorithmType.RSA_AGGREGATE && verified){

                RSASHA1Signer signer=new RSASHA1Signer(key.getModulus(),key.getExponent());
                BigInteger newsig= new BigInteger(signer.computeRSA(message.toByteArray()));
                if (newsig.equals(sig)){
                    logger.info("ICDB aggregate sign verified");
                    return true;
                }else
                    return false;

        }


        return verified;
    }

    public void execute(ICDBQuery icdbQuery) {


        if (icdbQuery.isAggregateQuery) {
            //compute aggregate average operation if any
            Stopwatch aggregateQueryExecutionTime = Stopwatch.createStarted();
            if (avgOperationCount.size()!=0){
                avgOperationCount.entrySet().forEach(entry-> {
                    DecimalFormat df = new DecimalFormat("#.0000");
                    columnComputedValue.put(entry.getKey(),Double.valueOf(df.format(columnComputedValue.get(entry.getKey())/entry.getValue())));
                    logger.debug("aggregate operation value: {}",columnComputedValue.get(entry.getKey()) );
                        }
                );
            }

           if (icdbQuery.executeandmatch(icdbCreate,columnComputedValue)){
            logger.info("aggregate operation matched");
               logger.debug("Total Aggregate Operation time: {}", statistics.getAggregateOperationTime());
               logger.debug("Aggregate query execution and match Time: {}", aggregateQueryExecutionTime.elapsed(ICDBTool.TIME_UNIT));

           }
        }else{

            Stopwatch queryExecutionTime = Stopwatch.createStarted();

            icdbQuery.execute(icdbCreate);

            statistics.setExecutionTime(queryExecutionTime.elapsed(ICDBTool.TIME_UNIT));
            logger.debug("Total query execution time: {}", statistics.getExecutionTime());
        }


    }


    protected long verifyCount = 0;
    /**
     * Executes and verifies a given query given a cursor into the data records
     * @return true if the query is verified
     */
    private boolean verifyRecords(Stream<Record> records, ICDBQuery icdbQuery) {
        final ForkJoinPool threadPool = threads < 1 ? new ForkJoinPool() : new ForkJoinPool(threads);

        logger.debug("Using {} thread(s)", threadPool.getParallelism());
        verifyCount = 0;
        List<CompletableFuture<Boolean>> futures;
        if (codeGen.getAlgorithm()== AlgorithmType.RSA_AGGREGATE){
            futures = records.map(record -> CompletableFuture.supplyAsync(() -> aggregateVerifyRecord(record, icdbQuery), threadPool))
                    .collect(Collectors.toList());
        }else {
            futures = records.map(record -> CompletableFuture.supplyAsync(() -> verifyRecord(record, icdbQuery), threadPool))
                    .collect(Collectors.toList());
        }



        // Asynchronously verify all signatures
        return futures.stream()
            .allMatch(f -> {
                try {
                    statistics.setQueryFetchSize(++verifyCount);
                    return f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    protected abstract boolean verifyRecord(Record record, ICDBQuery icdbQuery);

    //verification by using RSA homomorphic multiplication
    protected abstract boolean aggregateVerifyRecord(Record record, ICDBQuery icdbQuery) ;

    /**
     * Verifies data and serial number by regenerating the signature
     * @param serial the serial number
     * @param signature the original signature
     * @param data the data to verify
     * @return true if the regenerated signature matches the original signature
     */
    protected boolean verifyData(final long serial, final byte[] signature, final String data) {
        final byte[] serialBytes = ByteBuffer.allocate(8).putLong(serial).array();
        final byte[] dataBytes = data.getBytes(Charsets.UTF_8);

        final byte[] allBytes = ArrayUtils.addAll(dataBytes, serialBytes);

        final boolean serialVerified = icrl.contains(serial);
        final boolean signatureVerified = codeGen.verify(allBytes, signature);
        return serialVerified && signatureVerified;
    }

    /**
     * @return An error message, if it exists
     */
    public String getError() {
        return errorStatus.toString();
    }


    protected void computeAggregateOperation(ICDBQuery icdbQuery,Record record){
        icdbQuery.columnOperation.entrySet().forEach(entry -> {
            String ColumnName=((String) entry.getKey()).substring(((String)entry.getKey()).indexOf("(") + 1, ((String)entry.getKey()).indexOf(")"));
            if (entry.getValue().equalsIgnoreCase("SUM")){
                operateSum(record,entry,ColumnName);
            }else if (entry.getValue().equalsIgnoreCase("MAX")){
                operateMax(record,entry,ColumnName);
            }else if (entry.getValue().equalsIgnoreCase("MIN")){
                operateMin(record,entry,ColumnName);
            }else if (entry.getValue().equalsIgnoreCase("AVG")){
                operateAvg(record,entry,ColumnName);
            }
        });
    }

    protected void operateSum(Record record, Map.Entry entry,String ColumnName){
        if (columnComputedValue.get(entry.getKey())!=null){
            Double oldValue=columnComputedValue.get(entry.getKey());
            columnComputedValue.put((String)entry.getKey(),oldValue+ ((Integer)record.get(ColumnName)).doubleValue());
        }else {
            columnComputedValue.put((String)entry.getKey(), ((Integer)record.get(ColumnName)).doubleValue());
        }
    }

    protected void operateMax(Record record, Map.Entry entry,String ColumnName){

        if (columnComputedValue.get(entry.getKey())!=null){
            Double oldValue=columnComputedValue.get((String)entry.getKey());
            if (Double.parseDouble(record.get(ColumnName).toString()) > oldValue) {
                columnComputedValue.put((String)entry.getKey(),((Integer)record.get(ColumnName)).doubleValue());
            }

        }else {
            columnComputedValue.put((String)entry.getKey(), ((Integer)record.get(ColumnName)).doubleValue());
        }
    }

    protected void operateMin(Record record, Map.Entry entry,String ColumnName){

        if (columnComputedValue.get(entry.getKey())!=null){
            Double oldValue=columnComputedValue.get((String)entry.getKey());
            if (Double.parseDouble(record.get(ColumnName).toString()) < oldValue) {
                columnComputedValue.put((String)entry.getKey(), ((Integer)record.get(ColumnName)).doubleValue());
            }

        }else {
            columnComputedValue.put((String)entry.getKey(), ((Integer)record.get(ColumnName)).doubleValue());
        }
    }

    protected void operateAvg(Record record, Map.Entry entry,String ColumnName){

        if (columnComputedValue.get(entry.getKey())!=null){
            Double oldValue=columnComputedValue.get((String)entry.getKey());
            columnComputedValue.put((String)entry.getKey(), ((((Integer)record.get(ColumnName)).doubleValue())+oldValue));

        }else {
            columnComputedValue.put((String)entry.getKey(),  ((Integer)record.get(ColumnName)).doubleValue());
        }
        //count the total operation for avg calculation
        if (avgOperationCount.get(entry.getKey())==null)
            avgOperationCount.put((String)entry.getKey(),1);
        else
            avgOperationCount.put((String)entry.getKey(),avgOperationCount.get((String)entry.getKey())+1);
    }



}
