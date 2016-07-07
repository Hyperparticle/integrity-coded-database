package main;

import java.io.FileNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import convert.DBConnection;
import convert.DBConverter;
import convert.Format;
import convert.SchemaConverter;
import main.args.CommandLineArgs;
import main.args.ConvertDBCommand;
import main.args.ConvertQueryCommand;
import main.args.ExecuteQueryCommand;
import main.args.config.Config;
import net.sf.jsqlparser.JSQLParserException;
import parse.QueryConverter;
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

	private static final Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws JSQLParserException, FileNotFoundException {
		Stopwatch totalTime = Stopwatch.createStarted();

		// Parse the command-line arguments
		CommandLineArgs cmd = new CommandLineArgs(args);
		Config dbConfig = cmd.getConfig();
		DBConnection.configure(dbConfig.ip, dbConfig.port, dbConfig.user, dbConfig.password);

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
	private static void convertDB(CommandLineArgs cmd, Config dbConfig) {
		final ConvertDBCommand convertConfig = cmd.convertDBCommand;
		final String icdbSchema = dbConfig.schema + Format.ICDB_SUFFIX;

		// Duplicate the DB, and add additional columns
		DBConnection db = DBConnection.connect(dbConfig.schema, dbConfig);
		SchemaConverter.convertSchema(db, dbConfig, convertConfig);

		// Connect to the newly created DB
		DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);
		DBConverter dbConverter = new DBConverter(db, icdb, dbConfig, convertConfig);

		// Convert all data and load it
		dbConverter.convert();
		dbConverter.load(); // TODO: split this into a data exporter class
	}

	/**
	 * Converts the Query to an ICDB Query
	 */

	private static void convertQuery(CommandLineArgs cmd, Config dbConfig) throws JSQLParserException {
		final ConvertQueryCommand convertQueryCmd = cmd.convertQueryCommand;
		final String icdbSchema = dbConfig.schema + Format.ICDB_SUFFIX;

		DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);

		QueryConverter converter = new QueryConverter(convertQueryCmd, icdb);
		String result = converter.convert();
		System.out.println(result);
	}

	/**
	 * Executes the query
	 */
	private static void executeQuery(CommandLineArgs cmd, Config dbConfig) throws JSQLParserException {
		final ExecuteQueryCommand executeQueryCommand = cmd.executeQueryCommand;
		final String icdbSchema = dbConfig.schema + Format.ICDB_SUFFIX;

		DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);

		String query = executeQueryCommand.queries.get(0);

        // Convert query if specified
		if (executeQueryCommand.convert) {
            QueryConverter converter = new QueryConverter(executeQueryCommand, icdb);
            query = converter.convert();
        }

		QueryVerifier verifier = new QueryVerifier(executeQueryCommand, icdb, query);
		verifier.execute();
	}

}
