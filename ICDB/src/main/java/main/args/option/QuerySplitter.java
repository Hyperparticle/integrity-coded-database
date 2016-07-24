/**
ujwal-signature
*/
package main.args.option;

import java.util.Arrays;
import java.util.List;
import com.beust.jcommander.converters.IParameterSplitter;

/**
 * Splits the supplied query String into individual queries (by semicolon)
 *
 * Created on 6/8/2016
 * @author Dan Kondratyuk
 */
public class QuerySplitter implements IParameterSplitter {

	@Override
	public List<String> split(String value) {
		String[] queries = value.split(";");
		return Arrays.asList(queries);
	}

}
