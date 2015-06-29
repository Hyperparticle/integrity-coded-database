package AES;

import java.io.File;

public class DataVerification {

	private static final int EXPECTED_ARGS = 2;

	/**
	 * Tests the arguments and converts the specified DataFile.
	 * @param args
	 */
	public static void main(String[] args) {
		testArgs(args);
		verifyDataFile(args);
	}
	
	/**
	 * Ensures that the arguments specified by the user are correct.
	 * @param args
	 */
	private static void testArgs(String[] args) {
		if (args.length != EXPECTED_ARGS) {
			System.err.println("Usage: java DataConversion <DataFile Path> <KeyFile Path>");
			System.exit(1);
		}
	}
	
	/**
	 * Converts the DB file by creating an AESDataConverter.
	 * @param args
	 */
	private static void verifyDataFile(String[] args) {
		File dataFile = new File(args[0]);
		File keyFile = new File(args[1]);
		
		if (dataFile.isFile() && dataFile.exists() &&
				keyFile.isFile() && keyFile.exists()) {
			AESDataVerifier verifier = new AESDataVerifier(dataFile, keyFile);
			verifier.verify();
		} else {
			System.out.println("Error: Cannot open files.");
		}
	}

}
