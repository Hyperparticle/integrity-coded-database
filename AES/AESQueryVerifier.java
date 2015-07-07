package AES;

import AES.helper.Symbol;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import java.sql.*;

public class AESQueryVerifier {
	
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://localhost:3333/";
	private static final String USER = "root";
	private static final String PASS = "spidermonkey";
	
	private File queryFile;
	private final String databaseName;
	private final String connectionURL;
	
	public AESQueryVerifier(File queryFile, String databaseName) {
		this.queryFile = queryFile;
		this.databaseName = databaseName;
		
		connectionURL = DB_URL + databaseName + "?allowMultiQueries=true";
	}
	
	public void verify() {
		System.out.println("Verifying " + queryFile.getName() + "...");
		
		ArrayList<String> queries = getQueries();
		
		try {
			Class.forName(JDBC_DRIVER);
			Connection databaseConnection = DriverManager.getConnection(connectionURL, USER, PASS);
			verifyQueries(databaseConnection, queries);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}
	
	public boolean verifyQueries(Connection connection, ArrayList<String> queries) throws SQLException {
		Statement statement = connection.createStatement();
		
		for (String query : queries) {
			statement.executeQuery(query);
		}
		
		return false;
	}
	
	public ArrayList<String> getQueries() {
		try {
			Scanner queryScan = new Scanner(queryFile);
			queryScan.useDelimiter(Symbol.SEMI_COLON);
			
			ArrayList<String> queries = new ArrayList<String>();
			
			while (queryScan.hasNext()) {
				queries.add(queryScan.next() + queryScan.delimiter());
			}
			
			queryScan.close();
			return queries;
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		return null;
	}
}
