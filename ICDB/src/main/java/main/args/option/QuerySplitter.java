/**
ujwal-signature
*/
package main.args.option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.converters.IParameterSplitter;

public class QuerySplitter implements IParameterSplitter {

	@Override
	public List<String> split(String value) {
		List<String> queryList = new ArrayList<>();
		String[] Queries = value.split(";");
		Collections.addAll(queryList, Queries);

		return queryList;
	}

}
