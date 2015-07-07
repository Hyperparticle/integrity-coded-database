package AES.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.security.SecureRandom;

import javax.crypto.SecretKey;

public class AESFileGenerator {
	
	private Path dataPath;
	private File icrlFile;
	private File keyFile;
	
	private String databaseName;
	
	private long serialNumber;
	
	public AESFileGenerator(Path dataPath, String databaseName) {
		this.dataPath = dataPath;
		this.databaseName = databaseName;
	}
	
	/**
	 * Securely generate a SecretKey and keep it in a file.
	 */
	public SecretKey generateKey()
	{
		AESCipher cipher = new AESCipher();
		SecretKey key = cipher.generateKey();
		
		keyFile = new File(dataPath.toString() + Symbol.SLASH_DELIMITER + databaseName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.AES_KEY_FILE_EXTENSION);
		if (keyFile.exists())
			keyFile.delete();
		
		try {
			keyFile.createNewFile();
			Writer keyFileOutput = new BufferedWriter(new FileWriter(keyFile, true));
			
			keyFileOutput.write(Symbol.AES_KEY_STRING);
			keyFileOutput.write(cipher.keyToString(key));
			keyFileOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		return key;
	}

	/**
	 * Method to generate the first valid serial Number.
	 */
	public long generateSerialNum() {
		try {
			icrlFile = new File(dataPath.toString() + Symbol.SLASH_DELIMITER + databaseName + Symbol.ICRL_FILE_EXTENSION);

			if (icrlFile.exists())
				icrlFile.delete();
			icrlFile.createNewFile();
			
			SecureRandom rand = new SecureRandom();
			serialNumber = new Integer(rand.nextInt(Integer.MAX_VALUE));
			
			Writer icrlFileOutput = new BufferedWriter(new FileWriter(icrlFile));
			icrlFileOutput.write("First Valid Serial Number: ");
			icrlFileOutput.write(Long.toString(serialNumber));
			icrlFileOutput.write(System.lineSeparator());
			icrlFileOutput.close();
			
			return serialNumber;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		return 0;
	}
	
	/**
	 * Saves the last serial number.
	 */
	public void saveSerialNum(long serial) {
		try {
			serialNumber = serial;
			
			File tempFile = new File(dataPath.toString() + "/tmp.txt");
			
			BufferedReader input = new BufferedReader(new FileReader(icrlFile));
			BufferedWriter output = new BufferedWriter(new FileWriter(tempFile));
			
			// Delete the any lines containing "Last Valid Serial Number"
			String currentLine;
			while ((currentLine = input.readLine()) != null) {
				String trimmedLine = currentLine.trim();
				
			    if(trimmedLine.contains("Last Valid Serial Number")) continue;
			    output.write(currentLine + System.lineSeparator());
			}
			
			input.close();
			output.close();
			icrlFile.delete();
			tempFile.renameTo(icrlFile);
			
			// Write the new lines to the file
			Writer icrlFileOutput = new BufferedWriter(new FileWriter(icrlFile, true));
			
			icrlFileOutput.write("Last Valid Serial Number: ");
			icrlFileOutput.write(Long.toString(serialNumber));
			icrlFileOutput.write(System.lineSeparator());
			
			icrlFileOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}
}
