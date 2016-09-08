package verify;

import crypto.CodeGen;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
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
import verify.serial.Icrl;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final CodeGen codeGen;

    private final Icrl icrl = Icrl.Companion.getIcrl();

    protected final DSLContext icdbCreate;
    protected final StringBuilder errorStatus = new StringBuilder();

    public final Map<String, Double> columnComputedValue=new ConcurrentHashMap<String, Double>();
    public final Map<String, Integer> avgOperationCount=new ConcurrentHashMap<String, Integer>();

    protected final List<Integer> testTotal= new ArrayList<>();

    private static final Logger logger = LogManager.getLogger();

    public QueryVerifier(DBConnection icdb, UserConfig dbConfig) {
        this.icdb = icdb;
        this.codeGen = dbConfig.codeGen;

        this.icdbCreate = icdb.getCreate();
    }

    /**
     * Executes and verifies a given query
     * @return true if the query is verified
     */
    public boolean verify(ICDBQuery icdbQuery) {
        Stopwatch queryVerificationTime = Stopwatch.createStarted();

        logger.info("Verify Query: {}", icdbQuery.getVerifyQuery());

        Stream<Record> records = DBSource.stream(icdb, icdbQuery.getVerifyQuery(), DataSource.Fetch.LAZY);
        boolean verified = verify(records,icdbQuery);
       // System.out.print(columnComputedValue.get("salary"));
        System.out.print(testTotal);
        records.close();

        logger.debug("Total query verification time: {}", queryVerificationTime.elapsed(ICDBTool.TIME_UNIT));

        return verified;
    }

    public void execute(ICDBQuery icdbQuery) {
        Stopwatch queryExecutionTime = Stopwatch.createStarted();

        if (icdbQuery.isAggregateQuery) {
            //compute aggregate average operation if any
            if (avgOperationCount.size()!=0){
                avgOperationCount.entrySet().forEach(entry-> {
                    columnComputedValue.put(entry.getKey(),columnComputedValue.get(entry.getKey())/entry.getValue());
                        }
                );
            }

           if (icdbQuery.executeandmatch(icdbCreate,columnComputedValue)){
            logger.info("aggregate operation matched");
           }
        }
        else icdbQuery.execute(icdbCreate);

        logger.debug("Total query execution time: {}", queryExecutionTime.elapsed(ICDBTool.TIME_UNIT));
    }

    /**
     * Executes and verifies a given query given a cursor into the data records
     * @return true if the query is verified
     */
    protected abstract boolean verify(Stream<Record> records, ICDBQuery icdbQuery);

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
