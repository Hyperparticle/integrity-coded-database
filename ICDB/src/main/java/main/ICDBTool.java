package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.base.Charsets;
import crypto.AlgorithmType;
import io.source.DataSource;
import main.args.*;
import main.args.config.UserConfig;
import main.args.option.Granularity;
import org.apache.commons.io.FileUtils;
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
            if (cmd.benchmarkCommand.selectPath != null) {
                benchmarkSelect(cmd, dbConfig);
            } else if (cmd.benchmarkCommand.deletePath != null && cmd.benchmarkCommand.insert != null) {
                benchmarkDeleteInsert(cmd, dbConfig);
            }
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
            dbConfig.codeGen.getAlgorithm(), dbConfig.granularity, dbConfig.icdbSchema,
            executeQueryCommand.fetch, executeQueryCommand.threads, executeQueryCommand.query
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
     * Benchmarks select queries from a path
     */
    private static void benchmarkSelect(CommandLineArgs cmd, UserConfig dbConfig) {
        final BenchmarkCommand benchmarkCommand = cmd.benchmarkCommand;
        final String dbSchema = benchmarkCommand.schemaName != null ? benchmarkCommand.schemaName : dbConfig.icdbSchema;

        final AlgorithmType algorithm = dbConfig.codeGen.getAlgorithm();
        final Granularity granularity = dbConfig.granularity;

        File[] files = new File(benchmarkCommand.selectPath).listFiles();
        if (files == null) {
            return;
        }

//        try {
//            File statisticsFile = new File("./src/main/resources/statistics/");
//            statisticsFile.mkdirs();
//            FileUtils.cleanDirectory(statisticsFile);
//        } catch (IOException e) {
//            logger.error("Failed to clean statistics folder");
//        }

        Statistics statistics = new Statistics(
            new StatisticsMetadata(
                algorithm, granularity, dbSchema, benchmarkCommand.fetch, benchmarkCommand.threads, "select"
            ),
            new File("./src/main/resources/statistics/" + algorithm + "-" + granularity + "-select.csv")
        );

        Arrays.stream(files)
            .map(file -> {
                try { return FileUtils.readFileToString(file, Charsets.UTF_8); }
                catch (IOException e) { return null; }
            })
            .filter(s -> s != null)
            .sorted()
            .forEach(query -> {
                logger.debug("Running: {}", query);

                RunStatistics run = new RunStatistics();
                statistics.addRun(run);

                Stopwatch executionTime = Stopwatch.createStarted();
                executeQueryRun(query, benchmarkCommand.fetch, benchmarkCommand.threads, dbConfig, run, false);
                logger.debug("Total query execution time: {}", executionTime.elapsed(ICDBTool.TIME_UNIT));
            });

        statistics.outputRuns();
    }

    /**
     * Benchmarks select queries from stdin
     * Note: VERY hacky (I was so frustrated I wanted it to work)
     */
    private static void benchmarkDeleteInsert(CommandLineArgs cmd, UserConfig dbConfig) {
        final BenchmarkCommand benchmarkCommand = cmd.benchmarkCommand;
        final String dbSchema = benchmarkCommand.schemaName != null ? benchmarkCommand.schemaName : dbConfig.icdbSchema;

        final AlgorithmType algorithm = dbConfig.codeGen.getAlgorithm();
        final Granularity granularity = dbConfig.granularity;

        File[] insertFiles = new File(benchmarkCommand.insert).listFiles();
        File[] deleteFiles = new File(benchmarkCommand.insert).listFiles();
        if (insertFiles == null || deleteFiles == null) {
            return;
        }

        Statistics deleteStatistics = new Statistics(
            new StatisticsMetadata(
                    algorithm, granularity, dbSchema, benchmarkCommand.fetch, benchmarkCommand.threads, "delete"
            ),
            new File("./src/main/resources/statistics/" + algorithm + "-" + granularity + "-delete.csv")
        );
        Statistics insertStatistics = new Statistics(
            new StatisticsMetadata(
                    algorithm, granularity, dbSchema, benchmarkCommand.fetch, benchmarkCommand.threads, "insert"
            ),
            new File("./src/main/resources/statistics/" + algorithm + "-" + granularity + "-insert.csv")
        );

        List<String> insertQueries = Arrays.stream(insertFiles)
            .sorted((f1, f2) -> f1.toString().compareTo(f2.toString()))
            .map(file -> {
                try { return FileUtils.readFileToString(file, Charsets.UTF_8); }
                catch (IOException e) { return null; }
            })
            .filter(s -> s != null)
            .collect(Collectors.toList());
        List<String> deleteQueries = Arrays.stream(deleteFiles)
            .sorted((f1, f2) -> f1.toString().compareTo(f2.toString()))
            .map(file -> {
                try { return FileUtils.readFileToString(file, Charsets.UTF_8); }
                catch (IOException e) { return null; }
            })
            .filter(s -> s != null)
            .collect(Collectors.toList());

        for (int i = 0; i < insertQueries.size(); i++) {
            RunStatistics deleteRun = new RunStatistics();
            RunStatistics insertRun = new RunStatistics();
            deleteStatistics.addRun(deleteRun);
            insertStatistics.addRun(insertRun);

            Stopwatch executionTime = Stopwatch.createStarted();
            executeQueryRun(deleteQueries.get(i), benchmarkCommand.fetch, benchmarkCommand.threads, dbConfig, deleteRun, true);
            executeQueryRun(insertQueries.get(i), benchmarkCommand.fetch, benchmarkCommand.threads, dbConfig, insertRun, true);
            logger.debug("Total query execution time: {}", executionTime.elapsed(ICDBTool.TIME_UNIT));
        }

        deleteStatistics.outputRuns();
        insertStatistics.outputRuns();
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
