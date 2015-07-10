package AES.main;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import javax.crypto.SecretKey;

import AES.DataFileVerification;
import AES.helper.AESCipher;
import AES.helper.Symbol;

public class AESDataFileVerifier {
	
	private File icdbDataFile;
	private File icrlFile;
	private File keyFile;
	
	private String fileLocation;
	
	private SecretKey key;
	private AESCipher cipher = new AESCipher();
	
	private long mismatches = 0;
	private long mismatchedLines = 0;
	
	private long maxSerial;
	private long minSerial;
	private ArrayList<Long> revokedCodes = new ArrayList<Long>();
	
	/**
	 * Creates an AESDataConverter and gets filenames ready.
	 * @param dataFile the file path to convert
	 */
	public AESDataFileVerifier(File icdbDataFile, File keyFile) {
		this.icdbDataFile = icdbDataFile;
		this.keyFile = keyFile;
		
		fileLocation = icdbDataFile.getParent();
		
		key = getKeyFromFile();
		
		icrlFile = getICRLFile();
		getICRLBounds();
	}
	
	/**
	 * Creates an AESDataConverter and gets filenames ready.
	 */
	public AESDataFileVerifier(String path) {
		this.keyFile = DataFileVerification.getKeyFile(path);
		
		fileLocation = keyFile.getParent();
		key = getKeyFromFile();
		
		icrlFile = getICRLFile();
		getICRLBounds();
	}
	
	/**
	 * Verifies the data file.
	 */
	public void verifyFile() {
		if (icdbDataFile == null) {
			System.out.println("Error: datafile not set.");
		}
		
		System.out.println("Verifying " + icdbDataFile.getName() + "...");
		
		try {
			FileInputStream fStream = new FileInputStream(icdbDataFile);
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader input = new BufferedReader(new InputStreamReader(in));
			
			long lineNum = 1;
			String strLine;
			
			while ((strLine = input.readLine()) != null) {
				// Split the line by the data and the IC
				int index = strLine.lastIndexOf(Symbol.FILE_DELIMITER);
				String[] data = strLine.substring(0, index).split("\\|");
				String icCode = strLine.substring(index+1);
				
				verify(data, icCode, lineNum);
				
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
	
	public void verify(String[] data, String icCode, long lineNum) {
		// Decode the IC
		String[] decodedIC = cipher.decrypt(icCode, key).split("\\|");
		
		// Verify by comparing each entry in the data with the IC
		boolean mismatchedLine = false;
		for (int i = 0; i < data.length && i < decodedIC.length; i++) {
			if (!data[i].equals(decodedIC[i])) {
				if (!mismatchedLine) {
					System.out.println("Mismatch at line " + lineNum);
					mismatchedLine = true;
				}
					
				System.out.println("IC Code: \"" + decodedIC[i] + "\", Data: \"" + data[i] + "\".");
				
				mismatches++;
			}
		}
		
		// Make sure all data is verified!
		if (data.length != decodedIC.length) {
			System.out.println("Mismatch at line " + lineNum + ", data sizes do not match.");
			mismatchedLine = true;
			mismatches++;
		}
		
		// Verify that the serial number is valid
		try {
			long serial = Long.parseLong(data[data.length-1]);
			if (serial < minSerial || serial > maxSerial || revokedCodes.contains(serial)) {
				System.out.println("Mismatch at line " + lineNum + ", serial no. is invalid");
				mismatchedLine = true;
				mismatches++;
			}
		} catch (NumberFormatException e) {
			System.out.println("Mismatch at line " + lineNum + ", serial no. is not formatted correctly or missing");
			mismatchedLine = true;
			mismatches++;
		}
		
		
		if (mismatchedLine)
			mismatchedLines++;
	}
	
	private SecretKey getKeyFromFile() {
		try {
			Scanner scan = new Scanner(keyFile);
			
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				
				if (line.contains(Symbol.AES_KEY_STRING)) {
					String k = line.replace(Symbol.AES_KEY_STRING, "");
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
	
	private File getICRLFile() {
		File dir = new File(fileLocation);
		File [] files = dir.listFiles(new FilenameFilter() {
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(Symbol.ICRL_FILE_EXTENSION);
		    }
		});
		
		return files[0];
	}
	
	private void getICRLBounds() {
		try {
			FileInputStream fStream = new FileInputStream(icrlFile);
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader input = new BufferedReader(new InputStreamReader(in));
			
			String strLine;
			while ((strLine = input.readLine()) != null) {
				if (strLine.startsWith("First Valid Serial Number: ")) {
					strLine = strLine.replace("First Valid Serial Number: ", "");
					minSerial = Long.parseLong(strLine);
				} else if (strLine.startsWith("Last Valid Serial Number: ")) {
					strLine = strLine.replace("Last Valid Serial Number: ", "");
					maxSerial = Long.parseLong(strLine);
				} else if (strLine.startsWith("Revoked: ")) {
					strLine = strLine.replace("Revoked: ", "");
					revokedCodes.add(Long.parseLong(strLine));
				}
			}
			
			input.close();
			in.close();
			fStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
	}
}
