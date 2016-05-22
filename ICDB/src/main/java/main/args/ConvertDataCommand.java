package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import main.args.option.*;

import java.util.List;

/**
 * <p>
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { CommandLineArgs.CONVERT_DATA }, commandDescription = "Convert data tuples to ICDB data tuples")
public class ConvertDataCommand {

    @Parameter(names = { "-p", "--path" }, required = true, variableArity = true, description = "Convert all data files under the given path")
    public String dataPath;

    @Parameter(names = { "-k", "--key" }, required = true, description = "The key file path")
    public String keyPath;

    @Parameter(names = { "-d", "--dest" }, required = true, description = "The destination path")
    public String convertPath;

    // TODO: add direct passing of data
//    @Parameter(names = { "-t", "--tuple" }, variableArity = true, description = "Convert one or more tuples as arguments")
//    public List<String> tuples;

    @Parameter(names = { "-c", "--cipher" }, converter = CipherTypeConverter.class, description = "The type of cipher to use (RSA, AES, or SHA)")
    public CipherType cipherType = CipherType.SHA;

    @Parameter(names = { "-g", "--granularity" }, converter = GranularityConverter.class, description = "The granularity to use (per tuple or per field)")
    public Granularity granularity = Granularity.TUPLE;

    @Parameter(names = { "-dl", "--delimiter" }, description = "The delimiter between each data field")
    public String delimiter = Delimiters.DEFAULT;

    @Parameter(names = { "-h", "--help" }, help = true)
    public boolean help;

}
