/**
ujwal-signature
*/
package main.args.option;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.converters.IParameterSplitter;

public class QuerySplitter implements IParameterSplitter {

	@Override
	public List<String> split(String value) {
		List<String> queryList = new ArrayList<String>();
		String[] Queries = value.split(";");
		for (String query : Queries) {
			queryList.add(query);
		}
		// TODO Auto-generated method stub
		return queryList;
	}

}
