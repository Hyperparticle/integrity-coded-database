package verify;

import com.google.common.base.Stopwatch;
import convert.Format;
import main.args.ExecuteQueryCommand;
import main.args.config.Config;
import main.args.option.Granularity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     Verifies a SQL query
 * </p>
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public class QueryVerifier {

//    private final List<String> queries;
//    private final List<String> files;
//    private final String icdbName;
    private final String icdbQuery;
    private final Connection icdb;
    private final Granularity granularity;

    private static final Logger logger = LogManager.getLogger();

    public QueryVerifier(ExecuteQueryCommand command, Connection icdb, String icdbQuery) {
        this.icdbQuery = icdbQuery;
//        this.files = command.files;
        this.granularity = command.granularity;
//        this.icdbName = dbConfig.schema + Format.ICDB_SUFFIX;
        this.icdb = icdb;
    }

    public void execute() {
        Stopwatch queryVerificationTime = Stopwatch.createStarted();

        final DSLContext icdbCreate = DSL.using(icdb, SQLDialect.MYSQL);
//        final Schema icdbSchema = icdbCreate.meta().getSchemas().stream()
//                .filter(schema -> schema.getName().equals(icdbName))
//                .findFirst().get();

        Result<Record> result = icdbCreate.fetch(icdbQuery);

        logger.debug("Total query verification time: {}", queryVerificationTime);
    }

    private boolean verify() {
        return false;
    }


}
