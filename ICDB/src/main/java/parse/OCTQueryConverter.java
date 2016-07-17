/**
ujwal-signature
*/
package parse;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import convert.DBConnection;
import main.args.option.Granularity;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.SelectUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OCTQueryConverter implements QueryConverter {

    private final DBConnection icdb;
    private final CCJSqlParserManager parserManager = new CCJSqlParserManager();

    private final Logger logger = LogManager.getLogger();

	public OCTQueryConverter(DBConnection icdb) {
        this.icdb = icdb;
	}

	@Override
	public Statement convert(String query) {
	    logger.info("Converting query: {}", query);

	    try {
	        // Read and parse the query
            Reader reader = new StringReader(query);
            Statement statement = parserManager.parse(reader);

            // Convert based on statement type
            if (statement instanceof Select) {
                return convert((Select) statement);
            } else if (statement instanceof Insert) {
//                return convert((Insert) statement);
            } else if (statement instanceof Delete) {
//                return convert((Delete) statement);
            } else if (statement instanceof Update) {
//                return convert((Update) statement);
            } else {
                logger.error("SQL statement type not supported.");
            }
        } catch (JSQLParserException e) {
            logger.error("Failed to parse query: {}", e.getMessage());
        }

        return null;
	}

    /**
     * SELECT conversion. This effectively turns any SELECT query into a SELECT * query
     */
    private Statement convert(Select select) {
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

        List<SelectItem> selectList = new ArrayList<>();
        selectList.add(new AllColumns());

        // Convert query to a SELECT * to obtain all tuples
        plainSelect.setSelectItems(selectList);

        return select;
    }
//
//    /**
//     * INSERT conversion
//     */
//    private Statement convert(Insert insert) {
//        Insert insertStatement = (Insert) statement;
//        columns = insertStatement.getColumns();
//        table = insertStatement.getTable();
//        System.out.println(table.getDatabase().toString());
//        expressions = ((ExpressionList) insertStatement.getItemsList()).getExpressions();
//
//        // update the Query
//        updateColumnsAndValues(columns, expressions);
//        ICDBquery = insertStatement.toString();
//        System.out.println(insertStatement.toString());
//    }
//
//    /**
//     * DELETE conversion
//     */
//    private Statement convert(Delete delete) {
//        Delete deleteStatement = (Delete) statement;
//        table = deleteStatement.getTable();
//        Expression where = deleteStatement.getWhere();
//        // attributes for the verification of to be deleted data
//        List<String> AttributeList = getTableAttributes(table.getName());
//        // create a select query
//        Select select = SelectUtils.buildSelectFromTable(table);
//
//        // Generate a SELECT query to verify
//        select = SelectUtils.buildSelectFromTable(table);
//        // get the SELECT Clause
//        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
//        List<SelectItem> selectlist = new ArrayList<SelectItem>();
//        plainSelect.setSelectItems(selectlist);
//        plainSelect.setWhere(where);
//
//        for (String attribute : AttributeList) {
//            SelectUtils.addExpression(select, new Column(table, attribute));
//        }
//
//        ICDBquery = select.toString();
//    }
//
//    /**
//     * UPDATE conversion
//     */
//    private Statement convert(Update update) {
//        Update updateStatement = (Update) statement;
//        columns = updateStatement.getColumns();
//        String tableName = "";
//        List<Table> tables = null;
//        tables = ((Update) statement).getTables();
//        Select select = new Select();
//        for (Table tbl : tables) {
//            tableName = tbl.getName();
//            // attributes for the verification of to be deleted data
//            List<String> AttributeList = getTableAttributes(tableName);
//
//            // Generate a SELECT query to verify
//            select = SelectUtils.buildSelectFromTable(tbl);
//            // get the SELECT Clause
//            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
//            List<SelectItem> selectlist = new ArrayList<SelectItem>();
//            plainSelect.setSelectItems(selectlist);
//            plainSelect.setWhere(updateStatement.getWhere());
//
//            for (String attribute : AttributeList) {
//                SelectUtils.addExpression(select, new Column(attribute));
//            }
//
//        }
//
//        expressions = updateStatement.getExpressions();
//        // TODO get primary key value from SELECT fetch
//        String primaryKeyValue = "";
//
//        // generate the SVC column names and their values respective
//        updateColumnsAndValues(columns, expressions);
//        ICDBquery = select.toString();
//    }
//
//	/**
//	 * <p>
//	 * updates the column name and their respective values for the corresponding
//	 * original query to generate the ICDB query
//	 * </p>
//	 *
//	 * @param columns
//	 * @param expressions
//	 * @param PrimaryKey
//	 */
//
//	public void updateColumnsAndValues(List<Column> columns, List<Expression> expressions) {
//
//		List<Column> SVCcolumns = new ArrayList<Column>();
//		List<StringValue> SVCValues = new ArrayList<StringValue>();
//
//		int count = columns.size();
//		int j = 0;
//		String SerialNo = "12345";
//
//		Column newColumn = new Column("SVC");
//		SVCcolumns.add(newColumn);
//
//		StringBuilder svcMsgBuilder = new StringBuilder();
//		for (Expression exp : expressions) {
//			String value = exp.toString();
//			// check if the value is numeric
//			if (value.startsWith("\'") && value.endsWith("\'")) {
//				svcMsgBuilder.append(value.substring(1, value.length() - 1));
//			} else {
//				svcMsgBuilder.append(value);
//			}
//		}
//		svcMsgBuilder.append(SerialNo);
//		StringValue newValue = new StringValue("'" + svcMsgBuilder.toString() + "'");
//		// TODO encrypt the new value
//		SVCValues.add(newValue);
//
//		for (int i = 0; i < SVCcolumns.size(); i++) {
//			columns.add(SVCcolumns.get(i));
//			expressions.add(SVCValues.get(i));
//		}
//
//	}

}
