package main;

import convert.DataConverter;
import main.args.CommandLineArgs;
import main.args.ConvertDataCommand;

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

        if (cmd.isCommand(CommandLineArgs.CONVERT_DATA)) {
            if (cmd.convertDataCommand.help) {
                cmd.jCommander.usage(CommandLineArgs.CONVERT_DATA);
                System.exit(0);
            }

            parseConvertData(cmd.convertDataCommand);
        } else if (cmd.isCommand(CommandLineArgs.CONVERT_QUERY)) {
            // TODO
        } else if (cmd.isCommand(CommandLineArgs.EXECUTE_QUERY)) {
            // TODO
        } else {
            cmd.jCommander.usage();
        }
    }

    private static void parseConvertData(ConvertDataCommand params) {
        DataConverter converter = new DataConverter(params.cipherType, params.granularity, params.delimiter);

        if (params.tuples == null && params.files == null) {
            System.err.println("No data was converted because no data was specified.");
            System.exit(1);
        }

        converter.parse(params.tuples);
        converter.parseFiles(params.files);
    }

    private static void parseConvertQuery() {

    }

    private static void parseExecuteQuery() {

    }

}
