package parse.query;

import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;
import parse.QueryConverter;

import java.util.List;

/**
 * <p>
 *      Represents an ICDB query.
 * </p>
 * Created on 7/16/2016
 *
 * @author Dan Kondratyuk
 */
public class Query {

    private List<String> queries;
    private String selectIcdbQuery; // A select query responsible for obtaining verification results

    public Query(String statement, QueryConverter converter) {
        selectIcdbQuery = converter.convert(statement).toString();
    }

    public Query(String statement) {

    }

    public Cursor<Record> execute(DSLContext icdbCreate) {
        icdbCreate.fetchLazy(icdbQuery)
    }

}
