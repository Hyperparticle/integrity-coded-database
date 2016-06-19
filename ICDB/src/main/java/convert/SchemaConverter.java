package convert;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import main.args.ConvertDBCommand;
import main.args.config.Config;
import main.args.option.AlgorithmType;
import main.args.option.Granularity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import org.jooq.conf.Settings;
import org.jooq.impl.*;
import org.jooq.util.mysql.MySQLDataType;

import java.io.IOException;
import java.sql.*;
import java.util.Arrays;

/**
 * <p>
 *      Converts a given DB schema to an ICDB using a JDBC connection
 * </p>
 * Created on 6/3/2016
 *
 * @author Dan Kondratyuk
 */
public class SchemaConverter {

    private final String dbName;
    private final String icdbName;

    private final Connection db;
    private final Granularity granularity;

    private final boolean skipDuplicate;
    private final boolean skipSchema;

    private static final Logger logger = LogManager.getLogger();

    public SchemaConverter(Connection db, Config config, ConvertDBCommand convertConfig) {
        this.dbName = config.schema;
        this.icdbName = config.schema + ICDB.ICDB_SUFFIX;

        this.db = db;
        this.granularity = config.granularity;

        this.skipDuplicate = convertConfig.skipDuplicate;
        this.skipSchema = convertConfig.skipSchema;
    }

    public void convert() throws SQLException {
        if (!skipDuplicate) {
            logger.debug("Duplicating database");
            // Begin conversion by duplicating the original DB
            duplicateDB(dbName, icdbName);
        } else {
            logger.debug("Schema duplication skipped");
        }

        if (!skipSchema) {
            logger.debug("Converting schema to icdb");

            // Grab the DB context
            final DSLContext dbCreate = DSL.using(db, SQLDialect.MYSQL);

            // Add extra columns and convert all data
            convert(dbCreate, granularity.equals(Granularity.TUPLE));
        } else {
            logger.debug("Schema conversion skipped");
        }
    }

    private void convert(final DSLContext dbCreate, final boolean oct) {
        // Find the ICDB schema
        final Schema icdbSchema = dbCreate.meta().getSchemas().stream()
            .filter(schema -> schema.getName().equals(icdbName))
            .findFirst().get();

        // Fetch all table names
        // TODO: create service
        dbCreate.fetch("show full tables where Table_type = 'BASE TABLE'")
            .map(result -> result.get(0).toString())
            .forEach(tableName -> {
                // For each table
                Table<?> icdbTable = icdbSchema.getTable(tableName);

                // Add corresponding columns
                if (oct) {
                    addOCTColumns(dbCreate, icdbTable);
                } else {
                    addOCFColumns(dbCreate, icdbTable);
                }
            });
    }

    private void addOCTColumns(final DSLContext dbCreate, final Table<?> table) {
        // Create a svc column
        dbCreate.alterTable(table)
            .add(ICDB.SVC, MySQLDataType.TINYBLOB)
            .executeAsync();

        // Create a serial column
        dbCreate.alterTable(table)
            .add(ICDB.SERIAL, MySQLDataType.TINYBLOB)
            .executeAsync();
    }

    private void addOCFColumns(final DSLContext dbCreate, final Table<?> table) {
        // Loop through each field and create a corresponding column
        Arrays.asList(table.fields()).stream()
            .forEach(field -> {
                // Create a svc column
                dbCreate.alterTable(table)
                    .add(field.getName() + ICDB.SVC_SUFFIX, SQLDataType.BLOB)
                    .executeAsync();

                // Create a serial column
                dbCreate.alterTable(table)
                    .add(field.getName() + ICDB.SERIAL_SUFFIX, SQLDataType.BLOB)
                    .executeAsync();
            });
    }


    /**
     * Duplicates the schema by running a Bash script
     */
    private static void duplicateDB(String dbName, String icdbName) throws SQLException {
        Stopwatch duplicationTime = Stopwatch.createStarted();

        try {
            new ProcessBuilder(
                "bash",
                "./src/main/resources/scripts/duplicate-schema.sh",
                dbName,
                icdbName
            ).start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        logger.debug("Schema duplication time: {}", duplicationTime);
    }

}
