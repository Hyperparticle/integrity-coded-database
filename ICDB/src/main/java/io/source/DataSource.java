package io.source;

import java.util.stream.Stream;

/**
 * Interface exposing a simple way to obtain data from a given source.
 *
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public interface DataSource {

    /**
     * @return a stream originating from this source.
     */
    Stream<?> stream();

    /**
     * A strategy for fetching data. Data can be collected eagerly (all in advance), or processed lazily.
     */
    enum Fetch {
        EAGER, LAZY
    }

}
