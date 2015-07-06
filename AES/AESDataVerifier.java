package AES;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import javax.crypto.SecretKey;

public class AESDataVerifier {
	
	private File icdbDataFile;
	private File icrlFile;
	private File keyFile;
	
//	private String fileLocation;
	
	private SecretKey key;
	private AESCipher cipher = new AESCipher();
	
	private long mismatches = 0;
	private long mismatchedLines = 0;
	
	/**
	 * Creates an AESDataConverter and gets filenames ready.
	 * @param dataFile the file path to convert
	 * @param databaseName the name of the database the data comes from
	 */
	public AESDataVerifier(File icdbDataFile, File keyFile) {
		this.icdbDataFile = icdbDataFile;
		this.keyFile = keyFile;
		
		key = getKeyFromFile();
		
//		fileLocation = icdbDataFile.getParent();
	}
	
	/**
	 * Verifies the data file.
	 */
	public void verify() {
		System.out.println("Verifying " + icdbDataFile.getName() + "...");
		
		try {
			FileInputStream fStream = new FileInputStream(icdbDataFile);
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader input = new BufferedReader(new InputStreamReader(in));
			
			
			long lineNum = 1;
			String strLine;
			while ((strLine = input.readLine()) != null) {
				int index = strLine.lastIndexOf(Symbol.FILE_DELIMITER);
				String[] data = strLine.substring(0, index).split("\\|");
				String icCode = strLine.substring(index+1);
				
				String[] decodedIC = cipher.decrypt(icCode, key).split("\\|");
				
				boolean mismatchedLine = false;
				for (int i = 0; i < data.length && i < decodedIC.length; i++) {
					if (!data[i].equals(decodedIC[i])) {
						if (!mismatchedLine)
							System.out.println("Mismatch at line " + lineNum);
						System.out.println("IC Code: \"" + decodedIC[i] + "\", Data: \"" + data[i] + "\".");
						
						mismatchedLine = true;
						mismatches++;
					}
				}
				
				if (data.length != decodedIC.length) {
					System.out.println("Mismatch at line " + lineNum + ", data sizes do not match.");
					mismatchedLine = true;
					mismatches++;
				}
				
				if (mismatchedLine)
					mismatchedLines++;
				
				lineNum++;
			}
			
			input.close();
			in.close();
			fStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		if (mismatches == 0) {
			System.out.println("All data is verified.");
		} else {
			System.out.println("Recieved " + mismatches + " mismatches across " + mismatchedLines + " lines in " + icdbDataFile.getName());
		}
	}
	
	private SecretKey getKeyFromFile() {
		try {
			Scanner scan = new Scanner(keyFile);
			
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				
				String k;
				if (line.contains("AES KEY: ")) {
					k = line.replace("AES KEY: ", "");
					scan.close();
					return cipher.stringToKey(k);
				}
			}
			
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		return null;
	}
	
}
