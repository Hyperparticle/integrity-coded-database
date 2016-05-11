package main;

import com.beust.jcommander.JCommander;
import main.args.ConvertDataCommand;
import main.args.ConvertQueryCommand;
import main.args.ExecuteQueryCommand;

/**
 * <p>
 *     A tool for performing ICDB-related tasks.
 * </p>
 * Created on 5/10/2016
 * @author Dan Kondratyuk
 */
public class ICDBTool {

    public static void main(String[] args) {
        JCommander jCommander = new JCommander(new ICDBTool());

        ConvertDataCommand convertDataCommand = new ConvertDataCommand();
        ConvertQueryCommand convertQueryCommand = new ConvertQueryCommand();
        ExecuteQueryCommand executeQueryCommand = new ExecuteQueryCommand();

        jCommander.addCommand("convert-data", convertDataCommand);
        jCommander.addCommand("convert-query", convertQueryCommand);
        jCommander.addCommand("execute-query", executeQueryCommand);

        jCommander.parse(args);

        System.out.println(convertDataCommand.verify);
    }

}
