package io.destination

import java.util.stream.Stream

/**
 * Interface exposing a simple way to write

 * Created on 7/24/2016

 * @author Dan Kondratyuk
 */
@FunctionalInterface
interface DataDestination {

    /**
     * Write the data to the destination
     */
    fun write(data: Stream<List<String>>)

}
