package stats

import crypto.AlgorithmType
import io.source.DataSource
import main.args.option.Granularity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Metadata specified on run initialization
 *
 * Created on 9/4/2016
 * @author Dan Kondratyuk
 */
data class StatisticsMetadata(
    val algorithm: AlgorithmType,
    val granularity: Granularity,
    val schemaName: String,
    val fetchType: DataSource.Fetch,
    val threads: Int,
    val dbQuery: String = ""
) {
    private val date: String
        get() {
            val df = SimpleDateFormat("dd/MM/yy HH:mm:ss")
            val calendar = Calendar.getInstance()
            return df.format(calendar.time)
        }

    fun list(): List<String> = listOf(
        algorithm.toString(), granularity.toString(), schemaName, fetchType.toString(), threads.toString(), date, dbQuery
    )
}