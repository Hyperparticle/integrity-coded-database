package parse;

import cipher.CodeGen;
import convert.DBConnection;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import verify.ICRL;

import java.io.Reader;
import java.io.StringReader;

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
    private final String convertedQuery;  // The converted query, like the original, but with extra columns
    private final String verifyQuery;     // A select query responsible for obtaining verification results

    protected ICRL icrl = ICRL.getInstance();
    protected Long lastSerial; // Keep a reference to the last serial, updating the ICRL if this query was successful

    private static Logger logger = LogManager.getLogger();

    /**
     * Converts a given plain SQL statement into an ICDB statement
     */
    public ICDBQuery(String query, DBConnection icdb, CodeGen codeGen) {
        this.originalQuery = query;
        this.icdb = icdb;
        this.codeGen = codeGen;

        // Obtain ICDB queries
        // Parse each query twice to obtain two copies
        this.convertedQuery = parse(query, QueryType.CONVERT);
        this.verifyQuery = parse(query, QueryType.VERIFY);
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

    /**
     * Obtains data to verify the icdb query.
     * Warning: verify() should be called before execute(), to properly verify data integrity.
     * @param icdbCreate the context for executing queries
     * @return a cursor into the requested data
     */
    public Cursor<Record> getVerifyData(DSLContext icdbCreate) {
        return icdbCreate.fetchLazy(verifyQuery);
    }

    /**
     * Execute the original query.
     * Warning: verify() should be called before execute(), to properly verify data integrity.
     * @param icdbCreate the context for executing queries
     */
    public void execute(DSLContext icdbCreate) {
        String result = icdbCreate.fetch(convertedQuery).toString();
        logger.info("{}\n{}", convertedQuery, result);

        if (lastSerial != null) {
            icrl.add(lastSerial);
            lastSerial = null;
        }
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
