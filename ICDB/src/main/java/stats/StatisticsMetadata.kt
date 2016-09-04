package stats

import crypto.AlgorithmType
import io.source.DataSource
import main.args.option.Granularity

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
    val dbQuery: String,
    val fetchType: DataSource.Fetch,
    val threads: Int
)