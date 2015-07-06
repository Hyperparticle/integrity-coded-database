package AES;

public class QueryVerification {
	private static final int EXPECTED_ARGS = 1;
	
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://localhost:3333/";

	/**
	 * Tests the arguments and converts the specified DataFile.
	 * @param args
	 */
	public static void main(String[] args) {
		testArgs(args);
		VerifyQuery(args);
	}
	
	/**
	 * Ensures that the arguments specified by the user are correct.
	 * @param args
	 */
	private static void testArgs(String[] args) {
		if (args.length != EXPECTED_ARGS) {
			System.err.println("Usage: java DataConversion <Folder Path>");
			System.exit(1);
		}
	}
	
	private static void VerifyQuery(String[] args) {
		
	}
}
