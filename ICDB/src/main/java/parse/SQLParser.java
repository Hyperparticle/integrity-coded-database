package parse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.args.option.Granularity;
import net.sf.jsqlparser.JSQLParserException;

/**
 * @author ujwal-signature
 *
 */
public class SQLParser {
	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/";
	protected Granularity granularity = Granularity.TUPLE;
	private final Connection icdb;

	String query;
	String Schema;

	public SQLParser(String query, String schema, Granularity granularity, Connection icdb) throws JSQLParserException {
		this.query = query;
		this.Schema = schema;
		this.granularity = granularity;
		this.icdb = icdb;

	}

	public String parse() throws JSQLParserException {

		return "";

	}

	/**
	 * <p>
	 * get the primary key column for the table
	 * </p>
	 * 
	 * @param tablename
	 * @return
	 */
	public ArrayList<String> getPrimaryKeys(String tablename) {
		Connection connect = null;
		ArrayList<String> primarykeys = new ArrayList<String>();
		try {

			// This will load the MySQL driver, each DB has its own driver
			Class.forName(JDBC_DRIVER);

			// Setup the connection with the DB

			java.sql.DatabaseMetaData databaseMetaData = icdb.getMetaData();

			String tableName = tablename;

			String catalog_ = null;
			String schema = this.Schema;

			ResultSet result = databaseMetaData.getPrimaryKeys("", schema, tableName);
			// ResultSet result = databaseMetaData.getColumns(null, schema,
			// table_Name, null);

			while (result.next()) {
				primarykeys.add(result.getString(4));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {

			if (connect != null) {
				try {
					connect.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return primarykeys;

	}

	/**
	 * <p>
	 * get the list of attributes for the table
	 * </p>
	 * 
	 * @param tablename
	 * @return
	 */
	public List<String> getTableAttributes(String tablename) {
		List<String> AttributeList = new ArrayList<String>();
		Connection connect = null;
		try {
			// This will load the MySQL driver, each DB has its own driver
			Class.forName(JDBC_DRIVER);

			// Setup the connection with the DB
			java.sql.DatabaseMetaData databaseMetaData = icdb.getMetaData();

			String tableName = tablename;

			String table_Name = tableName;

			ResultSet resultSet = databaseMetaData.getColumns("", this.Schema, table_Name, "");
			while (resultSet.next()) {
				AttributeList.add(resultSet.getString("COLUMN_NAME"));
				String name = resultSet.getString("COLUMN_NAME");
				String type = resultSet.getString("TYPE_NAME");
				int size = resultSet.getInt("COLUMN_SIZE");

			}

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {

			if (connect != null) {
				try {
					connect.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return AttributeList;

	}

	public String strsToUpper(String str) {
		return str.toUpperCase();
	}

}
