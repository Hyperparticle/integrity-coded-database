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

    private final Icrl icrl;

    protected final DSLContext icdbCreate;
    protected final StringBuilder errorStatus = new StringBuilder();

    private static final Logger logger = LogManager.getLogger();

    public QueryVerifier(DBConnection icdb, UserConfig dbConfig) {
        this.icdb = icdb;
        this.codeGen = dbConfig.codeGen;

        this.icdbCreate = icdb.getCreate();
        this.icrl = Icrl.Companion.getInstance();
    }

    /**
     * Executes and verifies a given query
     * @return true if the query is verified
     */
    public boolean verify(ICDBQuery icdbQuery) {
        Stopwatch queryVerificationTime = Stopwatch.createStarted();

        logger.info("Verify Query: {}", icdbQuery.getVerifyQuery());

        Stream<Record> records = DBSource.stream(icdb, icdbQuery.getVerifyQuery(), DataSource.Fetch.LAZY);
        boolean verified = verify(records);
        records.close();

        logger.debug("Total query verification time: {}", queryVerificationTime.elapsed(ICDBTool.TIME_UNIT));

        return verified;
    }

    public void execute(ICDBQuery icdbQuery) {
        Stopwatch queryExecutionTime = Stopwatch.createStarted();

        icdbQuery.execute(icdbCreate);

        logger.debug("Total query execution time: {}", queryExecutionTime.elapsed(ICDBTool.TIME_UNIT));
    }

    /**
     * Executes and verifies a given query given a cursor into the data records
     * @return true if the query is verified
     */
    protected abstract boolean verify(Stream<Record> records);

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

}
