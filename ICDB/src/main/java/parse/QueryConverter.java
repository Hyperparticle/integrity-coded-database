package parse;

import net.sf.jsqlparser.statement.Statement;

/**
 * <p>
 * 		Converts a SQL query into an ICDB SQL query.
 * </p>
 * Created 5/8/2016
 * 
 * @author Dan Kondratyuk
 */
public interface QueryConverter {

	/**
     * Convert a query to an ICDB Query
	 */
    Statement convert(String query);

}
