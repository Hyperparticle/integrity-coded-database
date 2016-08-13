package io.destination

import org.supercsv.io.CsvListWriter
import org.supercsv.prefs.CsvPreference
import java.io.File
import java.io.FileWriter
import java.util.stream.Stream

/**
 * DataDestination that writes lines to a CSV file
 *
 * Created on 7/24/2016
 * @author Dan Kondratyuk
 */
class FileDestination(output: File) : DataDestination {

    private val writer = FileWriter(output)
    private val csvWriter = CsvListWriter(writer, CsvPreference.STANDARD_PREFERENCE)

    override fun write(data: Stream<List<String>>) =
        data.onClose { csvWriter.close(); writer.close() }
            .skip(1) // TODO: skip first as explicit parameter
            .forEach { csvWriter.write(it) }
}
