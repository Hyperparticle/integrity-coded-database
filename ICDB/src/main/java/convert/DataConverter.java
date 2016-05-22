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
import java.util.Scanner;
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

    private final Path dataPath;
    private final File keyFile;
    private final Path convertPath;

    private final CipherType cipherType;
    private final Granularity granularity;
    private final String delimiter;

    public DataConverter(ConvertDataCommand command) {
        this.dataPath = Paths.get(command.dataPath);
        this.keyFile = Paths.get(command.keyPath).toFile();
        this.convertPath = Paths.get(command.convertPath);

        this.cipherType = command.cipherType;
        this.granularity = command.granularity;
        this.delimiter = command.delimiter;
    }

    public void parse() {
        System.out.println("Converting data...");
        List<File> dataFiles = getFiles(dataPath);
        String key = getKey(keyFile);

        System.out.println("Conversion complete.");
    }

    private static List<File> getFiles(Path path) {
        try {
            return Files.walk(path)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Unable to obtain files under path " + path);
            System.exit(1);
        }

        return null;
    }

    private static String getKey(File keyFile) {
        try {
            Scanner fileScan = new Scanner(keyFile);
            return fileScan.nextLine();
        } catch (IOException e) {
            System.err.println("Error: unable to open keyfile " + keyFile);
            System.exit(1);
        }

        return null;
    }

}
