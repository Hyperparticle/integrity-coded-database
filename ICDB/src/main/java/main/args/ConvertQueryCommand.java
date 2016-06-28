package main.args;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import main.args.option.Granularity;
import main.args.option.GranularityConverter;

/**
 * <p>
 *     JCommander Command for converting a DB query
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = { CommandLineArgs.CONVERT_QUERY }, commandDescription = "Convert queries to ICDB queries")
public class ConvertQueryCommand extends ConfigCommand {

	@Parameter(names = { "-q", "--query" }, description = "Convert one or more queries as arguments")
	public List<String> queries;

	@Parameter(names = { "-f", "--file" }, description = "Convert all queries in one or more files")
	public List<String> files;

	@Parameter(names = { "-g", "--granularity" }, converter = GranularityConverter.class, description = "The granularity to use (per tuple or per field)")
	public Granularity granularity = Granularity.TUPLE;

}
