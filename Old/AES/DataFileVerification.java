package AES;

import AES.helper.Symbol;
import AES.main.AESDataFileVerifier;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataFileVerification {

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
			System.err.println("Usage: java DataVerification <Folder Path>");
			System.exit(1);
		}
	}
	
	/**
	 * Converts the DB file by creating an AESDataConverter.
	 * @param args
	 */
	private static void verifyDataFile(String[] args) {
		Path dataPath = Paths.get(args[0]);
		File keyFile = getKeyFile(dataPath.toString());
		
		File[] icdbFiles = getICDBFiles(dataPath.toString());
		
		// Verify all ICDB data files
		for (File icdb : icdbFiles) {
			AESDataFileVerifier verifier = new AESDataFileVerifier(icdb, keyFile);
			verifier.verifyFile();
		}
	}
	
	private static File[] getICDBFiles(String fileLocation) {
		File dir = new File(fileLocation);
		File [] files = dir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(Symbol.ICDB_UNL_EXTENSION);
		    }
		});
		
		return files;
	}
	
	public static File getKeyFile(String fileLocation) {
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
