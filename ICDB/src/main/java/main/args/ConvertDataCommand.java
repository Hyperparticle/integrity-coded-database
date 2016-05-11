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
@Parameters(commandNames = { "convert-data" }, commandDescription = "Convert data tuples to ICDB data tuples")
public class ConvertDataCommand {

    @Parameter(names = { "-t" }, description = "Convert one or more tuples as arguments")
    public List<String> tuples;

    @Parameter(names = { "-f" }, description = "Convert one or more files containing comma separated values")
    public List<String> files;

    @Parameter(names = { "--verify" }, description = "Verify that the generated checksums are correct")
    public Boolean verify = false;

    @Parameter(names = { "--cipher" }, description = "The cipher to use")
    public String cipher = "AES";

    @Parameter(names = { "--granularity" }, description = "OCF or OCT conversion")
    public String granularity = "Field";

}
