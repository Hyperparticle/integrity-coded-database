package AES.main;

import AES.helper.AESCipher;
import AES.helper.Symbol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.crypto.SecretKey;

/**
 * Generates an AES key (output to file), encrypts each line in the DB DataFile, 
 * and outputs the result to a file.
 * 
 * @author Daniel Kondratyuk
 *
 */
public class AESDataConverter {
	
	private File dataFile;
	
	private String icdbFileName;
	
	private SecretKey key;
	private long serialNumber;
	
	private AESCipher cipher = new AESCipher();
	
	/**
	 * Creates an AESDataConverter and gets filenames ready.
	 * @param dataFile the file path to convert
	 * @param databaseName the name of the database the data comes from
	 * @param key a predefined key to encrypt with
	 */
	public AESDataConverter(File dataFile, SecretKey key, long serialNumber) {
		this.dataFile = dataFile;
		this.key = key;
		this.serialNumber = serialNumber;
		
		icdbFileName = dataFile.getPath().replace(Symbol.UNL_FILE_EXTENSION, Symbol.ICDB_UNL_EXTENSION);
	}
	
	/**
	 * Converts the DataFile by generating a signature for each tuple (line) in the file.
	 */
	public long convertDataFile() {
		System.out.println("Converting " + dataFile.getName() + "...");
		
		try {
			FileInputStream fStream = new FileInputStream(dataFile);
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader input = new BufferedReader(new InputStreamReader(in));
			
			// If the file exists, replace it
			File icdbFile = new File(icdbFileName);
			if (icdbFile.exists())
				icdbFile.delete();
			icdbFile.createNewFile();
			
			BufferedWriter output = new BufferedWriter(new FileWriter(icdbFile));
			
			// Loop through each line and generate an encrypted IC.
			// Output results to file.
			String strLine;
			while ((strLine = input.readLine()) != null) {
				String message = strLine + Symbol.FILE_DELIMITER + Long.toString(serialNumber);
				String encryptedMessage = cipher.encrypt(message, key);
				
				output.write(message);
				output.write(Symbol.FILE_DELIMITER);
				output.write(encryptedMessage);
				output.write(System.lineSeparator());
				
				serialNumber++;
			}
			
			input.close();
			in.close();
			fStream.close();
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		System.out.println("Finished converting " + dataFile.getName() + ".");
		
		return serialNumber;
	}
	
}
