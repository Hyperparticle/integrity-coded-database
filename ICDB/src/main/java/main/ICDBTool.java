package main;

import convert.DBConnection;
import convert.DataConverter;
import convert.SchemaConverter;
import main.args.CommandLineArgs;
import main.args.ConvertDataCommand;
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

    public static void main(String[] args) {
        CommandLineArgs cmd = new CommandLineArgs(args);

        connectDB();

        if (cmd.isCommand(CommandLineArgs.CONVERT_DATA)) {
            if (cmd.convertDataCommand.help) {
                cmd.jCommander.usage(CommandLineArgs.CONVERT_DATA);
                System.exit(0);
            }

            DataConverter converter = new DataConverter(cmd.convertDataCommand);
            converter.parse();
        } else if (cmd.isCommand(CommandLineArgs.CONVERT_QUERY)) {
            // TODO
        } else if (cmd.isCommand(CommandLineArgs.EXECUTE_QUERY)) {
            // TODO
        } else {
            cmd.jCommander.usage();
        }
    }

    private static void connectDB() {
        try {
            DBConnection dbConnection = new DBConnection("localhost", 3306, "root", "");
            Connection connection = dbConnection.connect("sakila");

            SchemaConverter converter = new SchemaConverter(connection, Granularity.TUPLE);
            converter.convert();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
