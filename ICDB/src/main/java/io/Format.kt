package io

import org.apache.commons.lang3.StringUtils
import java.io.File
import java.nio.file.Paths

/**
 * Contains definitions for formatting
 *
 * Created on 6/13/2016
 * @author Dan Kondratyuk
 */
object Format {

    // SQL Naming
    const val IC_COLUMN = "ic"
    const val SERIAL_COLUMN = "serial"

    const val IC_SUFFIX = "_$IC_COLUMN"
    const val SERIAL_SUFFIX = "_$SERIAL_COLUMN"

    // File Naming
    const val DB_DATA_PATH = "./tmp/db-files/data"
    const val ICDB_DATA_PATH = "./tmp/icdb-files/data"

    const val DATA_FILE_EXTENSION = ".csv"

    // File Characters
    const val FILE_DELIMITER_CHAR = ','
    const val FILE_DELIMITER = FILE_DELIMITER_CHAR.toString()
    const val ENCLOSING_TAG = "\""
    const val MYSQL_NULL = "\\N"

    @JvmStatic fun getCsvFile(dataPath: String, tableName: String): File =
        Paths.get(dataPath, tableName + Format.DATA_FILE_EXTENSION)
            .toAbsolutePath().toFile()

    @JvmStatic fun limit(query: String): String =
            StringUtils.substring(query, 0, 100)

}
