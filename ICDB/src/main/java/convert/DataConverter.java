package convert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *     A DataConverter takes an input tuple and converts it into an ICDB tuple.
 * </p>
 * Created 5/8/2016
 *
 * @author Dan Kondratyuk
 */
public class DataConverter {

    private final String cipherType;
    private final String granularity;
    private final String delimiter;

    public DataConverter(String cipherType, String granularity, String delimiter) {
        this.cipherType = cipherType;
        this.granularity = granularity;
        this.delimiter = delimiter;
        // TODO: convert strings to enums?
    }

    public void parse(List<String> tuples) {
        if (tuples == null) { return; }

        // TODO: parse each tuple
    }

    public void parseFiles(List<String> dirs) {
        if (dirs == null) { return; }

        FileConverter converter = new FileConverter();

        List<File> fileList = fileList(dirs);
        System.out.println();
    }

    private static List<File> fileList(List<String> dirs) {
        return dirs.stream()
                .map(Paths::get)
                .flatMap(path -> {
                    try {
                        return Files.walk(path);
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

}
