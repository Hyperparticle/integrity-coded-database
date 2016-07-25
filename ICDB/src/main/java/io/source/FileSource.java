package io.source;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * DataSource that obtains data from a CSV file.
 *
 * Created on 7/24/2016
 * @author Dan Kondratyuk
 */
public class FileSource implements DataSource {

    /**
     * @return a stream that gets the data line by line
     */
    public static Stream<List<String>> stream(File dataFile) {
        return new FileSource(dataFile).stream();
    }

    private final File dataFile;

    private static final Logger logger = LogManager.getLogger();

    private FileSource(File dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    public Stream<List<String>> stream() {
        try {
            final Reader reader = new FileReader(dataFile);
            final CsvListReader csvReader = new CsvListReader(reader, CsvPreference.STANDARD_PREFERENCE);

            // Generate a stream that reads the CSV file line by line
            return Stream.generate(() -> propagate(csvReader::read))
                .limit(Files.lines(dataFile.toPath()).count()) // Read to the end
                .onClose(() -> { // Close all readers
                    close(csvReader);
                    close(reader);
                });
        } catch (IOException e) {
            logger.error("Unable to convert file {}: {}", dataFile.getName(), e.getMessage());
        }

        return Stream.empty();
    }

    /**
     * Nukes all checked exceptions!
     */
    private static <V> V propagate(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
