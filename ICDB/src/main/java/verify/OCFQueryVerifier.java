package verify;

import io.DBConnection;
import io.source.DataSource;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import stats.RunStatistics;

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
    protected boolean verifyRecord(Record record) {
        final int dataSize = record.size() / 3;
        for (int i = 0; i < dataSize; i++) {
            final long serial = (long) record.get(dataSize + 2*i + 1);
            final byte[] signature = (byte[]) record.get(dataSize + 2*i);
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
        }

        return true;
    }

}
