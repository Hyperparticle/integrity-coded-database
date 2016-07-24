package io;

import crypto.CodeGen;
import com.google.common.base.Stopwatch;
import main.ICDBTool;
import main.args.ConvertDBCommand;
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
import java.util.stream.Collectors;

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

    private final boolean skipExport;
    private final boolean skipConvert;
    private final boolean skipLoad;

    private final Granularity granularity;
    private final CodeGen codeGen;

    private final Path dataPath;
    private final Path convertedDataPath;

    private static final Logger logger = LogManager.getLogger();

    public DBConverter(DBConnection db, DBConnection icdb, UserConfig config, ConvertDBCommand convertConfig) {
        this.db = db;
        this.icdb = icdb;

        this.skipExport = convertConfig.skipExport;
        this.skipConvert = convertConfig.skipConvert;
        this.skipLoad = convertConfig.skipLoad;

        this.granularity = config.granularity;
        this.codeGen = config.codeGen;

        this.dbName = config.schema;
        this.icdbName = config.icdbSchema;

        this.dataPath = Paths.get(Format.DB_DATA_PATH);
        this.convertedDataPath = Paths.get(Format.ICDB_DATA_PATH);
    }

    public void convertAll() {
        export();  // 1. Export data outfile -> .csv files
        convert(); // 2. Read from file -> generate signature -> Write to file
        load();    // 3. Load data infile -> icdb
    }

    private void export() {
        if (skipExport) {
            logger.debug("Data export skipped");
            return;
        }

        try {
            logger.info("");
            logger.info("Exporting data from {}", dbName);
            Stopwatch dataExportTime = Stopwatch.createStarted();
            exportData();
            logger.debug("Total data export time: {}", dataExportTime.elapsed(ICDBTool.TIME_UNIT));
        } catch (IOException e) {
            logger.error("Failed to export DB {}: {}", dbName, e.getMessage());
        }
    }

    /**
     * Begin the DB conversion process, with the assumption that a converted schema already exists on the DB server.
     */
    private void convert() {
        if (skipConvert) {
            logger.debug("Data conversion skipped");
            return;
        }

        try {
            logger.info("");
            logger.info("Converting data from {}", dbName);
            Stopwatch dataConversionTime = Stopwatch.createStarted();
            convertData();
            logger.debug("Total data convert time: {}", dataConversionTime.elapsed(ICDBTool.TIME_UNIT));
        } catch (IOException e) {
            logger.error("Failed to convert DB {}: {}", dbName, e.getMessage());
        }
    }

    private void load() {
        if (skipLoad) {
            logger.debug("Data loading skipped");
            return;
        }

        logger.info("");
        logger.info("Loading converted data into {}", icdbName);
        Stopwatch dataLoadTime = Stopwatch.createStarted();
        importData();
        logger.debug("Total data load time: {}", dataLoadTime.elapsed(ICDBTool.TIME_UNIT));
    }

    private void exportData() throws IOException {
        Stopwatch dataExportTime = Stopwatch.createStarted();

        FileUtils.cleanDirectory(dataPath.toFile());

        db.getTables().forEach(tableName -> {
                Stopwatch exportTime = Stopwatch.createStarted();

                // For each table
                Table<?> icdbTable = db.getTable(tableName);

                // Get the output file path
                File outputFile = Format.getCsvFile(dataPath.toString(), tableName);

                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                    // Output to a csv file
                    db.getCreate().selectFrom(icdbTable)
                        .fetch().formatCSV(output, Format.FILE_DELIMITER_CHAR, Format.MYSQL_NULL);

                    logger.debug("Exported table {} in {}", tableName, exportTime.elapsed(ICDBTool.TIME_UNIT));
                } catch (IOException e) {
                    logger.error("Failed to export table {}: {}", tableName, e.getMessage());
                }
            });

        logger.debug("Total db data export time: {}", dataExportTime.elapsed(ICDBTool.TIME_UNIT));
    }

    private void convertData() throws IOException {
        FileUtils.cleanDirectory(convertedDataPath.toFile());

        FileConverter converter = new FileConverter(codeGen, granularity);

        // Find all files in the data path
        // TODO: parallelize even more by fetching each table into a stream
        Files.walk(dataPath)
            .filter(Files::isRegularFile)
            .collect(Collectors.toList())
            .parallelStream() // Convert in parallel
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
            String filePath = Format.getCsvFile(convertedDataPath.toString(), tableName)
                    .getAbsolutePath().replace("\\", "/");

//            try (InputStream input = new BufferedInputStream(new FileInputStream(inputFile))) {
                String query = "load data local infile '" + filePath + "' " +
                        "into table `" + tableName + "` " +
                        "fields terminated by '" + Format.FILE_DELIMITER + "' " +
                        "optionally enclosed by '"  + Format.ENCLOSING_TAG + "' " +
                        "lines terminated by '\n' " +
                        convertToBlob(icdbTable);

                // Truncate the table before loading the data
                icdb.getCreate().execute("truncate `" + tableName + "`;");

                try {
                    icdb.getCreate().execute(query);
                    logger.debug("Imported table {} in {}", tableName, importTime.elapsed(ICDBTool.TIME_UNIT));
                } catch (DataAccessException e) {
                    logger.error("Failed to import table {}: {}", tableName, e.getMessage());
                }
        });

        // Don't forget to set foreign key checks back
        icdb.getCreate().execute("set FOREIGN_KEY_CHECKS = 1;");

        logger.debug("Total icdb data import time: {}", importDataTime.elapsed(ICDBTool.TIME_UNIT));
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

        setValues.forEach(set -> builder.append(set)
                .append("=FROM_BASE64(@")
                .append(set)
                .append("),")
        );

        builder.setLength(builder.length()-1);
        builder.append(";");

        return builder.toString();
    }

}
