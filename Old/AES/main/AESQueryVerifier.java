package AES.main;

import AES.helper.Symbol;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
	private final String dataPath;
	
	public AESQueryVerifier(File queryFile, String databaseName) {
		this.queryFile = queryFile;
		this.databaseName = databaseName;
		
		dataPath = queryFile.getParent();
		
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
	
	public void verifyQueries(Connection connection, ArrayList<String> queries) throws SQLException {
		boolean verified = false;
		Statement statement = connection.createStatement();
		
		AESDataFileVerifier verifier = new AESDataFileVerifier(dataPath);
		
		for (String query : queries) {
			ResultSet result = statement.executeQuery(query);
			ResultSetMetaData resultMeta = result.getMetaData();
			int colCount = resultMeta.getColumnCount();
			
			String[] data = new String[colCount];
			
			if (!query.contains("use")) {
				long row = 0;
				
				while (result.next()) {
					for (int i = 1; i <= data.length; i++) {
						data[i-1] = result.getString(i);
					}
					
					String icCode = data[data.length-1];
					String[] dataCpy = Arrays.copyOf(data, data.length-1);
					
					verifier.verify(dataCpy, icCode, row);
					row++;
				}
			}
		}
		
		if (verified)
			System.out.println("All statements verified");
		else
			System.out.println("Statements not verified");
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
	
	private static File getKeyFile(String fileLocation) {
		File dir = new File(fileLocation);
		File [] files = dir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(Symbol.AES_KEY_FILE_EXTENSION);
		    }
		});
		
		return files[0];
	}
}
