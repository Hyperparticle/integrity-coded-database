package source;

import java.util.List;

/**
 * Interface exposing a simple way to obtain data from a given source. The underling implementation should use lazy
 * fetching to enhance performance.
 *
 * Created 5/8/2016
 * @author Dan Kondratyuk
 */
public interface DataSource {

    // TODO: use streams instead
    List<String> next();

}
