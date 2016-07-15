package verify;

import cipher.CodeGen;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import convert.DBConnection;
import convert.Format;
import main.args.ExecuteQueryCommand;
import main.args.config.ConfigArgs;
import main.args.config.UserConfig;
import main.args.option.Granularity;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.nio.ByteBuffer;

/**
 * <p>
 *     Verifies a SQL query
 * </p>
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public class QueryVerifier {

//    private final List<String> queries;
//    private final List<String> files;
//    private final String icdbName;
    private final String icdbQuery;
    private final DBConnection icdb;
    private final Granularity granularity;
    private final CodeGen codeGen;

    private final ICRL icrl;

    private StringBuilder errorStatus = new StringBuilder();

    private static final Logger logger = LogManager.getLogger();

    public QueryVerifier(ExecuteQueryCommand command, DBConnection icdb, UserConfig dbConfig, String icdbQuery) {
        this.icdbQuery = icdbQuery;
//        this.files = command.files;
        this.granularity = dbConfig.granularity;
        this.codeGen = dbConfig.codeGen;
//        this.icdbName = dbConfig.schema + Format.ICDB_SUFFIX;
        this.icdb = icdb;
        this.icrl = ICRL.getInstance();
    }

    public boolean verify() {
        Stopwatch queryVerificationTime = Stopwatch.createStarted();

        final DSLContext icdbCreate = DSL.using(icdb.getConnection(), SQLDialect.MYSQL);

        Cursor<Record> cursor = icdbCreate.fetchLazy(icdbQuery);
        boolean verified = granularity.equals(Granularity.TUPLE) ?
                verifyOCT(cursor) :
                verifyOCF(cursor);
        cursor.close();

        logger.debug("Total query verification time: {}", queryVerificationTime);
        return verified;
    }

    private boolean verifyOCT(Cursor<Record> cursor) {
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

    private boolean verifyOCF(Cursor<Record> cursor) {
        return cursor.stream().map(record -> {
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
        }).allMatch(verified -> verified);
    }

    private boolean verifyData(final long serial, final byte[] signature, final String data) {
        final byte[] serialBytes = ByteBuffer.allocate(8).putLong(serial).array();
        final byte[] dataBytes = data.getBytes(Charsets.UTF_8);

        final byte[] allBytes = ArrayUtils.addAll(dataBytes, serialBytes);

        final boolean serialVerified = icrl.contains(serial);
        final boolean signatureVerified = codeGen.verify(allBytes, signature);
        return serialVerified && signatureVerified;
    }

    public String getError() {
        return errorStatus.toString();
    }

}
