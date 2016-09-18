package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import crypto.AlgorithmType;
import io.Format;
import io.source.DataSource;
import main.args.*;
import main.args.config.UserConfig;
import main.args.option.Granularity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import io.DBConnection;
import io.DBConverter;
import io.SchemaConverter;
import main.args.config.ConfigArgs;
import parse.ICDBQuery;
import stats.RunStatistics;
import stats.Statistics;
import stats.StatisticsMetadata;
import verify.QueryVerifier;
import verify.serial.Icrl;

/**
 * <p>
 * A tool for performing ICDB-related tasks.
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
public class ICDBTool {

    // The time unit for all timed log statements
    public static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

	private static final Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws FileNotFoundException {
		Stopwatch totalTime = Stopwatch.createStarted();

		// Parse the command-line arguments
		CommandLineArgs cmd = new CommandLineArgs(args);
		ConfigArgs configArgs = cmd.getConfig();
        UserConfig dbConfig = UserConfig.init(configArgs);

        Icrl.Companion.debug(!dbConfig.validateIcrl);

		DBConnection.configure(dbConfig);

		// Execute a command
		if (cmd.isCommand(CommandLineArgs.CONVERT_DB)) {
			convertDB(cmd, dbConfig);
		} else if (cmd.isCommand(CommandLineArgs.CONVERT_QUERY)) {
			convertQuery(cmd, dbConfig);
		} else if (cmd.isCommand(CommandLineArgs.EXECUTE_QUERY)) {
			executeQuery(cmd, dbConfig);
		} else if (cmd.isCommand(CommandLineArgs.BENCHMARK)) {
		    benchmark(cmd, dbConfig);
		} else { // TODO: add revoke serial command
			cmd.jCommander.usage();
			System.exit(0);
		}

        logger.info("");
		logger.info("Total time elapsed: {}", totalTime.elapsed(ICDBTool.TIME_UNIT));
	}

	/**
	 * Converts the specified DB to an ICDB
	 */
	private static void convertDB(CommandLineArgs cmd, UserConfig dbConfig) {
		final ConvertDBCommand convertConfig = cmd.convertDBCommand;

		// Duplicate the DB, and add additional columns
		DBConnection db = DBConnection.connect(dbConfig.schema, dbConfig);
		SchemaConverter.convertSchema(db, dbConfig, convertConfig);

		// Connect to the newly created DB
		DBConnection icdb = DBConnection.connect(dbConfig.icdbSchema, dbConfig);
		DBConverter dbConverter = new DBConverter(db, icdb, dbConfig, convertConfig);

		// Export, convert, and load all data
		dbConverter.convertAll();
	}

	/**
	 * Converts the Query to an ICDB Query
	 */
	private static void convertQuery(CommandLineArgs cmd, UserConfig dbConfig) {
        final ConvertQueryCommand convertQueryCmd = cmd.convertQueryCommand;
        final String icdbSchema = dbConfig.icdbSchema;

        DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);

        convertQueryCmd.queries.forEach(query -> {
            ICDBQuery icdbQuery = dbConfig.granularity.getQuery(query, icdb, dbConfig.codeGen, new RunStatistics());

            logger.info("Verify query:");
            logger.info(icdbQuery.getVerifyQuery());

            logger.info("Converted query:");
            logger.info(icdbQuery.getConvertedQuery());
        });
    }

    /**
     * Executes a query
     */
    private static void executeQuery(CommandLineArgs cmd, UserConfig dbConfig) {
        final ExecuteQueryCommand executeQueryCommand = cmd.executeQueryCommand;

        StatisticsMetadata metadata = new StatisticsMetadata(
                dbConfig.codeGen.getAlgorithm(), dbConfig.granularity, dbConfig.icdbSchema, executeQueryCommand.query,
                executeQueryCommand.fetch, executeQueryCommand.threads
        );

        Statistics statistics = new Statistics(metadata, new File("./src/main/resources/statistics/data.csv"));
        RunStatistics run = new RunStatistics();
        statistics.addRun(run);

        executeQueryRun(
            executeQueryCommand.query, executeQueryCommand.fetch, executeQueryCommand.threads, dbConfig, run, true
        );

        statistics.outputRuns();
    }

    /**
     * Benchmarks the query
     */
    private static void benchmark(CommandLineArgs cmd, UserConfig dbConfig) {
        final BenchmarkCommand benchmarkCommand = cmd.benchmarkCommand;
//        final String dbSchema = benchmarkCommand.schemaName != null ? benchmarkCommand.schemaName : dbConfig.icdbSchema;
        final String dbSchema = dbConfig.icdbSchema;

        String query = benchmarkCommand.query;

//        Arrays.stream(AlgorithmType.values()).forEach(algorithm -> {
//            logger.info("Using Algorithm: {}", algorithm);
//            dbConfig.setAlgorithm(algorithm);
//
//            Arrays.stream(Granularity.values()).forEach(granularity -> {
//                logger.info("Using Granularity: {}", granularity);
//                dbConfig.setGranularity(granularity);
//
//                });
//            });

        final AlgorithmType algorithm = dbConfig.codeGen.getAlgorithm();
        final Granularity granularity = dbConfig.granularity;

        Statistics statistics = new Statistics(
            new StatisticsMetadata(
                algorithm, granularity, dbSchema, query, benchmarkCommand.fetch, benchmarkCommand.threads
            ),
            new File("./src/main/resources/statistics/" + algorithm + "-" + granularity + "-data.csv")
        );

        final int[] sizes = { 125_000, 250_000, 500_000, 1_000_000, 1_500_000, 2_000_000 };

        // Run through the following fetch sizes
        IntStream.of(sizes).forEach(i -> {
            RunStatistics run = new RunStatistics();
            statistics.addRun(run);

            Stopwatch executionTime = Stopwatch.createStarted();

            String limitQuery = query + " limit " + i;
            executeQueryRun(limitQuery, benchmarkCommand.fetch, benchmarkCommand.threads, dbConfig, run, false);

            logger.debug("LIMIT {}:", i);
            logger.debug("Total query execution time: {}", executionTime.elapsed(ICDBTool.TIME_UNIT));
        });

        statistics.outputRuns();
    }

    /**
     * Executes a query
     */
    private static void executeQueryRun(String query, DataSource.Fetch fetch, int threads, UserConfig dbConfig, RunStatistics run, boolean execute) {
        DBConnection icdb = DBConnection.connect(dbConfig.icdbSchema, dbConfig);
        ICDBQuery icdbQuery = dbConfig.granularity.getQuery(query, icdb, dbConfig.codeGen, run);

        logger.info("Original Query: {}", query);

        QueryVerifier verifier = dbConfig.granularity.getVerifier(icdb, dbConfig, threads, fetch, run);

        if (!icdbQuery.needsVerification()) {
            if (execute) { verifier.execute(icdbQuery); }
        } else if (verifier.verify(icdbQuery)) {
            logger.info("Query verified");
            if (execute) { verifier.execute(icdbQuery); }
        } else {
            logger.info(icdbQuery.getVerifyQuery());
            logger.info("Query failed to verify");
            logger.error(verifier.getError());
        }
    }

	static {
        System.setProperty("org.jooq.no-logo", "true");
	}

}
