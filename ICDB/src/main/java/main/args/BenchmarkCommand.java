package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Stopwatch;
import io.DBConnection;
import main.ICDBTool;
import main.args.config.UserConfig;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

/**
 * JCommander Command for benchmarking queries
 *
 * Created on 8/8/2016
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = {CommandLineArgs.BENCHMARK}, commandDescription = "Benchmark a Query")
public class BenchmarkCommand implements ConfigCommand {

    @Parameter(names = { "-q", "--query" }, description = "Execute a query as an argument")
    private String query;

    @Parameter(names = { "-s", "--schema" }, description = "Specify a custom database schema name")
    private String schemaName;

    @Parameter(names = { "-p", "--parallel" }, description = "Execute a query in parallel to improve performance (default true)")
    private Boolean parallel = true;


    @Override
    public void execute(@NotNull UserConfig userConfig) {

    }

    /**
     * Benchmarks the query
     */
    private void benchmark(CommandLineArgs cmd, UserConfig dbConfig) {
        final BenchmarkCommand benchmarkCommand = cmd.benchmarkCommand;
        final String dbSchema = benchmarkCommand.schemaName != null ? benchmarkCommand.schemaName : dbConfig.icdbSchema;

        DBConnection db = DBConnection.connect(dbSchema, dbConfig);
        String query = benchmarkCommand.query;

        // Run through the following fetch sizes
        IntStream.of(500000, 1000000, 1500000, 2000000)
                .forEach(i -> {
                    Stopwatch executionTime = Stopwatch.createStarted();
                    String limitQuery = query + " limit " + i;
                    db.getCreate().fetch(limitQuery);
                    logger.debug("LIMIT {}:", i);
                    logger.debug("Total query execution time: {}", executionTime.elapsed(ICDBTool.TIME_UNIT));
                });
    }

}
