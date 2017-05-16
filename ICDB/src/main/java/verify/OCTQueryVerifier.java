package verify;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.math.BigIntegerMath;
import crypto.signer.RSASHA1Signer;
import io.DBConnection;
import io.Format;
import io.result.Fetch;
import io.source.DataSource;
import main.ICDBTool;
import main.args.config.UserConfig;
import org.apache.commons.lang3.ArrayUtils;
import org.jooq.Field;
import org.jooq.Record;
import stats.RunStatistics;

import parse.ICDBQuery;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Executes an OCT query and verifies data integrity.
 *
 * Created on 7/16/2016
 * @author Dan Kondratyuk
 */
public class OCTQueryVerifier extends QueryVerifier {

    public OCTQueryVerifier(DBConnection icdb, UserConfig dbConfig, int threads, Fetch fetch, RunStatistics statistics) {
        super(icdb, dbConfig, threads, fetch, statistics);
    }

    public String getError() {
        return errorStatus.toString();
    }

    @Override
    protected boolean verifyRecord(Record record, ICDBQuery icdbQuery) {
        final StringBuilder builder = new StringBuilder();

        //   Field<?>[] Serial = record.fields(Format.SERIAL_COLUMN);
        //  Field<?> IC = record.field(Format.IC_COLUMN);

        int index = 0;
        boolean verified = false;
        boolean isSkip = false;
        for (Field<?> attr : record.fields()) {

            if (!attr.getName().equals("ic") && !attr.getName().equals("serial")) {
                final Object value = record.get(index);
                builder.append(value);

                index++;
                if (isSkip)
                    isSkip = false;

            } else {
                if (isSkip)
                    continue;

                final byte[] signature = (byte[]) record.get(index);
                final long serial = (long) record.get(index + 1);

                final String data = builder.toString();
                verified = verifyData(serial, signature, data);

                builder.setLength(0);
                if (!verified) {
                    errorStatus.append("\n")
                            .append(record.toString())
                            .append("\n");
                    break;
                }

                if (record.size() == index + 2)
                    break;
                else {
                    isSkip = true;
                    index += 2;
                }
            }

//        final StringBuilder builder = new StringBuilder();
//
//        for (int i = 0; i < record.size() - 2; i++) {
//            final Object value = record.get(i);
//            builder.append(value);
//        }
//
//        final long serial = (long) record.get(Format.SERIAL_COLUMN);
//        final byte[] signature = (byte[]) record.get(Format.IC_COLUMN);
//        final String data = builder.toString();
//
//        final boolean verified = verifyData(serial, signature, data);
//
//        if (!verified) {
//            errorStatus.append("\n")
//                    .append(record.toString())
//                    .append("\n");
//        }

        }

        if (icdbQuery.isAggregateQuery) {
            Stopwatch aggregateOperationTime = Stopwatch.createStarted();
            computeAggregateOperation(icdbQuery, record);
            statistics.setAggregateOperationTime( statistics.getAggregateOperationTime()+aggregateOperationTime.elapsed(ICDBTool.TIME_UNIT));
        }

        return verified;
    }

    @Override
    protected boolean aggregateVerifyRecord(Record record, ICDBQuery icdbQuery) {

        final StringBuilder builder = new StringBuilder();

        int index = 0;
        boolean isSkip = false;
        for (Field<?> attr : record.fields()) {

            if (!attr.getName().equals("ic") && !attr.getName().equals("serial")) {
                final Object value = record.get(index);
                builder.append(value);

                index++;
                if (isSkip)
                    isSkip = false;

            } else {
                if (isSkip)
                    continue;

                final byte[] signature = (byte[]) record.get(index);
                final long serial = (long) record.get(index + 1);
                final byte[] serialBytes = ByteBuffer.allocate(8).putLong(serial).array();

                final String data = builder.toString();
                final byte[] dataBytes = data.getBytes(Charsets.UTF_8);

                //check the ICRL
                if (!icrl.contains(serial)) {
                    return false;
                }

                final byte[] allData = ArrayUtils.addAll(dataBytes, serialBytes);

                RSASHA1Signer signer=new RSASHA1Signer(key.getModulus(),key.getExponent());
                message = message.multiply(new BigInteger(signer.computehash(allData))).mod(key.getModulus());
                sig = sig.multiply(new BigInteger(signature)).mod(key.getModulus());

                if (record.size() == index + 2)
                    break;
                else {
                    isSkip = true;
                    index += 2;
                }
            }

        }



        // final boolean verified = codeGen.verify(message.toByteArray(), sig.toByteArray());


        return  true;
    }


}
