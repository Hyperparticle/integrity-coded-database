package parse;

import com.google.common.base.Stopwatch;
import crypto.CodeGen;
import io.DBConnection;
import io.Format;
import main.ICDBTool;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import stats.RunStatistics;
import stats.Statistics;
import verify.serial.AbstractIcrl;
import verify.serial.Icrl;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *      Represents an ICDB query. Encapsulates the converted query, plus a select query for verification.
 * </p>
 * Created on 7/16/2016
 *
 * @author Dan Kondratyuk
 */
public abstract class ICDBQuery {

    private final CCJSqlParserManager parserManager = new CCJSqlParserManager();
    protected final DBConnection icdb;
    protected final CodeGen codeGen;

    private final String originalQuery;   // The original query (SELECT, INSERT, DELETE, etc.)
    private String convertedQuery;  // The converted query, like the original, but with extra columns
    private String verifyQuery;     // A select query responsible for obtaining verification results

    protected AbstractIcrl icrl = Icrl.Companion.getIcrl();

    // Update the ICRL if this query was successful
    protected List<Long> serialsToBeRevoked = new ArrayList<>();

    public Map<String, String> columnOperation = new ConcurrentHashMap<String, String>();
    public boolean isAggregateQuery;
    private boolean requiresUpdate;

    private static final Logger logger = LogManager.getLogger();

    /**
     * Converts a given plain SQL statement into an ICDB statement
     */
    public ICDBQuery(String query, DBConnection icdb, CodeGen codeGen, RunStatistics statistics) {
        this.originalQuery = query;
        this.icdb = icdb;
        this.codeGen = codeGen;

        // Obtain ICDB queries
        Stopwatch queryConversionTime = Stopwatch.createStarted();
        this.verifyQuery = parse(originalQuery, QueryType.VERIFY);
        this.convertedQuery = parse(originalQuery, QueryType.CONVERT);

        statistics.setQueryConversionTime(queryConversionTime.elapsed(ICDBTool.TIME_UNIT));
        logger.debug("Query conversion time: {}", statistics.getQueryConversionTime());
    }

    private String parse(String query, QueryType queryType) {
        Statement statement = null;

        try {
            // Read and parse the query
            Reader reader = new StringReader(query);
            statement = parserManager.parse(reader);

            // Find the appropriate method to call based on the query and statement types
            if (statement instanceof Select) {
                statement = queryType.parseQuery((Select) statement, this);
            } else if (statement instanceof Insert) {
                statement = queryType.parseQuery((Insert) statement, this);
            } else if (statement instanceof Delete) {
                statement = queryType.parseQuery((Delete) statement, this);
            } else if (statement instanceof Update) {
                requiresUpdate = true;
                statement = queryType.parseQuery((Update) statement, this);
            } else {
                logger.error("SQL statement type not supported.");
            }
        } catch (JSQLParserException e) {
            logger.error("Failed to parse query");
            e.printStackTrace();
        }

        return statement == null ? "" : statement.toString();
    }

    protected abstract Statement parseConvertedQuery(Select select);
    protected abstract Statement parseConvertedQuery(Insert insert);
    protected abstract Statement parseConvertedQuery(Delete delete);
    protected abstract Statement parseConvertedQuery(Update update);

    protected abstract Statement parseVerifyQuery(Select select);
    protected abstract Statement parseVerifyQuery(Insert insert);
    protected abstract Statement parseVerifyQuery(Delete delete);
    protected abstract Statement parseVerifyQuery(Update update);

    // TODO: This is a temporary workaround. A better solution would be to pass a context object around with the results.
    protected Result<Record> deleteSelectResults;
    protected Result<Record> updateSelectResults;

//    /**
//     * Obtains data to verify the icdb query.
//     * Warning: verify() should be called before execute(), to properly verify data integrity.
//     * @param icdbCreate the context for executing queries
//     * @return a cursor into the requested data
//     */
//    public Cursor<Record> getVerifyData(DSLContext icdbCreate) {
//        if (requiresUpdate) {
//            updateSelectResults = icdbCreate.fetch(verifyQuery);
//        }
//
//        return icdbCreate.fetchLazy(verifyQuery);
//    }

