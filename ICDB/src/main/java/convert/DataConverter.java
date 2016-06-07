package convert;

import com.google.common.base.Charsets;
import main.args.ConvertDataCommand;
import main.args.option.Granularity;
import main.args.option.MACType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
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

    private final MACType macType;

    private final Granularity granularity;
    private final String delimiter;

    public DataConverter(ConvertDataCommand command) {
        this.dataPath = Paths.get(command.dataPath);
        this.keyFile = Paths.get(command.keyPath).toFile();
        this.outputPath = Paths.get(command.outputPath);

        this.macType = command.macType;

        this.granularity = command.granularity;
        this.delimiter = command.delimiter;
    }

    public void parse() {

    }
}
