package main.args;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.DBConnection;
import main.args.config.UserConfig;
import main.args.option.QuerySplitter;
import org.jetbrains.annotations.NotNull;
import parse.ICDBQuery;
import verify.QueryVerifier;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     JCommander Command for executing ICDB queries
 * </p>
 * Created on 5/10/2016
 *
 * @author Dan Kondratyuk
 */
@Parameters(commandNames = {CommandLineArgs.EXECUTE}, commandDescription = "Execute queries on an ICDB schema")
public class ExecuteCommand implements ConfigCommand {

    @Parameter(names = { "-q", "--query" }, variableArity = true, description = "Execute one or more query arguments", splitter = QuerySplitter.class)
    private List<String> queries = new ArrayList<>();

    @Parameter(names = { "-s", "--schema" }, description = "Specify a custom database schema name")
    private String schemaName;

    @Parameter(names = { "-s", "--skip" }, description = "Skip one or more execution steps (convert|verify|execute)")
    private List<String> skipSteps = new ArrayList<>();

    @Parameter(names = { "-p", "--parallel" }, description = "Execute a query in parallel to improve performance (default true)")
    private Boolean parallel = true;


    @Override
    public void execute(@NotNull UserConfig userConfig) {

    }

    /**
     * Executes a query
     */
    private void executeQuery(CommandLineArgs cmd, UserConfig dbConfig) {
        final ExecuteCommand executeCommand = cmd.executeCommand;
        final String icdbSchema = dbConfig.icdbSchema;

        DBConnection icdb = DBConnection.connect(icdbSchema, dbConfig);

        String query = executeCommand.query;
        ICDBQuery icdbQuery = dbConfig.granularity.getQuery(query, icdb, dbConfig.codeGen);

        logger.info("Original Query: {}", query);

        QueryVerifier verifier = dbConfig.granularity.getVerifier(icdb, dbConfig);

        if (!icdbQuery.needsVerification()) {
            verifier.execute(icdbQuery);
        } else if (verifier.verify(icdbQuery)) {
            logger.info("Query verified");
            verifier.execute(icdbQuery);
        } else {
            logger.info(icdbQuery.getVerifyQuery());
            logger.info("Query failed to verify");
            logger.error(verifier.getError());
        }
    }

}
