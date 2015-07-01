package AES;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DataVerification {

	private static final int EXPECTED_ARGS = 1;

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
			System.err.println("Usage: java DataConversion <Folder Path>");
			System.exit(1);
		}
	}
	
	/**
	 * Converts the DB file by creating an AESDataConverter.
	 * @param args
	 */
	private static void verifyDataFile(String[] args) {
		Path dataPath = Paths.get(args[0]);
		File keyFile = getKeyFile(dataPath);
		
		ArrayList<File> list = fileList(dataPath);
		
		for (File unl : list) {
			AESDataVerifier verifier = new AESDataVerifier(unl, keyFile);
			verifier.verify();
		}
	}
	
	private static ArrayList<File> fileList(Path schemaFilePath) {
		ArrayList<File> unlFiles = new ArrayList<File>();
		try {
			Files.walk(schemaFilePath.getParent()).forEach(dataFilePath -> {
				if (Files.isRegularFile(dataFilePath)) {
					if (dataFilePath.toString().endsWith("_ICDB.unl")) {
						unlFiles.add(dataFilePath.toFile());
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return unlFiles;
	}
	
	private static File getKeyFile(Path schemaFilePath) {
		ArrayList<File> keyFiles = new ArrayList<File>();
		
		try {
			Files.walk(schemaFilePath.getParent()).forEach(dataFilePath -> {
				if (Files.isRegularFile(dataFilePath)) {
					if (dataFilePath.toString().endsWith("_aes.txt")) {
						keyFiles.add(dataFilePath.toFile());
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return keyFiles.get(0);
	}

}
