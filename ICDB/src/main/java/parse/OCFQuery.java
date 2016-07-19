package parse;

import cipher.CodeGen;
import convert.DBConnection;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

/**
 * <p>
 * </p>
 * Created on 7/18/2016
 *
 * @author Dan Kondratyuk
 */
public class OCFQuery extends ICDBQuery {

    public OCFQuery(String query, DBConnection icdb, CodeGen codeGen) {
        super(query, icdb, codeGen);
    }

    ////////////
    // SELECT //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Select select) {
        return select; // Return the original query. TODO: convert SELECT * to return all non-icdb columns
    }

    /**
     * SELECT conversion. This effectively turns any SELECT query into a SELECT * query
     */
    @Override
    protected Statement parseVerifyQuery(Select select) {
        return null;
    }

    ////////////
    // INSERT //
    ////////////

    @Override
    protected Statement parseConvertedQuery(Insert insert) {
        return null;
    }

    @Override
    protected Statement parseVerifyQuery(Insert insert) {
        return null;
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

    //	/**
//	 * <p>
//	 * generates an ICDB query for the provided original query
//	 * </p>
//	 *
//	 * @param query
//	 */
//	public String generateICDBQuery(String query) {
//		String ICDBquery = "";
//
//		CCJSqlParserManager pm = new CCJSqlParserManager();
//		net.sf.jsqlparser.statement.Statement statement = null;
//
//		List<Column> columns = null;
//		Table table = null;
//		List<Expression> expressions = null;
//		ArrayList<String> primarykeys = null;
//		try {
//			statement = pm.parse(new StringReader(query));
//
//			/////////// SELECT//////////////////
//			if (statement instanceof Select) {
//				Select selectStatement = (Select) statement;
//				// get tablename
//				StringBuilder builder = new StringBuilder();
//				builder.setLength(0);
//				TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
//				List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
//
//				for (String tbl : tableList) {
//					builder.append(tbl);
//				}
//				// get the SELECT Clause
//				PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
//				boolean isSelectAll = false;
//
//				for (SelectItem column : plainSelect.getSelectItems()) {
//					if (column.toString().equals("*"))
//						isSelectAll = true;
//					break;
//
//				}
//
//				if (isSelectAll == true) {
//
//					List<SelectItem> selectlist = new ArrayList<SelectItem>();
//					plainSelect.setSelectItems(selectlist);
//
//					for (String tbl : tableList) {
//						// attributes for the verification of to be selected
//						// data
//						List<String> AttributeList = getTableAttributes(tbl);
//						// add each of attributes to the SELECT query
//						for (String attribute : AttributeList) {
//
//							SelectUtils.addExpression(selectStatement, new Column(attribute));
//						}
//					}
//
//				} else {
//
//					// add Column_SVC for each column in select clause
//					// Iterate through the list of select Items
//					boolean hasTblplusColumn = false;
//					if (tableList.size() > 1)
//						hasTblplusColumn = true;
//					List<String> SVCList = new ArrayList<String>();
//					for (SelectItem column : plainSelect.getSelectItems()) {
//
//						SVCList.add(column.toString());
//					}
//
//					List<SelectItem> selectlist = new ArrayList<SelectItem>();
//					plainSelect.setSelectItems(selectlist);
//
//					for (String column : SVCList) {
//						SelectUtils.addExpression(selectStatement, new Column(column));
//						SelectUtils.addExpression(selectStatement, new Column(column + "_svc"));
//						SelectUtils.addExpression(selectStatement, new Column(column + "_serial"));
//					}
//					// add Columns from WHERE clause
//					if (plainSelect.getWhere() != null) {
//						String[] WhereParts = plainSelect.getWhere().toString().split("AND");
//						for (String parts : WhereParts) {
//							StringTokenizer tokenizer = new StringTokenizer(parts, " = ");
//							// check if the SVC list includes WHERE clause
//							// columns
//							String column = tokenizer.nextElement().toString();
//							if (!SVCList.contains(column)) {
//								SVCList.add(column);
//								SelectUtils.addExpression(selectStatement, new Column(column));
//								SelectUtils.addExpression(selectStatement, new Column(column + "_svc"));
//								SelectUtils.addExpression(selectStatement, new Column(column + "_serial"));
//							}
//						}
//					}
//
//					for (String tbl : tableList) {
//						// add primary key column if Query doesn't include the
//						// key
//						primarykeys = getPrimaryKeys(tbl);
//						for (String primaryKey : primarykeys) {
//							if (hasTblplusColumn) {
//								if (!SVCList.contains(tbl + "." + primaryKey) && primaryKey.length() != 0) {
//									SelectUtils.addExpression(selectStatement, new Column(new Table(tbl), primaryKey));
//									SelectUtils.addExpression(selectStatement,
//											new Column(new Table(tbl), primaryKey + "_svc"));
//									SelectUtils.addExpression(selectStatement,
//											new Column(new Table(tbl), primaryKey + "_serial"));
//								}
//							} else {
//								if (!SVCList.contains(primaryKey) && primaryKey.length() != 0) {
//									SelectUtils.addExpression(selectStatement, new Column(primaryKey));
//									SelectUtils.addExpression(selectStatement, new Column(primaryKey + "_svc"));
//									SelectUtils.addExpression(selectStatement, new Column(primaryKey + "_serial"));
//								}
//							}
//
//						}
//
//					}
//
//				}
//				ICDBquery = plainSelect.toString();
//			}
//			/////////// INSERT//////////////////
//			else if (statement instanceof Insert) {
//
//				Insert insertStatement = (Insert) statement;
//				columns = insertStatement.getColumns();
//				table = insertStatement.getTable();
//				expressions = ((ExpressionList) insertStatement.getItemsList()).getExpressions();
//
//				primarykeys = getPrimaryKeys(table.getName());
//
//				// update the Query
//				updateColumnsAndValues(columns, expressions, primarykeys, null);
//				ICDBquery = insertStatement.toString();
//			}
//			////////////// DELETE///////////////////////
//			else if (statement instanceof Delete) {
//				Delete deleteStatement = (Delete) statement;
//				table = deleteStatement.getTable();
//				Expression where = deleteStatement.getWhere();
//				// attributes for the verification of to be deleted data
//				List<String> AttributeList = getTableAttributes(table.getName());
//				// create a select query
//				Select select = SelectUtils.buildSelectFromTable(table);
//
//				// Generate a SELECT query to verify
//				select = SelectUtils.buildSelectFromTable(table);
//				// get the SELECT Clause
//				PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
//				List<SelectItem> selectlist = new ArrayList<SelectItem>();
//				plainSelect.setSelectItems(selectlist);
//				plainSelect.setWhere(where);
//
//				for (String attribute : AttributeList) {
//					SelectUtils.addExpression(select, new Column(table, attribute));
//				}
//
//				ICDBquery = select.toString();
//			}
//			////////////// UPDATE/////////////////////
//			else if (statement instanceof Update) {
//
//				Update updateStatement = (Update) statement;
//				columns = updateStatement.getColumns();
//				String tableName = "";
//				List<Table> tables = null;
//				tables = ((Update) statement).getTables();
//				Select select = new Select();
//				for (Table tbl : tables) {
//					tableName = tbl.getName();
//					// attributes for the verification of to be deleted data
//					List<String> AttributeList = getTableAttributes(tableName);
//
//					// Generate a SELECT query to verify
//					select = SelectUtils.buildSelectFromTable(tbl);
//					// get the SELECT Clause
//					PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
//					List<SelectItem> selectlist = new ArrayList<SelectItem>();
//					plainSelect.setSelectItems(selectlist);
//					plainSelect.setWhere(updateStatement.getWhere());
//
//					primarykeys = getPrimaryKeys(tableName);
//
//					if (granularity == granularity.FIELD) {
//
//						for (String primarykey : primarykeys) {
//							Column pkColumn = new Column(primarykey);
//							if (!columns.contains(pkColumn)) {
//								SelectUtils.addExpression(select, pkColumn);
//								SelectUtils.addExpression(select, new Column(primarykey + "_SVC"));
//							}
//
//						}
//
//						for (Column column : columns) {
//							SelectUtils.addExpression(select, column);
//							SelectUtils.addExpression(select, new Column(column.toString() + "_SVC"));
//						}
//					} else {
//						for (String attribute : AttributeList) {
//							SelectUtils.addExpression(select, new Column(attribute));
//						}
//					}
//
//				}
//
//				expressions = updateStatement.getExpressions();
//				// TODO get primary key value from SELECT fetch
//				String primaryKeyValue = "";
//
//				// generate the SVC column names and their values respective
//				updateColumnsAndValues(columns, expressions, primarykeys, primaryKeyValue);
//				ICDBquery = select.toString();
//			}
//
//		} catch (JSQLParserException e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//		return ICDBquery;
//
//	}
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
//	public void updateColumnsAndValues(List<Column> columns, List<Expression> expressions,
//			ArrayList<String> primarykeys, String PrimaryKeyValue) {
//		List<Column> SVCcolumns = new ArrayList<Column>();
//		List<StringValue> SVCValues = new ArrayList<StringValue>();
//
//		int count = columns.size();
//		int j = 0;
//		String SerialNo = "12345";
//
//		if (PrimaryKeyValue == null) {
//			int pkIndex = 0;
//			for (String primarykey : primarykeys) {
//				for (Column column : columns) {
//					if (column.getColumnName().equalsIgnoreCase(primarykey)) {
//						break;
//					}
//					pkIndex++;
//				}
//				String Value = expressions.get(pkIndex).toString();
//				if (Value.startsWith("\'") && Value.endsWith("\'")) {
//					if (PrimaryKeyValue == null) {
//						PrimaryKeyValue = Value.substring(1, Value.length() - 1);
//					} else {
//						PrimaryKeyValue += Value.substring(1, Value.length() - 1);
//					}
//
//				}
//			}
//
//		}
//
//		for (int i = 0; i < count; i++) {
//
//			Column newColumn = new Column(columns.get(j).getColumnName() + "_svc");
//			SVCcolumns.add(newColumn);
//
//			// SVC message = primary key+ attribute name+ attribute
//			// value + serial number
//			String value = expressions.get(j).toString();
//			// check if the value is numeric
//			if (value.startsWith("\'") && value.endsWith("\'")) {
//				StringValue newValue = new StringValue("'" + PrimaryKeyValue + columns.get(j).getColumnName()
//						+ value.substring(1, value.length() - 1) + SerialNo + "'");
//				// TODO encrypt the new value
//				SVCValues.add(newValue);
//			} else {
//				StringValue newValue = new StringValue(
//						"'" + PrimaryKeyValue + columns.get(j).getColumnName() + value + SerialNo + "'");
//
//				// TODO encrypt the new value
//				SVCValues.add(newValue);
//			}
//			j++;
//		}
//
//		for (int i = 0; i < SVCcolumns.size(); i++) {
//			columns.add(SVCcolumns.get(i));
//			expressions.add(SVCValues.get(i));
//		}
//
//	}

}
