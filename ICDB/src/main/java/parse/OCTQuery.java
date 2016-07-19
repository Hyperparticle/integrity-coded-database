package parse;

import cipher.CodeGen;
import cipher.signature.Sign;
import com.google.common.base.Charsets;
import convert.DBConnection;
import convert.DataConverter;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import org.jooq.tools.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * </p>
 * Created on 7/18/2016
 *
 * @author Dan Kondratyuk
 */
public class OCTQuery extends ICDBQuery {

    public OCTQuery(String query, DBConnection icdb, CodeGen codeGen) {
        super(query, icdb, codeGen);
    }

    ////////////
    // SELECT //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Select select) {
        return select; // Return the original query. // TODO: convert SELECT * to return all non-icdb columns
    }

    /**
     * SELECT conversion. This effectively turns any SELECT query into a SELECT * query
     */
    @Override
    protected Statement parseVerifyQuery(Select select) {
        return convertToSelectAll(select);
    }

    ////////////
    // INSERT //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Insert insert) {
        // Get expression list from query
        ItemsList itemsList = insert.getItemsList();
        List<Expression> expressions = ((ExpressionList) itemsList).getExpressions();

        // Obtain the data bytes
        final List<String> data = expressions.stream()
                .map(expression -> {
                    String[] trimmed = expression.toString().split("'");
                    return trimmed.length < 2 ? expression.toString() : trimmed[1];
                })
                .collect(Collectors.toList());
        final String dataString = StringUtils.join(data.toArray());
        final byte[] dataBytes = dataString.getBytes(Charsets.UTF_8);

        DataConverter converter = new DataConverter(dataBytes, codeGen, icrl);

        // Add base64 representation of signature to store it in the query properly
        final String signatureString = Sign.toBase64(converter.getSignature());
        expressions.add(new HexValue("from_base64('" + signatureString + "')"));

        // Add serial number to expression list
        lastSerial = converter.getSerial();
        expressions.add(new DoubleValue(lastSerial.toString()));

        return insert;
    }

    @Override
    protected Statement parseVerifyQuery(Insert insert) {
        return null; // Verifying an insert statement is not necessary
    }

    ////////////
    // DELETE //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Delete delete) {
        return null;
    }

    @Override
    protected Statement parseVerifyQuery(Delete delete) {
        return null;
    }

    ////////////
    // UPDATE //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Update update) {
        return null;
    }

    @Override
    protected Statement parseVerifyQuery(Update update) {
        return null;
    }

    /**
     * Converts a SELECT query to SELECT *
     */
    private Select convertToSelectAll(Select select) {
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

        List<SelectItem> selectList = new ArrayList<>();
        selectList.add(new AllColumns());

        // Convert query to a SELECT * to obtain all tuples
        plainSelect.setSelectItems(selectList);

        return select;
    }

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
