package parse.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import parse.QueryConverter;

/**
 * <p>
 *      Represents an ICDB query.
 * </p>
 * Created on 7/16/2016
 *
 * @author Dan Kondratyuk
 */
public class ICDBQuery {

    private String originalQuery;   // The original query (SELECT, INSERT, DELETE, etc.)
    private String selectIcdbQuery; // A select query responsible for obtaining verification results

    private static Logger logger = LogManager.getLogger();

    /**
     * Converts a given plain SQL statement into an ICDB statement
     */
    public ICDBQuery(String statement, QueryConverter converter) {
        originalQuery = statement;
        selectIcdbQuery = converter.convert(statement).toString();
    }

    /**
     * Verifies the icdb query. Warning: this method should be before execute(), to ensure data integrity
     * @param icdbCreate the context for executing queries
     * @return a cursor into the requested data
     */
    public Cursor<Record> verify(DSLContext icdbCreate) {
        return icdbCreate.fetchLazy(selectIcdbQuery);
    }

    /**
     * Execute the original query. Warning: this method should be called after verify(), to ensure data integrity
     * @param icdbCreate the context for executing queries
     */
    public void execute(DSLContext icdbCreate) {
        logger.info(icdbCreate.fetch(originalQuery));
    }

}
