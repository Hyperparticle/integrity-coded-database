package main;

import convert.DBConnection;
import convert.DataConverter;
import convert.ICDB;
import convert.SchemaConverter;
import main.args.CommandLineArgs;
import main.args.config.Config;
import main.args.option.AlgorithmType;
import main.args.option.Granularity;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * <p>
 *     A tool for performing ICDB-related tasks.
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
public class ICDBTool {



    public static void main(String[] args) throws SQLException {
        // Parse the command-line arguments
        CommandLineArgs cmd = new CommandLineArgs(args);
        Config config = cmd.getConfig();

        // Connect to the DB
        DBConnection.set(config.ip, config.port, config.user, config.password);

        Connection db =  DBConnection.connect(config.schema);

        // Execute a command
        if (cmd.isCommand(CommandLineArgs.CONVERT_DB)) {
            // Create a new schema
            SchemaConverter schemaConverter = new SchemaConverter(db, config);
            schemaConverter.convert();

            Connection icdb =  DBConnection.connect(config.schema + ICDB.ICDB_SUFFIX);

            // Convert all data
            DataConverter dataConverter = new DataConverter(db, icdb, config);
            dataConverter.convert();
        } else if (cmd.isCommand(CommandLineArgs.CONVERT_DATA)) {
            // TODO
        } else if (cmd.isCommand(CommandLineArgs.CONVERT_QUERY)) {
            // TODO
        } else if (cmd.isCommand(CommandLineArgs.EXECUTE_QUERY)) {
            // TODO
        } else {
            cmd.jCommander.usage();
        }
    }

}
