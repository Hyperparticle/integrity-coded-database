package convert;

import main.args.ConvertDataCommand;
import main.args.option.Granularity;
import main.args.option.AlgorithmType;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private final AlgorithmType algorithmType;

    private final Granularity granularity;
    private final String delimiter;

    public DataConverter(ConvertDataCommand command) {
        this.dataPath = Paths.get(command.dataPath);
        this.keyFile = Paths.get(command.keyPath).toFile();
        this.outputPath = Paths.get(command.outputPath);

        this.algorithmType = command.algorithmType;

        this.granularity = command.granularity;
        this.delimiter = command.delimiter;
    }

    // TODO
    public void parse() {
        // Assumptions: duplicate icdb schema already exists
        // Steps:
        // 1. Export data outfile -> .unl files
        // 2. Filereader -> generate signature -> filewriter
        // 3. Load data infile -> icdb
    }

    private void exportData() {

    }

    private void convertData() {

    }

    private void loadData() {

    }
}
