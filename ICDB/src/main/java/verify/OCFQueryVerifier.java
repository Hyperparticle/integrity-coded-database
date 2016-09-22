package verify;

import com.google.common.base.Stopwatch;
import io.DBConnection;
import io.source.DataSource;
import main.ICDBTool;
import main.args.config.UserConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import parse.ICDBQuery;
import stats.RunStatistics;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * <p>
 *      Executes an OCF query and verifies data integrity
 * </p>
 * Created on 7/16/2016
 *
 * @author Dan Kondratyuk
 */
public class OCFQueryVerifier extends QueryVerifier {

    private static final Logger logger = LogManager.getLogger();

    public OCFQueryVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch, RunStatistics statistics) {
        super(icdb, dbConfig, threads, fetch, statistics);
    }

    @Override
    protected boolean verifyRecord(Record record, ICDBQuery icdbQuery) {
        final int dataSize = record.size() / 3;
        for (int i = 0; i < dataSize; i++) {
            final long serial = (long) record.get(dataSize + 2 * i + 1);
            final byte[] signature = (byte[]) record.get(dataSize + 2 * i);
            final String data = record.get(i).toString();

            final boolean verified = verifyData(serial, signature, data);

            if (!verified) {
                errorStatus.append("\n")
                        .append(record.field(i))
                        .append(" : ")
                        .append(record.get(i))
                        .append("\n");
                return false;
            }

            if (icdbQuery.isAggregateQuery) {
                Stopwatch aggregateOperationTime = Stopwatch.createStarted();
                computeAggregateOperation(icdbQuery, record);
                statistics.setAggregateOperationTime( statistics.getAggregateOperationTime()+aggregateOperationTime.elapsed(ICDBTool.TIME_UNIT));
            }
        }

        return true;
    }

    @Override
    protected boolean aggregateVerifyRecord(Record record, ICDBQuery icdbQuery) {
        final int dataSize = record.size() / 3;
        for (int i = 0; i < dataSize; i++) {
            final long serial = (long) record.get(dataSize + 2 * i + 1);
            final byte[] signature = (byte[]) record.get(dataSize + 2 * i);
            final String data = record.get(i).toString();
            final byte[] dataBytes = ByteBuffer.allocate(8).putLong(Long.valueOf(data)).array();

            final String serialString = Long.toString(serial);
            final byte[] serialBytes = ByteBuffer.allocate(8).putLong(serial).array();

            //check the ICRL
            if (!icrl.contains(serial)) {
                return false;
            }

            final byte[] allData = ArrayUtils.addAll(dataBytes, serialBytes);

            message = message.multiply(new BigInteger(allData)).multiply(key.getModulus());
            sig = sig.multiply(new BigInteger(signature)).multiply(key.getModulus());

        }


        final boolean verified = codeGen.verify(message.toByteArray(), sig.toByteArray());


        return verified;
    }
}
