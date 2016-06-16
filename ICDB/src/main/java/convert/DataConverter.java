package convert;

import com.google.common.base.Charsets;
import main.args.ConvertDataCommand;
import main.args.config.Config;
import main.args.option.Granularity;
import main.args.option.AlgorithmType;
import org.apache.commons.csv.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jooq.*;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.tools.StringUtils;
import org.jooq.util.derby.sys.Sys;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * <p>
 *     A DataConverter takes an input tuple and converts it into an ICDB tuple.
 * </p>
 * Created 5/8/2016
 *
 * @author Dan Kondratyuk
 */
public class DataConverter {

    private final String dbName;
    private final String icdbName;

    private final Connection db;
    private final Granularity granularity;
    private final AlgorithmType algorithm;
    private final byte[] key;

    private final Path dataPath;
    private final Path convertedDataPath;

    public DataConverter(Connection db, Config config) {
        this.db = db;
        this.granularity = config.granularity;
        this.algorithm = config.algorithm;
        this.key = config.key.getBytes(Charsets.UTF_8);

        this.dbName = config.schema;
        this.icdbName = config.schema + ICDB.ICDB_SUFFIX;

        this.dataPath = Paths.get("./tmp/db-files/data");
        this.convertedDataPath = Paths.get("./tmp/converted-db-files/data");
    }

    // TODO
    public void convert() {
        // Assumptions: duplicate icdb schema already exists

        // Grab the DB context
        final DSLContext dbCreate = DSL.using(db, SQLDialect.MYSQL);

        // Find the schemas
        final Schema dbSchema = dbCreate.meta().getSchemas().stream()
                .filter(schema -> schema.getName().equals(dbName))
                .findFirst().get();
        final Schema icdbSchema = dbCreate.meta().getSchemas().stream()
            .filter(schema -> schema.getName().equals(icdbName))
            .findFirst().get();

        try {
            // 1. Export data outfile -> .csv files
            exportData(dbCreate, dbSchema);

            // 2. Read from file -> generate signature -> Write to file
            convertData();

            // 3. Load data infile -> icdb
            importData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportData(final DSLContext dbCreate, final Schema dbSchema) throws IOException {
        FileUtils.cleanDirectory(dataPath.toFile());

        // Fetch all table names
        dbCreate.fetch("show full tables where Table_type = 'BASE TABLE'")
            .map(result -> result.get(0).toString())
            .forEach(tableName -> {
                // For each table
                Table<?> icdbTable = dbSchema.getTable(tableName);

                // Get the output file path
                File outputFile = Paths.get(dataPath.toString(), tableName + ICDB.DATA_EXT)
                        .toAbsolutePath().toFile();

                try (
                    OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))
                ) {
                    // Output to a csv file
                    dbCreate.selectFrom(icdbTable)
                        .fetch().formatCSV(output);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    private void convertData() throws IOException {
        // Walk data path
        Files.walk(dataPath)
            .filter(Files::isRegularFile)
            .forEach(path -> {
                File output = Paths.get(convertedDataPath.toString(), path.getFileName().toString()).toFile();
                convertFile(path.toFile(), output);
            });
    }

    private void convertFile(File input, File output) {
        StringBuilder builder = new StringBuilder();

        try (
            BufferedReader reader = new BufferedReader(new FileReader(input));
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        ) {
            // Parse the csv
            // TODO: use opencsv?
            Iterable<CSVRecord> records = CSVFormat.MYSQL.parse(reader);
            for (CSVRecord record : records) {
                builder.setLength(0);
                record.forEach(builder::append);

                byte[] signature = algorithm.generateSignature(
                    builder.toString().getBytes(Charsets.UTF_8), key
                );
                System.out.println();
            }

            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void importData() {

    }

//    private void insertOCTData(final DSLContext dbCreate, final Table<?> dbTable, final Table<?> icdbTable) {
//        dbCreate.fetch("select * from " + dbName + "." + dbTable.getName())
//                .parallelStream()
//                .forEach(result -> {
//                    // For each tuple, generate a signature
//                    byte[] signature = algorithm.generateSignature(
//                            StringUtils.join(result.intoArray()).getBytes(Charsets.UTF_8), key
//                    );
//
//                    List<Object> icdbTuple = new ArrayList<>(result.intoList());
//                    icdbTuple.add(signature);
//                    icdbTuple.add(new byte[] { 0x10 }); // TODO: generate serial number
//
//                    dbCreate.insertInto(icdbTable, Arrays.asList(icdbTable.fields()))
//                            .values(icdbTuple)
//                            .executeAsync();
//                });
////                    icdbCreate.execute("insert into " + table + "(svc)" + "values(" + )
//    }
}
