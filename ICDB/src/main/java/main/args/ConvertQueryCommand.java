package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

/**
 * <p>
 * <p>
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { "convert-query" }, commandDescription = "Convert queries to ICDB queries")
public class ConvertQueryCommand {

    @Parameter(names = { "-q" }, description = "Convert one or more queries as arguments")
    public List<String> queries;

    @Parameter(names = { "-f" }, description = "Convert all queries in one or more files")
    public List<String> files;

}
