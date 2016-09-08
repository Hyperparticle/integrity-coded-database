package verify;

import io.DBConnection;
import io.Format;
import io.source.DataSource;
import main.args.config.UserConfig;
import org.jooq.Record;
import stats.RunStatistics;

import parse.ICDBQuery;

/**
 * Executes an OCT query and verifies data integrity.
 *
 * Created on 7/16/2016
 * @author Dan Kondratyuk
 */
public class OCTQueryVerifier extends QueryVerifier {

    public OCTQueryVerifier(DBConnection icdb, UserConfig dbConfig, int threads, DataSource.Fetch fetch, RunStatistics statistics) {
        super(icdb, dbConfig, threads, fetch, statistics);
    }

    public String getError() {
        return errorStatus.toString();
    }

    @Override
    protected boolean verifyRecord(Record record, ICDBQuery icdbQuery) {
//            final StringBuilder builder = new StringBuilder();
//
//            Field<?> Serial = record.field(Format.SERIAL_COLUMN);
//            Field<?> IC = record.field(Format.IC_COLUMN);
//
//            int index = 0;
//            boolean verified = false;
//            boolean isSkip = false;
//            for (Field<?> attr : record.fields()) {
//
//                if (!attr.getName().contains(Serial.getName()) && !attr.getName().contains(IC.getName())) {
//                    final Object value = record.get(index);
//                    builder.append(value);
//
//                    index++;
//                    if (isSkip)
//                        isSkip = false;
//
//                } else {
//                    if (isSkip)
//                        continue;
//
//                    final byte[] signature = (byte[]) record.get(index);
//                    final long serial = (long) record.get(index + 1);
//
//                    final String data = builder.toString();
//                    verified = verifyData(serial, signature, data);
//
//                    builder.setLength(0);
//                    if (!verified) {
//                        errorStatus.append("\n")
//                                .append(record.toString())
//                                .append("\n");
//                        break;
//                    }
//
//                    if (record.size() == index + 2)
//                        break;
//                    else {
//                        isSkip = true;
//                        index += 2;
//                    }
//                }

        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < record.size() - 2; i++) {
            final Object value = record.get(i);
            builder.append(value);
        }

        final long serial = (long) record.get(Format.SERIAL_COLUMN);
        final byte[] signature = (byte[]) record.get(Format.IC_COLUMN);
        final String data = builder.toString();

        final boolean verified = verifyData(serial, signature, data);

        if (!verified) {
            errorStatus.append("\n")
                    .append(record.toString())
                    .append("\n");
        }

        if (icdbQuery.isAggregateQuery) {
            computeAggregateOperation(icdbQuery, record);
        }

        return verified;
    }

}
