package verify;

import convert.DBConnection;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Cursor;
import org.jooq.Record;

import java.util.Iterator;
import java.util.Spliterator;

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

    public OCFQueryVerifier(DBConnection icdb, UserConfig dbConfig) {
        super(icdb, dbConfig);
    }

    protected boolean verify(Cursor<Record> cursor) {
        Iterator<Record> it = cursor.iterator();

        while (it.hasNext()) {
            Record record = it.next();

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
            }
        }

        return true;

//        return cursor.stream().map(record -> {
//            final int dataSize = record.size() / 3;
//            for (int i = 0; i < dataSize; i++) {
//                final long serial = (long) record.get(dataSize + 2*i + 1);
//                final byte[] signature = (byte[]) record.get(dataSize + 2*i);
//                final String data = record.get(i).toString();
//
//                final boolean verified = verifyData(serial, signature, data);
//
//                if (!verified) {
//                    errorStatus.append("\n")
//                            .append(record.field(i))
//                            .append(" : ")
//                            .append(record.get(i))
//                            .append("\n");
//                    return false;
//                }
//            }
//
//            return true;
//        }).allMatch(verified -> verified);
    }

}
