package convert;

import cipher.CodeCipher;
import main.args.ConvertDataCommand;
import main.args.option.Granularity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

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
    private final Path outputPath;

    private final CodeCipher codeCipher;
    private final Granularity granularity;
    private final String delimiter;

    public DataConverter(ConvertDataCommand command) {
        this.dataPath = Paths.get(command.dataPath);
        this.keyFile = Paths.get(command.keyPath).toFile();
        this.outputPath = Paths.get(command.outputPath);

        this.granularity = command.granularity;
        this.delimiter = command.delimiter;

        // TODO: give correct data to cipher
        String key = getKey(keyFile);
        codeCipher = command.cipherType.getCipher(key);
    }

    public void parse() {
        System.out.println("Converting data...");
        List<File> dataFiles = getFiles(dataPath);

        if (dataFiles == null) { return; }
        if (dataFiles.isEmpty()) {
            System.out.println("Directory is empty; no files converted.");
            return;
        }

        dataFiles.forEach(this::convert);

        System.out.println("Conversion complete.");
    }

    private void convert(File dataFile) {
        try {
            String outputName = dataFile.getName().replace(".", "-icdb.");
            File outputFile = Paths.get(outputPath.toString(), outputName).toFile();

            Scanner dataScan = new Scanner(dataFile);
            FileWriter writer = new FileWriter(outputFile);
            StringBuilder builder = new StringBuilder();

            while (dataScan.hasNextLine()) {
                builder.setLength(0);

                if (granularity.equals(Granularity.TUPLE)) {
                    String line = dataScan.nextLine();
                    String code = codeCipher.encrypt(line);

                    builder.append(line)
                            .append(delimiter)
                            .append(code)
                            .append("\n");

                    writer.append(builder);
                } else {
                    StringTokenizer tokenizer = new StringTokenizer(dataScan.nextLine(), delimiter);

                    while (tokenizer.hasMoreTokens()) {
                        String next = tokenizer.nextToken();
                        String code = codeCipher.encrypt(next);

                        builder.append(next)
                                .append(delimiter)
                                .append(code)
                                .append(delimiter);
                    }

                    builder.setLength(builder.length() - delimiter.length());
                    builder.append("\n");

                    writer.append(builder);
                }
            }


            dataScan.close();
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to convert file " + dataFile);
        }
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
