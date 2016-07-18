package main;

import java.io.FileNotFoundException;

import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import convert.DBConnection;
import convert.DBConverter;
import convert.SchemaConverter;
import main.args.CommandLineArgs;
import main.args.ConvertDBCommand;
import main.args.ConvertQueryCommand;
import main.args.ExecuteQueryCommand;
import main.args.config.ConfigArgs;
import net.sf.jsqlparser.JSQLParserException;
import parse.ICDBQuery;
import verify.QueryVerifier;

/**
 * <p>
 * 	A tool for performing ICDB-related tasks.
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
public class ICDBTool {

	private static final Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws JSQLParserException, FileNotFoundException {
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
		} else {
			 cmd.jCommander.usage();
			System.exit(0);
		}

		logger.info("Total time elapsed: {}", totalTime);
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

		// Convert all data and load it
		dbConverter.convert();
		dbConverter.load();
	}

	/**
	 * Converts the Query to an ICDB Query
	 */
	private static void convertQuery(CommandLineArgs cmd, UserConfig dbConfig) throws JSQLParserException {
        final ConvertQueryCommand convertQueryCmd = cmd.convertQueryCommand;
        final String icdbSchema = dbConfig.icdbSchema;

        DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);

        convertQueryCmd.queries.forEach(query -> {
            ICDBQuery icdbQuery = dbConfig.granularity.getQuery(query, icdb);

            logger.info("Verify query:");
            logger.info(icdbQuery.getVerifyQuery());

            logger.info("Converted query:");
            logger.info(icdbQuery.getConvertedQuery());
        });
    }

	/**
	 * Executes the query
	 */
	private static void executeQuery(CommandLineArgs cmd, UserConfig dbConfig) throws JSQLParserException {
		final ExecuteQueryCommand executeQueryCommand = cmd.executeQueryCommand;
		final String icdbSchema = dbConfig.icdbSchema;

		DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);

		String query = executeQueryCommand.queries.get(0);
        ICDBQuery icdbQuery = dbConfig.granularity.getQuery(query, icdb);

		QueryVerifier verifier = dbConfig.granularity.getVerifier(icdb, dbConfig);

        if (verifier.verify(icdbQuery)) {
            logger.info("Query verified");
            verifier.execute(icdbQuery);
        } else {
            logger.info("Query failed to verify");
            logger.error(verifier.getError());
        }
	}

	static {
        System.setProperty("org.jooq.no-logo", "true");
	}

}
