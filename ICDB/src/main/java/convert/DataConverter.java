package convert;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import mac.Signature;
import main.args.config.Config;
import main.args.option.Granularity;
import main.args.option.AlgorithmType;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.tools.StringUtils;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    private final List<String> dbTableNames = new ArrayList<>();

    private final Connection db;
    private final Connection icdb;
    private final Granularity granularity;
    private final AlgorithmType algorithm;
    private final byte[] key;

    private final Path dataPath;
    private final Path convertedDataPath;

    private static final Logger logger = LogManager.getLogger();

    public DataConverter(Connection db, Connection icdb, Config config) {
        this.db = db;
        this.icdb = icdb;

        this.granularity = config.granularity;
        this.algorithm = config.algorithm;
        this.key = config.key.getBytes(Charsets.UTF_8);

        this.dbName = config.schema;
        this.icdbName = config.schema + ICDB.ICDB_SUFFIX;

        this.dataPath = Paths.get(ICDB.DB_DATA_PATH);
        this.convertedDataPath = Paths.get(ICDB.ICDB_DATA_PATH);
    }

    public void convert() {
        Stopwatch dataConvertTime = Stopwatch.createStarted();

        // Assumptions: duplicate icdb schema already exists

        // Grab the DB context
        final DSLContext dbCreate = DSL.using(db, SQLDialect.MYSQL);
        final DSLContext icdbCreate = DSL.using(icdb, SQLDialect.MYSQL);

        // Find the schemas
        final Schema dbSchema = dbCreate.meta().getSchemas().stream()
                .filter(schema -> schema.getName().equals(dbName))
                .findFirst().get();
        final Schema icdbSchema = dbCreate.meta().getSchemas().stream()
            .filter(schema -> schema.getName().equals(icdbName))
            .findFirst().get();

        dbTableNames.addAll(getTables(dbCreate));

        try {
            // 1. Export data outfile -> .csv files
            exportData(dbCreate, dbSchema);

            // 2. Read from file -> generate signature -> Write to file
            convertData();

            // 3. Load data infile -> icdb
            importData(icdbCreate, icdbSchema);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.debug("Total data conversion time: {}", dataConvertTime);
    }

    // TODO: pull this out into a service
    private Collection<String> getTables(final DSLContext dbCreate) {
        return dbCreate.fetch("show full tables where Table_type = 'BASE TABLE'")
            .map(result -> result.get(0).toString());
    }

    // TODO: move this to the fileconverter class
    private void exportData(final DSLContext dbCreate, final Schema dbSchema) throws IOException {
        FileUtils.cleanDirectory(dataPath.toFile());

        dbTableNames.forEach(tableName -> {
                // For each table
                Table<?> icdbTable = dbSchema.getTable(tableName);

                // Get the output file path
                File outputFile = Paths.get(dataPath.toString(), tableName + ICDB.DATA_EXT)
                        .toAbsolutePath().toFile();

                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                    // Output to a csv file
                    dbCreate.selectFrom(icdbTable)
                        .fetch().formatCSV(output, ICDB.FILE_DELIMITER_CHAR, ICDB.MYSQL_NULL);
                } catch (IOException e) {
                    // TODO
                    e.printStackTrace();
                }
            });
    }

    private void convertData() throws IOException {
        FileUtils.cleanDirectory(convertedDataPath.toFile());

        // Walk data path
        Files.walk(dataPath)
            .filter(Files::isRegularFile)
            .forEach(path -> {
                File output = Paths.get(convertedDataPath.toString(), path.getFileName().toString()).toFile();
                convertFile(path.toFile(), output);
            });
    }

    private void convertFile(final File input, final File output) {
        try (
            final Reader reader = new FileReader(input);
            final Writer writer = new FileWriter(output)
        ) {
            // Parse the csv
            final CsvPreference preference = CsvPreference.STANDARD_PREFERENCE;
            final CsvListReader csvReader = new CsvListReader(reader, preference);
            final CsvListWriter csvWriter = new CsvListWriter(writer, preference);

            // Discard the first line
            List<String> nextLine = csvReader.read();
            while ((nextLine = csvReader.read()) != null) {
                // Combine the list into a string
                final String data = StringUtils.join(nextLine);
                final byte[] dataBytes = data.getBytes(Charsets.UTF_8);

                // Generate the signature
                final byte[] signature = algorithm.generateSignature(dataBytes, key);
                final String signatureString = Signature.toBase64(signature);

                // TODO: add a serial
                final String serial = Signature.toBase64(new byte[] {0x33});

                // Write the line
                nextLine.add(signatureString);
                nextLine.add(serial);
                csvWriter.write(nextLine);
            }


            csvReader.close();
            csvWriter.close();
            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
    }

    private void importData(final DSLContext icdbCreate, final Schema icdbSchema) {
        // Ignore foreign key constraints when migrating
        icdbCreate.execute("set FOREIGN_KEY_CHECKS = 0;");

        dbTableNames.forEach(tableName -> {
            // For each table
            Table<?> icdbTable = icdbSchema.getTable(tableName);

            // Get the output file path
            String filePath = Paths.get(convertedDataPath.toString(), tableName + ICDB.DATA_EXT)
                    .toAbsolutePath().toFile()
                    .getAbsolutePath().replace("\\", "/");

//            try (InputStream input = new BufferedInputStream(new FileInputStream(inputFile))) {
                String query = "load data infile '" + filePath + "' " +
                        "into table `" + tableName + "` " +
                        "fields terminated by ',' " +
                        "optionally enclosed by '\"' " +
                        "lines terminated by '\n' " +
                        convertToBlob(icdbTable);

                // Truncate the table before loading the data
                icdbCreate.execute("truncate " + tableName + ";");

                try {
                    icdbCreate.execute(query);
                } catch (DataAccessException e) {
                    System.err.println(e.getMessage());// TODO
                }


//                icdbCreate.loadInto(icdbTable)
//                    .loadCSV(input, Charsets.UTF_8)
//                    .fields(icdbTable.fields())
//                    .execute();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        });

        // Don't forget to set foreign key checks back
        icdbCreate.execute("set FOREIGN_KEY_CHECKS = 1;");
    }

    /**
     * We need to augment the load query because MySQL is too stupid to be able to load blob types in any encoding :(
     */
    private static String convertToBlob(Table<?> table) {
        StringBuilder builder = new StringBuilder();
        Field<?>[] fields = table.fields();

        List<String> setValues = new ArrayList<>(fields.length);

        builder.append("(");

        Arrays.stream(table.fields())
                .forEach(field -> {
                    DataType<?> dataType = field.getDataType().getSQLDataType();

                    if (dataType.equals(SQLDataType.BLOB) || dataType.equals(SQLDataType.OTHER)) {
                        builder.append("@");
                        setValues.add(field.getName());
                    }
                    builder.append(field.getName())
                        .append(",");
                });

        builder.setLength(builder.length()-1);
        builder.append(") SET ");

        setValues.stream()
                .forEach(set -> builder.append(set)
                        .append("=FROM_BASE64(@")
                        .append(set)
                        .append("),")
                );

        builder.setLength(builder.length()-1);
        builder.append(";");

        return builder.toString();
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
