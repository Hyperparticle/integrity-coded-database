package io.result

import io.DBConnection
import org.jooq.Record
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * Represents a list of Records fetched from an ICDB table.
 * Contains convenience methods for extracting table/record/icdb data.
 *
 * Created on 9/27/2016
 * @author Dan Kondratyuk
 */
class IcdbResult private constructor(private val records: Stream<Record>) {



    companion object {
        fun fetch(icdb: DBConnection, fetchQuery: String, strategy: Fetch): IcdbResult {
            // Obtain a stream of the fetched data
            val records: Stream<Record> = when (strategy) {
                Fetch.EAGER -> {
                    val iterator = icdb.create.fetch(fetchQuery).iterator()
                    val spliterator = Spliterators.spliteratorUnknownSize(iterator.iterator(), Spliterator.ORDERED)
                    StreamSupport.stream(spliterator, false)
                }
                Fetch.LAZY -> icdb.create.fetchStream(fetchQuery)
            }

            return IcdbResult(records)
        }
    }

}

