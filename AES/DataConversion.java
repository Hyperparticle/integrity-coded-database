package AES;

import java.io.File;

/**
 * Converts a DB data file (.unl) to an ICDB AES file.
 * 
 * @author Daniel Kondratyuk
 *
 */
public class DataConversion {
	
	private static final int EXPECTED_ARGS = 2;

	/**
	 * Tests the arguments and converts the specified DataFile.
	 * @param args
	 */
	public static void main(String[] args) {
		testArgs(args);
		convertDataFile(args);
	}
	
	/**
	 * Ensures that the arguments specified by the user are correct.
	 * @param args
	 */
	private static void testArgs(String[] args) {
		if (args.length != EXPECTED_ARGS) {
			System.err.println("Usage: java DataConversion <DataFile Path> <Database Name>");
			System.exit(1);
		}
	}
	
	/**
	 * Converts the DB file by creating an AESDataConverter.
	 * @param args
	 */
	private static void convertDataFile(String[] args) {
		File dataFile = new File(args[0]);
		String databaseName = args[1];
		
		if (dataFile.isFile() && dataFile.exists()) {
			AESDataConverter converter = new AESDataConverter(dataFile, databaseName);
			converter.convert();
		} else {
			System.out.println("Error: Cannot open Datafile.");
		}
	}
}
