package verify;

import convert.DBConnection;
import convert.Format;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Cursor;
import org.jooq.Record;

/**
 * <p>
 *      Executes an OCT query and verifies data integrity
 * </p>
 * Created on 7/16/2016
 *
 * @author Dan Kondratyuk
 */
public class OCTQueryVerifier extends QueryVerifier {

    private static final Logger logger = LogManager.getLogger();

    public OCTQueryVerifier(DBConnection icdb, UserConfig dbConfig) {
        super(icdb, dbConfig);
    }

    protected boolean verify(Cursor<Record> cursor) {
        return cursor.stream().map(record -> {
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < record.size() - 2; i++) {
                final Object value = record.get(i);
                builder.append(value);
            }

            final long serial = (long) record.get(Format.SERIAL_COLUMN);
            final byte[] signature = (byte[]) record.get(Format.SVC_COLUMN);
            final String data = builder.toString();

            final boolean verified = verifyData(serial, signature, data);

            if (!verified) {
                errorStatus.append("\n")
                        .append(record.toString())
                        .append("\n");
            }

            return verified;
        }).allMatch(verified -> verified);
    }

    public String getError() {
        return errorStatus.toString();
    }

}