package main.args;

import com.beust.jcommander.JCommander;

/**
 * <p>
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
public class CommandLineArgs {

    public static final String CONVERT_DATA  = "convert-data";
    public static final String CONVERT_QUERY = "convert-query";
    public static final String EXECUTE_QUERY = "execute-query";

    public final JCommander jCommander;
    public final ConvertDataCommand convertDataCommand;
    public final ConvertQueryCommand convertQueryCommand;
    public final ExecuteQueryCommand executeQueryCommand;

    public CommandLineArgs(String[] args) {
        jCommander = new JCommander();

        convertDataCommand  = new ConvertDataCommand();
        convertQueryCommand = new ConvertQueryCommand();
        executeQueryCommand = new ExecuteQueryCommand();

        jCommander.addCommand(convertDataCommand);
        jCommander.addCommand(convertQueryCommand);
        jCommander.addCommand(executeQueryCommand);

        try {
            jCommander.parse(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public boolean isCommand(String command) {
        return jCommander.getParsedCommand().equals(command);
    }

}
