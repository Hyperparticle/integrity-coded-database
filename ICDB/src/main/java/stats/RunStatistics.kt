package stats

/**
 * Collects statistics about a run
 *
 * Created on 9/4/2016
 * @author Dan Kondratyuk
 */
data class RunStatistics(
    // Data collected as the run progresses
    var queryFetchSize: Long = 0,
    var queryConversionTime: Long = 0,
    var dataFetchTime: Long = 0,
    var verificationTime: Long = 0,
    var aggregateOperationTime: Long = 0
) {
    fun list(): List<Long> = listOf(queryFetchSize, queryConversionTime, dataFetchTime, verificationTime, aggregateOperationTime)
}