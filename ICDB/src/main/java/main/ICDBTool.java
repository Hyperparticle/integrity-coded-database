package main;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import convert.DBConnection;
import convert.DBConverter;
import convert.Format;
import convert.ICDB;
import convert.SchemaConverter;
import main.args.CommandLineArgs;
import main.args.ConvertDBCommand;
import main.args.ConvertQueryCommand;
import main.args.config.Config;
import net.sf.jsqlparser.JSQLParserException;
import parse.QueryConverter;

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

	public static void main(String[] args) {
		Stopwatch totalTime = Stopwatch.createStarted();

		// Parse the command-line arguments
		CommandLineArgs cmd = new CommandLineArgs(args);
		Config dbConfig = cmd.getConfig();

		// Connect to the DB
		DBConnection dbConnection = new DBConnection(dbConfig.ip, dbConfig.port, dbConfig.user, dbConfig.password);
		Connection db = null;

		try {
			logger.info("Connecting to DB {} at {}:{}", dbConfig.schema, dbConfig.ip, dbConfig.port);
			db = dbConnection.connect(dbConfig.schema);
		} catch (SQLException e) {
			logger.error("Unable to connect to {}: {}", dbConfig.schema, e.getMessage());
			logger.debug(e.getStackTrace());
			System.exit(1);
		}

		if (db == null) {
			return;
		}

		// Execute a command
		if (cmd.isCommand(CommandLineArgs.CONVERT_DB)) {
			convertDB(cmd, dbConfig, dbConnection, db);
		} else if (cmd.isCommand(CommandLineArgs.CONVERT_DATA)) {
			// TODO
		} else if (cmd.isCommand(CommandLineArgs.CONVERT_QUERY)) {
			// TODO
		} else if (cmd.isCommand(CommandLineArgs.EXECUTE_QUERY)) {
			// TODO
		} else {
			cmd.jCommander.usage();
			System.exit(0);
		}

		logger.info("Total time elapsed: {}", totalTime);
	}

	/**
	 * Converts the specified DB to an ICDB
	 */
	private static void convertDB(CommandLineArgs cmd, Config dbConfig, DBConnection dbConnection, Connection db) {
		ConvertDBCommand convertConfig = cmd.convertDBCommand;

		// Duplicate the DB, and add additional columns
		try {
			SchemaConverter schemaConverter = new SchemaConverter(db, dbConfig, convertConfig);
			schemaConverter.convertSchema();
		} catch (SQLException e) {
			logger.error("There was an error attempting to convert the schema: {}", e.getMessage());
			logger.debug(e.getStackTrace());
			System.exit(1);
		}

		if (!convertConfig.skipData) {
			// Connect to the newly created DB
			String icdbSchema = dbConfig.schema + Format.ICDB_SUFFIX;
			Connection icdb = null;

			try {
				logger.debug("Connecting to icdb {}", icdbSchema);
				icdb = dbConnection.connect(icdbSchema);
			} catch (SQLException e) {
				logger.error("Unable to connect to icdb: {}", e.getMessage());
				logger.debug(e.getStackTrace());
				System.exit(1);
			}

			if (icdb == null) {
				return;
			}

			// Convert all data
			logger.info("Migrating data from {} to {}", dbConfig.schema, icdbSchema);
			DBConverter DBConverter = new DBConverter(db, icdb, dbConfig);
			DBConverter.convert();
		} else {
			logger.debug("Data conversion skipped");
		}
	}

	/**
	 * Converts the Query to ICDB Query
	 * 
	 * @throws JSQLParserException
	 */
	public static void convertQuery(CommandLineArgs cmd, Config dbConfig, DBConnection dbConnection)
			throws JSQLParserException {
		ConvertQueryCommand convertQueryCmd = cmd.convertQueryCommand;
		// Connect to the newly created DB
		String icdbSchema = (dbConfig.schema + ICDB.ICDB_SUFFIX).toUpperCase();
		Connection icdb = null;

		try {
			logger.debug("Connecting to icdb {}", icdbSchema);
			icdb = dbConnection.connect(icdbSchema);
		} catch (SQLException e) {
			logger.error("Unable to connect to icdb: {}", e.getMessage());
			logger.debug(e.getStackTrace());
			System.exit(1);
		}

		if (icdb == null) {
			return;
		}
		QueryConverter converter = new QueryConverter(convertQueryCmd, dbConfig, icdb);
		converter.convert();
	}

}
