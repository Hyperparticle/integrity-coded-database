package AES;

import java.io.File;

import AES.main.AESQueryVerifier;

public class QueryVerification {
	
	private static final int EXPECTED_ARGS = 2;

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
			System.err.println("Usage: java QueryVerification <QueryFile Path>");
			System.exit(1);
		}
	}
	
	private static void VerifyQuery(String[] args) {
		File queryFile = new File(args[0]);
		String databaseName = args[1];
		
		if (queryFile.exists() && queryFile.isFile() && queryFile.canRead()) {
			AESQueryVerifier verifier = new AESQueryVerifier(queryFile, databaseName);
			verifier.verify();
		} else {
			System.out.println("Error: unable to open QueryFile");
			System.exit(1);
		}
	}
}
