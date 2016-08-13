package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * <p>
 *     JCommander Command for converting an existing database
 * </p>
 * Created on 6/8/2016
 *
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { CommandLineArgs.CONVERT_DB }, commandDescription = "Convert an existing DB to ICDB")
public class ConvertDBCommand {

    @Parameter(names = { "--skip-duplicate" }, description = "If set, the duplicate DB step will be skipped")
    public Boolean skipDuplicate = false;

    @Parameter(names = { "--skip-schema" }, description = "If set, the schema conversion step will be skipped")
    public Boolean skipSchema = false;

    @Parameter(names = { "--skip-export" }, description = "If set, the data export step will be skipped")
    public Boolean skipExport = false;

    @Parameter(names = { "--skip-data" }, description = "If set, the data conversion step will be skipped")
    public Boolean skipConvert = false;

    @Parameter(names = { "--skip-load" }, description = "If set, the data loading step will be skipped")
    public Boolean skipLoad = false;

}
