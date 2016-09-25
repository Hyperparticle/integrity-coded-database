package stats

/**
 * Collects statistics about a run
 *
 * Created on 9/4/2016
 * @author Dan Kondratyuk
 */
data class RunStatistics(
    // Data collected as the run progresses
    var run: Long = 0,
    var queryFetchSize: Long = 0,
    var queryConversionTime: Long = 0,
    var dataFetchTime: Long = 0,
    var verificationTime: Long = 0,
    var executionTime: Long = 0,
    var aggregateOperationTime: Long = 0
) {
    fun list(): List<Long> = listOf(
        run,
        queryFetchSize,
        queryConversionTime,
        dataFetchTime,
        verificationTime,
        executionTime,
        aggregateOperationTime
    )
}