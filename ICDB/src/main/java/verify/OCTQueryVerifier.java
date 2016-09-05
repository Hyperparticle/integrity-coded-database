package verify;

import io.DBConnection;
import io.Format;
import io.source.DataSource;
import main.args.config.UserConfig;
import org.jooq.Record;
import stats.RunStatistics;

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
    protected boolean verifyRecord(Record record) {
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

        return verified;
    }

}
