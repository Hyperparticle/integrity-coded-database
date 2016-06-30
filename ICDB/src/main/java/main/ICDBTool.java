package main;

import java.sql.Connection;
import java.sql.SQLException;

import main.args.ExecuteQueryCommand;
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

	public static void main(String[] args) throws JSQLParserException {
		Stopwatch totalTime = Stopwatch.createStarted();

		// Parse the command-line arguments
		CommandLineArgs cmd = new CommandLineArgs(args);
		Config dbConfig = cmd.getConfig();

		// Connect to the DB
		DBConnection dbConnection = new DBConnection(dbConfig.ip, dbConfig.port, dbConfig.user, dbConfig.password);
		Connection db = connect(dbConfig.schema, dbConfig, dbConnection);

		if (db == null) {
			return;
		}

		// Execute a command
		if (cmd.isCommand(CommandLineArgs.CONVERT_DB)) {
			convertDB(cmd, dbConfig, dbConnection, db);
		} else if (cmd.isCommand(CommandLineArgs.CONVERT_DATA)) {
			// TODO
		} else if (cmd.isCommand(CommandLineArgs.CONVERT_QUERY)) {
			convertQuery(cmd, dbConfig, dbConnection);
		} else if (cmd.isCommand(CommandLineArgs.EXECUTE_QUERY)) {
			executeQuery(cmd, dbConfig, dbConnection);
		} else {
			cmd.jCommander.usage();
			System.exit(0);
		}

		logger.info("Total time elapsed: {}", totalTime);
	}

	private static Connection connect(String schema, Config dbConfig, DBConnection dbConnection) {
        try {
            logger.info("Connecting to DB {} at {}:{}", schema, dbConfig.ip, dbConfig.port);
            return dbConnection.connect(schema);
        } catch (SQLException e) {
            logger.error("Unable to connect to {}: {}", schema, e.getMessage());
            logger.debug(e.getStackTrace());
            System.exit(1);
        }

        return null;
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

        // Connect to the newly created DB
        String icdbSchema = dbConfig.schema + Format.ICDB_SUFFIX;
        Connection icdb = connect(icdbSchema, dbConfig, dbConnection);
        DBConverter dbConverter = new DBConverter(db, icdb, dbConfig);

        if (icdb == null) {
            return;
        }

		if (!convertConfig.skipData) {
			// Convert all data
			logger.info("Converting data from {}", dbConfig.schema);
			dbConverter.convert();
		} else {
			logger.debug("Data conversion skipped");
		}

        if (!convertConfig.skipLoad) {
            // Load all data
            logger.info("Migrating data from {} to {}", dbConfig.schema, icdbSchema);
            dbConverter.load();
        } else {
            logger.debug("Data conversion skipped");
        }
	}

	/**
	 * Converts the Query to an ICDB Query
	 */
	private static void convertQuery(CommandLineArgs cmd, Config dbConfig, DBConnection dbConnection)
			throws JSQLParserException {
		ConvertQueryCommand convertQueryCmd = cmd.convertQueryCommand;
        String icdbSchema = dbConfig.schema + Format.ICDB_SUFFIX;
        Connection icdb = connect(icdbSchema, dbConfig, dbConnection);

		if (icdb == null) {
			return;
		}

		QueryConverter converter = new QueryConverter(convertQueryCmd, icdb);
		String result = converter.convert();
        System.out.println(result);
	}

    /**
     * Executes the query
     */
    private static void executeQuery(CommandLineArgs cmd, Config dbConfig, DBConnection dbConnection)
            throws JSQLParserException {
        ExecuteQueryCommand executeQueryCommand = cmd.executeQueryCommand;
        String icdbSchema = dbConfig.schema + Format.ICDB_SUFFIX;
        Connection icdb = connect(icdbSchema, dbConfig, dbConnection);

        if (icdb == null) {
            return;
        }

        QueryConverter converter = new QueryConverter(executeQueryCommand, icdb);
        String result = converter.convert();
        System.out.println(result);

        QueryVerifier verifier = new QueryVerifier(executeQueryCommand, icdb, result);
        verifier.execute();
    }

}
