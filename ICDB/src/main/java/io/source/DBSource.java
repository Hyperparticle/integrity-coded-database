package io.source;

import io.DBConnection;
import org.jooq.Record;
import java.util.stream.Stream;

/**
 * DataSource that obtains data from a database via a fetch query.
 *
 * Created on 7/24/2016
 * @author Dan Kondratyuk
 */
public class DBSource implements DataSource {

    public static Stream<Record> stream(DBConnection icdb, String fetchQuery, Fetch strategy) {
        return new DBSource(icdb, fetchQuery, strategy).stream();
    }

    private final DBConnection icdb;
    private final String fetchQuery;
    private final Fetch strategy;

    private DBSource(DBConnection icdb, String fetchQuery, Fetch strategy) {
        this.icdb = icdb;
        this.fetchQuery = fetchQuery;
        this.strategy = strategy;
    }

    @Override
    public Stream<Record> stream() {
        switch (strategy) {
            case EAGER: return icdb.getCreate()
                    .fetch(fetchQuery)
                    .stream();
            case LAZY: return icdb.getCreate()
                    .fetchStream(fetchQuery);
            default: return Stream.empty();
        }
    }

}
