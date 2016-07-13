package convert;

import cipher.CodeGen;
import com.google.common.base.Stopwatch;
import main.args.ConvertDBCommand;
import main.args.config.ConfigArgs;
import main.args.config.UserConfig;
import main.args.option.Granularity;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *      A DBConverter exports data to a file from an existing database, converts it to an icdb-compliant data file (by
 *      generating signatures in the appropriate columns), and loads the data into a new icdb.
 * </p>
 * Created 5/8/2016
 *
 * @author Dan Kondratyuk
 */
public class DBConverter {

    private final String dbName;
    private final String icdbName;
    private final DBConnection db;
    private final DBConnection icdb;

    private final boolean skipData;
    private final boolean skipLoad;

    private final Granularity granularity;
    private final CodeGen codeGen;

    private final Path dataPath;
    private final Path convertedDataPath;

    private static final Logger logger = LogManager.getLogger();

    public DBConverter(DBConnection db, DBConnection icdb, UserConfig config, ConvertDBCommand convertConfig) {
        this.db = db;
        this.icdb = icdb;

        this.skipData = convertConfig.skipData;
        this.skipLoad = convertConfig.skipLoad;

        this.granularity = config.granularity;
        this.codeGen = config.codeGen;

        this.dbName = config.schema;
        this.icdbName = config.icdbSchema;

        this.dataPath = Paths.get(Format.DB_DATA_PATH);
        this.convertedDataPath = Paths.get(Format.ICDB_DATA_PATH);
    }

    /**
     * Begin the DB conversion process, with the assumption that a converted schema already exists on the DB server.
     */
    public void convert() {
        if (skipData) {
            logger.debug("Data conversion skipped");
            return;
        }

        logger.info("Converting data from {}", dbName);
        Stopwatch dataConvertTime = Stopwatch.createStarted();

        try {
            // 1. Export data outfile -> .csv files
            exportData();

            // 2. Read from file -> generate signature -> Write to file
            convertData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.debug("Total data conversion time: {}", dataConvertTime);
    }

    public void load() {
        if (skipLoad) {
            logger.debug("Data loading skipped");
            return;
        }

        logger.info("Migrating data to {}", icdbName);

        // 3. Load data infile -> icdb
        importData();
    }

    // TODO: move this to the fileconverter class
    private void exportData() throws IOException {
        Stopwatch dataExportTime = Stopwatch.createStarted();

        FileUtils.cleanDirectory(dataPath.toFile());

        db.getTables().forEach(tableName -> {
                Stopwatch exportTime = Stopwatch.createStarted();

                // For each table
                Table<?> icdbTable = db.getTable(tableName);

                // Get the output file path
                File outputFile = Paths.get(dataPath.toString(), tableName + Format.DATA_FILE_EXTENSION)
                        .toAbsolutePath().toFile();

                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                    // Output to a csv file
                    db.getCreate().selectFrom(icdbTable)
                        .fetch().formatCSV(output, Format.FILE_DELIMITER_CHAR, Format.MYSQL_NULL);
                } catch (IOException e) {
                    // TODO
                    e.printStackTrace();
                }

                logger.debug("Exported table {} in {}", tableName, exportTime);
            });

        logger.debug("Total db data export time: {}", dataExportTime);
    }

    private void convertData() throws IOException {
        FileUtils.cleanDirectory(convertedDataPath.toFile());

        FileConverter converter = new FileConverter(codeGen, granularity);

        // Walk data path
        Files.walk(dataPath)
            .filter(Files::isRegularFile)
            .forEach(path -> {
                File output = Paths.get(convertedDataPath.toString(), path.getFileName().toString()).toFile();
                converter.convertFile(path.toFile(), output);
            });
    }

    private void importData() {
        Stopwatch importDataTime = Stopwatch.createStarted();

        // Ignore foreign key constraints when migrating
        icdb.getCreate().execute("set FOREIGN_KEY_CHECKS = 0;");

        icdb.getTables().forEach(tableName -> {
            Stopwatch importTime = Stopwatch.createStarted();

            // For each table
            Table<?> icdbTable = icdb.getTable(tableName);

            // Get the output file path
            String filePath = Paths.get(convertedDataPath.toString(), tableName + Format.DATA_FILE_EXTENSION)
                    .toAbsolutePath().toFile()
                    .getAbsolutePath().replace("\\", "/");

//            try (InputStream input = new BufferedInputStream(new FileInputStream(inputFile))) {
                String query = "load data local infile '" + filePath + "' " +
                        "into table `" + tableName + "` " +
                        "fields terminated by '" + Format.FILE_DELIMITER + "' " +
                        "optionally enclosed by '"  + Format.ENCLOSING + "' " +
                        "lines terminated by '\n' " +
                        convertToBlob(icdbTable);

                // Truncate the table before loading the data
                icdb.getCreate().execute("truncate `" + tableName + "`;");

                try {
                    icdb.getCreate().execute(query);
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

            logger.debug("Imported table {} in {}", tableName, importTime);
        });

        // Don't forget to set foreign key checks back
        icdb.getCreate().execute("set FOREIGN_KEY_CHECKS = 1;");

        logger.debug("Total icdb data import time: {}", importDataTime);
    }

    /**
     * We need to augment the load query because MySQL is not smart enough to be able to load blob types in other encodings :(
     */
    private static String convertToBlob(Table<?> table) {
        StringBuilder builder = new StringBuilder();
        Field<?>[] fields = table.fields();

        List<String> setValues = new ArrayList<>(fields.length);

        builder.append("(");

        Arrays.stream(fields)
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

}
