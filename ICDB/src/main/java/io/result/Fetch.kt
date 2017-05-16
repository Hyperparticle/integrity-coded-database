package io.result

/**
 * A strategy for fetching data. Data can be collected eagerly (all in advance), or processed lazily.
 *
 * Created on 9/27/2016
 * @author Dan Kondratyuk
 */
enum class Fetch {
    EAGER, LAZY
}