package verify;

import io.DBConnection;
import io.Format;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Cursor;
import org.jooq.Field;
import org.jooq.Record;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Executes an OCT query and verifies data integrity.
 *
 * Created on 7/16/2016
 * @author Dan Kondratyuk
 */
public class OCTQueryVerifier extends QueryVerifier {

    private static final Logger logger = LogManager.getLogger();

    public OCTQueryVerifier(DBConnection icdb, UserConfig dbConfig) {
        super(icdb, dbConfig);
    }

    protected boolean verify(Stream<Record> records) {
        List<CompletableFuture<Boolean>> futures = records
                .map(record -> CompletableFuture.supplyAsync(() -> {
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
                }))
                .collect(Collectors.toList());

        // Asynchronously verify all signatures
        return futures.stream()
            .allMatch(f -> {
                try {
                    return f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    public String getError() {
        return errorStatus.toString();
    }

}
