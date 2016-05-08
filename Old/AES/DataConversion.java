package AES;

import AES.helper.AESFileGenerator;
import AES.main.AESDataConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.crypto.SecretKey;

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
		convertDataFiles(args);
	}
	
	/**
	 * Ensures that the arguments specified by the user are correct.
	 * @param args
	 */
	private static void testArgs(String[] args) {
		if (args.length != EXPECTED_ARGS) {
			System.err.println("Usage: java DataConversion <Folder Path> <Database Name>");
			System.exit(1);
		}
	}
	
	/**
	 * Converts the DB file by creating an AESDataConverter.
	 * @param args
	 */
	private static void convertDataFiles(String[] args) {
		Path dataPath = Paths.get(args[0]); 
		String databaseName = args[1];
		
		AESFileGenerator generator = new AESFileGenerator(dataPath, databaseName);
		SecretKey key = generator.generateKey();
		long serialNumber = generator.generateSerialNum();
		
		ArrayList<File> list = fileList(dataPath);
		
		for (File unl : list) {
			AESDataConverter converter = new AESDataConverter(unl, key, serialNumber);
			serialNumber = converter.convertDataFile();
			generator.saveSerialNum(serialNumber);
		}
	}
	
	private static ArrayList<File> fileList(Path schemaFilePath) {
		ArrayList<File> unlFiles = new ArrayList<File>();
		try {
			Files.walk(schemaFilePath.getParent()).forEach(dataFilePath -> {
				if (Files.isRegularFile(dataFilePath)) {
					if(dataFilePath.toString().endsWith(".unl") && !dataFilePath.toString().endsWith("_ICDB.unl")) {
						unlFiles.add(dataFilePath.toFile());
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return unlFiles;
	}
}
