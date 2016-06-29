package main.args;

import cipher.mac.AlgorithmType;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import main.args.option.*;

/**
 * <p>
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { CommandLineArgs.CONVERT_DATA }, commandDescription = "Convert data tuples to ICDB data tuples")
public class ConvertDataCommand extends ConfigCommand {

    @Parameter(names = { "-i", "--input" }, variableArity = true, description = "Convert all data files under the given directory")
    public String dataPath = "./tmp/db-files/data";

    @Parameter(names = { "-k", "--key" }, required = true, description = "The key file path")
    public String keyPath;

    @Parameter(names = { "-o", "--output" }, description = "The output destination path")
    public String outputPath = "./tmp/converted-db-files/data";

    // TODO: add direct passing of data
//    @Parameter(names = { "-t", "--tuple" }, variableArity = true, description = "Convert one or more tuples as arguments")
//    public List<String> tuples;

    @Parameter(names = { "-c", "--cipher" }, converter = MACTypeConverter.class, description = "The type of cipher to use (RSA, AES, or SHA)")
    public AlgorithmType algorithmType = AlgorithmType.SHA;

    @Parameter(names = { "-g", "--granularity" }, converter = GranularityConverter.class, description = "The granularity to use (per tuple or per field)")
    public Granularity granularity = Granularity.TUPLE;

    @Parameter(names = { "-d", "--delimiter" }, description = "The delimiter between each data field")
    public String delimiter = Delimiters.DEFAULT;

    @Parameter(names = { "-h", "--help" }, help = true)
    public boolean help;

}
