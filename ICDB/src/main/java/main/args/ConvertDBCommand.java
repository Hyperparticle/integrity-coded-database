package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * <p>
 * <p>
 * </p>
 * Created on 6/8/2016
 *
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { CommandLineArgs.CONVERT_DB }, commandDescription = "Convert an existing DB to ICDB")
public class ConvertDBCommand {

    @Parameter(names = { "--export" }, description = "Whether to export the DB")
    public Boolean export;

}
