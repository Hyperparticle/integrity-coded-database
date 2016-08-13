package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.DBConnection;
import io.DBConverter;
import io.SchemaConverter;
import main.args.config.UserConfig;
import main.args.option.QuerySplitter;
import org.jetbrains.annotations.NotNull;
import parse.ICDBQuery;

import java.util.List;

/**
 * JCommander Command for converting a database or database query
 *
 * Created on 6/8/2016
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = {CommandLineArgs.CONVERT}, commandDescription = "Convert an existing DB or DB query")
public class ConvertCommand implements ConfigCommand {

    @Parameter(names = { "-db, --database" }, description = "Convert a database (leave blank for default schema specified in the config file)")
    private String dbSchema;

    @Parameter(names = { "-s", "--skip" }, description = "Skip one or more DB conversion steps (duplicate|schema|export|data|import)")
    private List<String> skipSteps;

    @Parameter(names = { "-p", "--parallel" }, description = "Convert a database in parallel to improve performance (default true)")
    private Boolean parallel = true;

    @Parameter(names = { "-q", "--query" }, description = "Convert one or more query arguments", splitter = QuerySplitter.class)
    private List<String> queries;


    @Override
    public void execute(@NotNull UserConfig userConfig) {

    }

    /**
     * Converts the specified DB to an ICDB
     */
    private void convertDB(UserConfig dbConfig) {
        // Duplicate the DB, and add additional columns
        DBConnection db = DBConnection.connect(dbConfig.schema, dbConfig);
        SchemaConverter.convertSchema(db, dbConfig, convertConfig);

        // Connect to the newly created DB
        DBConnection icdb = DBConnection.connect(dbConfig.icdbSchema, dbConfig);
        DBConverter dbConverter = new DBConverter(db, icdb, dbConfig, convertConfig);

        // Export, convert, and load all data
        dbConverter.convertAll();
    }

    /**
     * Converts the Query to an ICDB Query
     */
    private void convertQuery(UserConfig dbConfig) {
        final ConvertQueryCommand convertQueryCmd = cmd.convertQueryCommand;
        final String icdbSchema = dbConfig.icdbSchema;

        DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);

        convertQueryCmd.queries.forEach(query -> {
            ICDBQuery icdbQuery = dbConfig.granularity.getQuery(query, icdb, dbConfig.codeGen);

            logger.info("Verify query:");
            logger.info(icdbQuery.getVerifyQuery());

            logger.info("Converted query:");
            logger.info(icdbQuery.getConvertedQuery());
        });
    }

}
