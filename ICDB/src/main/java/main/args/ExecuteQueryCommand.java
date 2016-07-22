package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import main.args.option.Granularity;
import main.args.option.GranularityConverter;

import java.util.List;

/**
 * <p>
 *     JCommander Command for executing an ICDB query
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { CommandLineArgs.EXECUTE_QUERY }, commandDescription = "Execute queries on an ICDB Schema")
public class ExecuteQueryCommand extends ConfigCommand {

    @Parameter(names = { "-q", "--query" }, description = "Execute a query as an argument")
    public String query;

//    @Parameter(names = { "-f", "--file" }, description = "Execute all queries in one or more files")
//    public List<String> files;

    @Parameter(names = { "--convert" }, description = "Convert the query before executing")
    public Boolean convert = false;

//    @Parameter(names = { "--skip-verify" }, description = "Skip the data verification stage after query execution")
//    public Boolean skipVerify = false;

}
