package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.source.DataSource;

/**
 * JCommander Command for benchmarking queries. Reads queries from stdin.
 *
 * Created on 8/8/2016
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { CommandLineArgs.BENCHMARK }, commandDescription = "Benchmark a Query")
public class BenchmarkCommand {

    @Parameter(names = { "-db", "--database" }, description = "Specify a custom database schema name")
    public String schemaName;

    @Parameter(names = { "-f", "--fetch" }, description = "Use eager or lazy fetching. (Default: LAZY)")
    public DataSource.Fetch fetch = DataSource.Fetch.LAZY;

    @Parameter(names = { "-t", "--threads" }, description = "The number of worker threads for verification. An argument of 0 will use the JVM default configuration, which usually results in the best parallel performance. (Default: 0)")
    public Integer threads = 0;

    @Parameter(names = { "-s", "--select" }, description = "Specify a path for all select queries")
    public String selectPath;

    @Parameter(names = { "-d", "--delete" }, description = "Specify a path for all delete queries")
    public String deletePath;

    @Parameter(names = { "-i", "--insert" }, description = "Specify a path for all insert queries")
    public String insertPath;

    @Parameter(names = { "-u", "--update" }, description = "Specify a path for all update queries")
    public String updatePath;

    @Parameter(names = { "-b", "--baseline" }, description = "Do baseline queries")
    public Boolean baseline = false;

}
