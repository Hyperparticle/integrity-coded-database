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

}
