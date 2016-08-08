package main;

import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.source.DBSource;
import io.source.DataSource;
import main.args.*;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import io.DBConnection;
import io.DBConverter;
import io.SchemaConverter;
import main.args.config.ConfigArgs;
import org.jooq.Record;
import parse.ICDBQuery;
import verify.QueryVerifier;

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
            ICDBQuery icdbQuery = dbConfig.granularity.getQuery(query, icdb, dbConfig.codeGen);

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
        final String icdbSchema = dbConfig.icdbSchema;

        DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);

        String query = executeQueryCommand.query;
        ICDBQuery icdbQuery = dbConfig.granularity.getQuery(query, icdb, dbConfig.codeGen);

        logger.info("Original Query: {}", query);

        QueryVerifier verifier = dbConfig.granularity.getVerifier(icdb, dbConfig);

        if (!icdbQuery.needsVerification()) {
            verifier.execute(icdbQuery);
        } else if (verifier.verify(icdbQuery)) {
            logger.info("Query verified");
            verifier.execute(icdbQuery);
        } else {
            logger.info(icdbQuery.getVerifyQuery());
            logger.info("Query failed to verify");
            logger.error(verifier.getError());
        }
    }

    /**
     * Benchmarks the query
     */
    private static void benchmark(CommandLineArgs cmd, UserConfig dbConfig) {
        final BenchmarkCommand benchmarkCommand = cmd.benchmarkCommand;
        final String dbSchema = benchmarkCommand.schemaName != null ? benchmarkCommand.schemaName : dbConfig.icdbSchema;

        DBConnection db = DBConnection.connect(dbSchema, dbConfig);
        String query = benchmarkCommand.query;

        Stopwatch executionTime = Stopwatch.createStarted();

        logger.info("\n" + db.getCreate().fetch(query));

        logger.debug("Total query execution time: {}", executionTime.elapsed(ICDBTool.TIME_UNIT));
    }

	static {
        System.setProperty("org.jooq.no-logo", "true");
	}

}
