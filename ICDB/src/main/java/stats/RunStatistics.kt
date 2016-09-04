package stats

import crypto.AlgorithmType
import io.source.DataSource
import main.args.option.Granularity

/**
 * Collects statistics about a run and outputs it to a CSV file
 *
 * Created on 9/4/2016
 * @author Dan Kondratyuk
 */
data class RunStatistics(
    val metadata: StatisticsMetadata
) {
    // Data collected as the run progresses
    var queryFetchSize: Long = 0
    var queryConversionTime: Long = 0
    var dataFetchTime: Long = 0
    var verificationTime: Long = 0

    fun list(): List<Long> = listOf(queryFetchSize, queryConversionTime, dataFetchTime, verificationTime)
}