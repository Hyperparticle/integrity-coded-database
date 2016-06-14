package convert;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sun.javaws.exceptions.InvalidArgumentException;
import main.ICDBTool;
import main.args.config.Config;
import main.args.option.AlgorithmType;
import main.args.option.Granularity;
import org.apache.commons.lang3.ArrayUtils;
import org.jooq.*;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.*;
import org.jooq.tools.StringUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 *      Converts a given DB schema to an ICDB using a JDBC connection
 * </p>
 * Created on 6/3/2016
 *
 * @author Dan Kondratyuk
 */
public class SchemaConverter {

    private final Schema dbSchema;
    private final Schema icdbSchema;

    private final Connection db;
    private final Granularity granularity;
    private final AlgorithmType algorithm;
    private final byte[] key;

    public SchemaConverter(Connection db, Config config) {
        this.dbSchema = new SchemaImpl(config.schema, new CatalogImpl(""));
        this.db = db;
        this.granularity = config.granularity;
        this.algorithm = config.algorithm;
        this.key = config.key.getBytes(Charsets.UTF_8);

        this.icdbSchema = new SchemaImpl(config.schema + ICDB.SUFFIX, new CatalogImpl(""));
    }

    public void convert() throws SQLException {
        // Begin conversion by duplicating the original DB
        final Connection icdb = duplicateDB();

        Settings settings = new Settings();
//        settings.setRenderNameStyle(RenderNameStyle.AS_IS);

        // Grab the DB context
        final DSLContext dbCreate = DSL.using(db, SQLDialect.MYSQL, settings);
        final DSLContext icdbCreate = DSL.using(icdb, SQLDialect.MYSQL, settings);

        // Add extra columns and convert all data
        if (granularity.equals(Granularity.TUPLE)) {        // OCT conversion
            convertOCT(dbCreate, icdbCreate);
        } else if (granularity.equals(Granularity.FIELD)) { // OCF conversion
            convertOCF(dbCreate);
        } else {
            throw new InvalidParameterException(granularity.toString() + " is not a recognized granularity level");
        }
    }

    private void convertOCT(final DSLContext dbCreate, final DSLContext icdbCreate) {
        icdbCreate.fetch("show full tables where Table_type = 'BASE TABLE'")     // Fetch all table names
//                .parallelStream()
                .map(result -> result.get(0).toString())
                .forEach(tableName -> {                            // For each table,
//                    icdbCreate.alterTable(table)                 // Create a svc column
//                        .add(ICDB.SVC, SQLDataType.BLOB)
//                        .execute();
//                    icdbCreate.alterTable(table)                 // Create a serial column
//                        .add(ICDB.SERIAL, SQLDataType.BLOB)
//                        .execute();

                    // TODO: pull this out
                    Table<?> table = icdbCreate.meta().getTables().stream()
                            .filter(t -> t.getName().equals(tableName))
                            .findFirst().get();

                    dbCreate.fetch("select * from " + tableName)
//                        .parallelStream()
                        .forEach(result -> {
                            // For each tuple, generate a signature
                            byte[] signature = algorithm.generateSignature(
                                StringUtils.join(result.intoArray()).getBytes(Charsets.UTF_8), key
                            );

                            List<Object> icdbTuple = new ArrayList<>(result.intoList());
                            icdbTuple.add(signature);
                            icdbTuple.add(new byte[] { 0x10 }); // TODO: generate serial number

                            icdbCreate.insertInto(table, Arrays.asList(table.fields()))
                                    .values(icdbTuple)
                                    .executeAsync();
                        });
//                    icdbCreate.execute("insert into " + table + "(svc)" + "values(" + )
                });
    }

    // TODO: loop through each column
    private void convertOCF(final DSLContext create) {
        throw new NotImplementedException();

//        create.fetch("show full tables where Table_type = 'BASE TABLE'")     // Fetch all table names
//                .map(result -> result.get(0).toString())
//                .forEach(table -> {                                          // For each table,
//                    create.alterTable(table)
//                            .addColumn()
//
//                    create.alterTable(table)                                 // Create a svc column
//                            .add(ICDB.SVC, SQLDataType.BLOB)
//                            .execute();
//                    create.alterTable(table)                                 // Create a serial column
//                            .add(ICDB.SERIAL, SQLDataType.BLOB)
//                            .execute();
//                });
    }


    /**
     * Duplicates the schema by running a Bash script
     */
    private Connection duplicateDB() throws SQLException {
//        try {
//            new ProcessBuilder(
//                "bash",
//                "./src/main/resources/scripts/duplicate-schema.sh",
//                dbSchema,
//                icdbSchema
//            ).start();
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }

        return DBConnection.connect(icdbSchema.getName());
    }

}
