package AES;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.security.SecureRandom;

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
	private File icrlFile;
	private File keyFile;
	
	private String databaseName;
	private String fileLocation;
	private String tableName;
	private String icdbFileName;
	
	private SecretKey key;
	private long serialNumber;
	
	private AESCipher cipher = new AESCipher();
	
	/**
	 * Creates an AESDataConverter and gets filenames ready.
	 * @param dataFile the file path to convert
	 * @param databaseName the name of the database the data comes from
	 */
	public AESDataConverter(File dataFile, String databaseName) {
		this.dataFile = dataFile;
		this.databaseName = databaseName;
		
		fileLocation = dataFile.getParent();
		tableName = dataFile.getName().replace(Symbol.UNL_FILE_EXTENSION, "");
		icdbFileName = dataFile.getPath().replace(Symbol.UNL_FILE_EXTENSION, Symbol.ICDB_UNL_EXTENSION);
	}
	
	/**
	 * Generates a key and initiates conversion.
	 */
	public void convert() {
//		generateSerialNum();
		generateKey();
		convertDataFile();
	}
	
	/**
	 * Converts the DataFile by generating a signature for each tuple (line) in the file.
	 */
	private void convertDataFile() {
		System.out.println("Converting " + dataFile.getName() + "...");
		
		try {
			FileInputStream fStream = new FileInputStream(dataFile);
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader input = new BufferedReader(new InputStreamReader(in));
			
			File icdbFile = new File(icdbFileName);
			if (icdbFile.exists())
				icdbFile.delete();
			icdbFile.createNewFile();
			
			BufferedWriter output = new BufferedWriter(new FileWriter(icdbFile));
			
			String strLine;
			while ((strLine = input.readLine()) != null) {
//				System.out.println( "Serial Number :: " + serialNumber );
				
				String message = strLine; // + Symbol.SLASH_DELIMITER + serialNumber;
				String encryptedMessage = cipher.encrypt(message, key); // + Symbol.FILE_DELIMITER + serialNumber;
				
				output.write(strLine);
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
	}
	
	/**
	 * Securely generate a SecretKey and keep it in a file.
	 */
	private void generateKey()
	{
		key = cipher.generateKey();
		
		keyFile = new File(fileLocation + Symbol.SLASH_DELIMITER + databaseName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.AES_KEY_FILE_EXTENSION);
		if (keyFile.exists())
			keyFile.delete();
		
		try {
			keyFile.createNewFile();
			Writer keyFileOutput = new BufferedWriter(new FileWriter(keyFile, true));
			
			keyFileOutput.write("AES KEY: ");
			keyFileOutput.write(cipher.keyToString(key));
			keyFileOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}

	/**
	 * Method to generate the first valid serial Number. (Maybe Unnecessary?)
	 */
	private void generateSerialNum() {
		try {
			databaseName = databaseName.replace( ".sql", "" );
			File icrlFile = new File(fileLocation + Symbol.SLASH_DELIMITER + databaseName + Symbol.ICRL_FILE_EXTENSION);

			if (icrlFile.exists())
				icrlFile.delete();
			icrlFile.createNewFile();
			this.icrlFile = icrlFile;
			
			SecureRandom rand = new SecureRandom();
			serialNumber = new Integer(rand.nextInt(Integer.MAX_VALUE));
			
			Writer icrlFileOutput = new BufferedWriter(new FileWriter(icrlFile));
			icrlFileOutput.write("First Valid Serial Number: ");
			icrlFileOutput.write(Long.toString(serialNumber));
			icrlFileOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
	}
}
