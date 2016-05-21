package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

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

    @Parameter(names = { "-f" }, variableArity = true, description = "Convert one or more files containing delimited values")
    public List<String> files;

    @Parameter(names = { "-t" }, variableArity = true, description = "Convert one or more tuples as arguments")
    public List<String> tuples;

    @Parameter(names = { "-c" }, description = "The type of cipher to use (RSA, AES, or SHA)")
    public String cipherType = "SHA";

    @Parameter(names = { "-g" }, description = "The granularity to use (per tuple or per field)")
    public String granularity = "tuple";

    @Parameter(names = { "-d" }, description = "The field delimiter")
    public String delimiter = "|";

    @Parameter(names = { "-h", "--help" }, help = true)
    public boolean help;

}
