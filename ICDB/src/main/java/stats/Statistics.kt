package stats

import org.apache.logging.log4j.LogManager
import org.supercsv.io.CsvListWriter
import org.supercsv.prefs.CsvPreference
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

/**
 * Collects a list of statistics runs and outputs them to file
 *
 * Created on 9/4/2016
 * @author Dan Kondratyuk
 */
class Statistics(private val metadata: StatisticsMetadata, private val outputFile: File) {

    // The runs are collected in a list
    private val runs = ArrayList<RunStatistics>()

    private val logger = LogManager.getLogger()

    /**
     * Add a run to the statistics list
     */
    fun addRun(run: RunStatistics) = runs.add(run)

    /**
     * Output the collected runs to a CSV file
     */
    fun outputRuns() {
        try {
            outputFile.parentFile.mkdirs()

            val writer = FileWriter(outputFile)
            val csvWriter = CsvListWriter(writer, CsvPreference.STANDARD_PREFERENCE)

            // Output metadata
            csvWriter.write(listOf(
                "Algorithm", "Granularity", "Schema Name", "DB Query", "Fetch Type", "Threads", "Date"
            ))
            csvWriter.write(metadata.list())

            // Output data
            csvWriter.write(listOf(
                "run", "Query Fetch Size", "Query Conversion Time", "Data Fetch Time",
                "Verification Time", "Execution Time", "Aggregate operation Time"
            ))
            runs.forEach { csvWriter.write(it.list()) }

            csvWriter.close()
            writer.close()
        } catch (e: IOException) {
            logger.error("Failed to output statistics to file: ", e.message)
        }
    }

}