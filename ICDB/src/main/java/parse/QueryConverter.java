package parse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import main.args.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import convert.DBConnection;
import main.args.ConvertQueryCommand;
import main.args.ExecuteQueryCommand;
import main.args.option.Granularity;
import net.sf.jsqlparser.JSQLParserException;

/**
 * <p>
 * Converts a SQL query into an ICDB SQL query.
 * </p>
 * Created 5/8/2016
 * 
 * @author Dan Kondratyuk
 */

// TODO ICDB query output file path.//Database name to fetch the metadata.
public class QueryConverter {

	private Path outputPath = Paths.get("/Users/ujwal-mac/Desktop/queries");
	private File queryFile;

	private final List<String> queries;
	private final List<String> files;
	private final DBConnection icdb;
	private final Granularity granularity;

	private static final Logger logger = LogManager.getLogger();

	public QueryConverter(ConvertQueryCommand command, DBConnection icdb) {
		this.queries = command.queries;
		// this.queries = new ArrayList<String>();
		// queries.add("SELECT gender,first_name FROM employees;");
		this.files = command.files;
		this.granularity = command.granularity;
		// this.granularity = Granularity.FIELD;
		System.out.println(this.granularity);
		this.icdb = icdb;
	}

	public QueryConverter(ConvertQueryCommand command, DBConnection icdb, Config dbConfig) {
		this.queries = command.queries;
		this.files = command.files;
		this.icdb = icdb;
		this.granularity = dbConfig.granularity;
	}

	public QueryConverter(ExecuteQueryCommand executeQueryCommand, String query, DBConnection icdb, Config dbConfig) {
		this.queries = new ArrayList<>();
		queries.add(query);
		this.files = new ArrayList<>();
		this.granularity = dbConfig.granularity;
		this.icdb = icdb;
	}

	/**
	 * <p>
	 * Convert a query or files with queries to their respective ICDB Query
	 * </p>
	 * 
	 * @throws JSQLParserException
	 */
	public String convert() throws JSQLParserException {
		try {

			String outputName = "ICDBquery.sql";
			// File outputFile = Paths.get(outputPath.toString(),
			// outputName).toFile();
			// FileWriter writer = new FileWriter(outputFile);
			StringBuilder builder = new StringBuilder();
			String Schema = "";
			if (this.queries.size() != 0) {

				for (String query : queries) {
					System.out.println(query);
					builder.setLength(0);
					// Get the database name mentioned in USE command
					StringTokenizer tokenizer = new StringTokenizer(query, ";");

					while (tokenizer.hasMoreTokens()) {
						String next = tokenizer.nextToken();

						SQLParser parser;
						if (granularity == Granularity.TUPLE) {
							parser = new OCTparser(next, Schema, this.granularity, this.icdb.getConnection());
						} else {
							parser = new OCFparser(next, Schema, this.granularity, this.icdb.getConnection());
						}
						// SQLParser parser = new SQLParser(next, Schema,
						// this.granularity, this.icdb);
						builder.append(parser.parse()).append(";").append("\n");

					}
					// writer.append(builder);
				}
				// writer.close();
			} else {
				for (String file : files) {
					// Each queries in the files should include USE SCHEMA
					// command

					this.queryFile = Paths.get(file).toFile();
					outputName = queryFile.getName() + "-icdb";
					// outputFile = Paths.get(outputPath.toString(),
					// outputName).toFile();
					// writer = new FileWriter(outputFile);
					Scanner queryScan = new Scanner(this.queryFile);

					while (queryScan.hasNextLine()) {
						builder.setLength(0);

						String Query = queryScan.nextLine();
						if (Query.contains("USE")) {

							Schema = (Query.substring(Query.indexOf(" ")).trim() + "_ICDB").toUpperCase();
							builder.append((Query + "_ICDB").toUpperCase()).append(";").append("\n");
						} else {
							if (Schema == "") {
								System.out.println("No Database used for the Query provided.");
								break;
							}

							SQLParser parser;
							if (granularity == Granularity.TUPLE) {
								parser = new OCTparser(Query, Schema, this.granularity, this.icdb.getConnection());
							} else {
								parser = new OCFparser(Query, Schema, this.granularity, this.icdb.getConnection());
							}
							// SQLParser parser = new SQLParser(Query, Schema,
							// this.granularity, this.icdb);
							builder.append(parser.parse()).append(";").append("\n");
						}

						// writer.append(builder);
					}
					// writer.close();
					queryScan.close();
				}

			}

			// TODO: use streams to return multiple queries
			return builder.toString();
		} catch (IOException e) {
			// TODO: handle exception
			System.err.println("Failed to convert query ");
		}

		return null;
	}

}