    /**
     * Execute the original query.
     * Warning: verify() should be called before execute(), to properly verify data integrity.
     * @param icdbCreate the context for executing queries
     */
    public void execute(DSLContext icdbCreate) {
        if (requiresUpdate) {
            this.convertedQuery = parse(originalQuery, QueryType.CONVERT);
        }

        String result = icdbCreate.fetch(convertedQuery).toString();
        logger.info("{}\n{}", Format.limit(convertedQuery), result);

        // Add all pending serials
        icrl.commit();

        // Revoke all pending serials
        if (!serialsToBeRevoked.isEmpty()) {
            serialsToBeRevoked.forEach(serial -> icrl.revoke(serial));
            serialsToBeRevoked.clear();
        }
    }

    /**
     * Aggregate Query
     * Execute the original query and match with the computed data( if aggregate query).
     * Warning: verify() should be called before execute(), to properly verify data integrity.
     * @param icdbCreate the context for executing queries
     */
    public boolean executeandmatch(DSLContext icdbCreate, Map < String, Double > columnComputedValue) {
        if (requiresUpdate) {
            this.convertedQuery = parse(originalQuery, QueryType.CONVERT);
        }
        Result result=icdbCreate.fetch(convertedQuery);

        Set set = columnComputedValue.entrySet();
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry)iterator.next();
            logger.info(result.getValues(0));



            if (Double.parseDouble(result.getValues((String) entry.getKey()).get(0).toString())!=(Double) (entry.getValue())){
                return false;
            }
        }

        String resultstr = result.toString();
        logger.info("{}\n{}", Format.limit(convertedQuery), resultstr);

        // Add all pending serials
        icrl.commit();

        // Revoke all pending serials
        if (!serialsToBeRevoked.isEmpty()) {
            serialsToBeRevoked.forEach(serial -> icrl.revoke(serial));
            serialsToBeRevoked.clear();
        }
        return  true;

    }

    public String getConvertedQuery() {
        return convertedQuery;
    }

    public String getVerifyQuery() {
        return verifyQuery;
    }

    /**
     * @return true if this query needs verification
     */
    public boolean needsVerification() {
        return !verifyQuery.equals("");
    }

    /**
     * QueryType enums map a statement type (Select, Insert, etc.) with a query type (Convert, Verify) to convert the
     * given query statement appropriately.
     */
    private enum QueryType {
        CONVERT {
            @Override
            public Statement parseQuery(Select select, ICDBQuery icdbQuery) {
                return icdbQuery.parseConvertedQuery(select);
            }

            @Override
            public Statement parseQuery(Insert insert, ICDBQuery icdbQuery) {
                return icdbQuery.parseConvertedQuery(insert);
            }

            @Override
            public Statement parseQuery(Delete delete, ICDBQuery icdbQuery) {
                return icdbQuery.parseConvertedQuery(delete);
            }

            @Override
            public Statement parseQuery(Update update, ICDBQuery icdbQuery) {
                return icdbQuery.parseConvertedQuery(update);
            }
        },
        VERIFY {
            @Override
            public Statement parseQuery(Select select, ICDBQuery icdbQuery) {
                return icdbQuery.parseVerifyQuery(select);
            }

            @Override
            public Statement parseQuery(Insert insert, ICDBQuery icdbQuery) {
                return icdbQuery.parseVerifyQuery(insert);
            }

            @Override
            public Statement parseQuery(Delete delete, ICDBQuery icdbQuery) {
                return icdbQuery.parseVerifyQuery(delete);
            }

            @Override
            public Statement parseQuery(Update update, ICDBQuery icdbQuery) {
                return icdbQuery.parseVerifyQuery(update);
            }
        };

        public abstract Statement parseQuery(Select select, ICDBQuery icdbQuery);
        public abstract Statement parseQuery(Insert insert, ICDBQuery icdbQuery);
        public abstract Statement parseQuery(Delete delete, ICDBQuery icdbQuery);
        public abstract Statement parseQuery(Update update, ICDBQuery icdbQuery);
    }
}
