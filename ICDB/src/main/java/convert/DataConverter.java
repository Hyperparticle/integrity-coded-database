package convert;

import main.args.ConvertDataCommand;
import main.args.option.CipherType;
import main.args.option.Granularity;

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

    private final String dataPath;
    private final String keyPath;
    private final String convertPath;

    private final CipherType cipherType;
    private final Granularity granularity;
    private final String delimiter;

    public DataConverter(ConvertDataCommand command) {
        this.dataPath = command.dataPath;
        this.keyPath = command.keyPath;
        this.convertPath = command.convertPath;

        this.cipherType = command.cipherType;
        this.granularity = command.granularity;
        this.delimiter = command.delimiter;
    }

    public void parse() {
    }

//    public void parseFiles() {
//        if (dirs == null) { return; }
//
//        FileConverter converter = new FileConverter();
//
//        List<File> fileList = fileList(dirs);
//        System.out.println();
//    }
//
//    private static List<File> fileList(List<String> dirs) {
//        return dirs.stream()
//                .map(Paths::get)
//                .flatMap(path -> {
//                    try {
//                        return Files.walk(path);
//                    } catch (IOException e) {
//                        return Stream.empty();
//                    }
//                })
//                .filter(Files::isRegularFile)
//                .map(Path::toFile)
//                .collect(Collectors.toList());
//    }

}
