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
@Parameters(commandDescription = "Execute queries on an ICDB Server")
public class ExecuteQueryCommand {

    @Parameter(description = "Execute one or more queries as arguments")
    public List<String> queries;

    @Parameter(names = { "-f" }, description = "Execute all queries in one or more files")
    public List<String> files;

    @Parameter(names = { "--convert" }, description = "Convert the query before executing")
    public Boolean convert = false;

    @Parameter(names = { "--skip-verify" }, description = "Skip the data verification stage after query execution")
    public Boolean skipVerify = false;

}
