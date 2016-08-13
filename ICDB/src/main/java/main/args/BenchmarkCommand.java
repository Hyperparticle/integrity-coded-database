package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * JCommander Command for benchmarking queries
 *
 * Created on 8/8/2016
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { CommandLineArgs.BENCHMARK }, commandDescription = "Benchmark a Query")
public class BenchmarkCommand {

    @Parameter(names = { "-q", "--query" }, description = "Execute a query as an argument")
    public String query;

    @Parameter(names = { "-db", "--database" }, description = "Specify a custom database schema name")
    public String schemaName;

}
