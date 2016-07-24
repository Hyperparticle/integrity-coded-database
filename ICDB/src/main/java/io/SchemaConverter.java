package io;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import main.ICDBTool;
import main.args.config.UserConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;
import org.jooq.util.mysql.MySQLDataType;

import com.google.common.base.Stopwatch;

import main.args.ConvertDBCommand;
import main.args.option.Granularity;

/**
 * <p>
 * Converts a given DB schema to an ICDB using a JDBC connection
 * </p>
 * Created on 6/3/2016
 *
 * @author Dan Kondratyuk
 */
public class SchemaConverter {

	private final String dbName;
	private final String icdbName;

	private final DBConnection db;
	private final Granularity granularity;
	private final UserConfig dbConfig;

	private final boolean skipDuplicate;
	private final boolean skipSchema;

	private static final Logger logger = LogManager.getLogger();

	private SchemaConverter(DBConnection db, UserConfig dbConfig, ConvertDBCommand convertConfig) {
		this.dbName = dbConfig.schema;
		this.icdbName = dbConfig.icdbSchema;

		this.db = db;
		this.granularity = dbConfig.granularity;
		this.dbConfig = dbConfig;

		this.skipDuplicate = convertConfig.skipDuplicate;
		this.skipSchema = convertConfig.skipSchema;
	}

	public static void convertSchema(DBConnection db, UserConfig config, ConvertDBCommand convertConfig) {
		try {
			SchemaConverter converter = new SchemaConverter(db, config, convertConfig);
			converter.convertSchema();
		} catch (SQLException e) {
			logger.error("There was an error attempting to convert the schema: {}", e.getMessage());
			logger.debug(e.getStackTrace());
			System.exit(1);
		}
	}

	/**
	 * Creates an ICDB schema from an existing one. There are 2 steps, both of
	 * which are skippable: duplicating an existing database schema (no data),
	 * and converting the database schema to comply with ICDB standards
	 */
	private void convertSchema() throws SQLException {
		// Begin conversion by duplicating the original DB
		duplicateDB(dbName, icdbName);

		// Add extra columns and convert all data
		convertSchema(granularity.equals(Granularity.TUPLE));
	}

	private void convertSchema(final boolean oct) {
		if (skipSchema) {
			logger.debug("Schema conversion skipped");
			return;
		}

		logger.info("");
		logger.info("Converting DB schema to icdb");

		// Get the ICDB
		final DBConnection icdb = DBConnection.connect(icdbName, dbConfig);

        if (icdb == null) {
            return;
        }

		// Fetch all table names
		icdb.getTables().forEach(tableName -> {
			// For each table
			Table<?> icdbTable = icdb.getTable(tableName);

			// Add corresponding columns
			if (oct) {
				addOCTColumns(icdb, icdbTable);
			} else {
				addOCFColumns(icdb, icdbTable);
			}
		});
	}

	private void addOCTColumns(final DBConnection icdb, final Table<?> table) {
		boolean converted = Arrays.stream(table.fields()).anyMatch(field -> field.getName().equals(Format.IC_COLUMN));

		if (converted) {
			logger.debug("Table already converted. Skipping {}", table.getName());
			return;
		}

		// Create a svc column
		icdb.getCreate().alterTable(table).add(Format.IC_COLUMN, MySQLDataType.TINYBLOB).execute();

		// Create a serial column
		icdb.getCreate().alterTable(table).add(Format.SERIAL_COLUMN, SQLDataType.BIGINT).execute();
	}

	private void addOCFColumns(final DBConnection icdb, final Table<?> table) {
		boolean converted = Arrays.stream(table.fields())
				.anyMatch(field -> field.getName().endsWith(Format.IC_SUFFIX));

		if (converted) {
			logger.debug("Table already converted. Skipping {}", table.getName());
			return;
		}

		// Loop through each field and create a corresponding column
		Arrays.stream(table.fields()).forEach(field -> {
			// Create a svc column
			icdb.getCreate().alterTable(table).add(field.getName() + Format.IC_SUFFIX, MySQLDataType.TINYBLOB)
					.execute();

			// Create a serial column
			icdb.getCreate().alterTable(table).add(field.getName() + Format.SERIAL_SUFFIX, SQLDataType.BIGINT)
					.execute();
		});
	}

	/**
	 * Duplicates the schema by running a Bash script
	 */
	private void duplicateDB(String dbName, String icdbName) throws SQLException {
		if (skipDuplicate) {
			logger.debug("Database duplication skipped");
			return;
		}

        logger.info("");
		logger.info("Duplicating database {}", dbName);
		Stopwatch duplicationTime = Stopwatch.createStarted();

		try {
			new ProcessBuilder("bash", "./src/main/resources/scripts/duplicate-schema.sh", dbName, icdbName)
					.start()
					.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		logger.debug("Schema duplication time: {}", duplicationTime.elapsed(ICDBTool.TIME_UNIT));
	}

}
