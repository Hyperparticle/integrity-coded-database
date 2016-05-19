/**
* @author ujwal-mac
*   <p>
*     A Generates the key and stores in the file for SHA algorithm
*   </p>
* Created 5/18/2016
*/
package cipher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Path;
import java.security.SecureRandom;

import helper.FileEndings;

public class SHAKeyGenerator extends BaseKeyGenerator {

	private File keyFile;

	/**
	 * Setup SHAKeyGenerator
	 * 
	 * @param dataPath
	 * @param databaseName
	 */
	public SHAKeyGenerator(Path dataPath, String databaseName) {
		super(dataPath, databaseName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Securely generate a 16byte Salt and keep it in a file.
	 */
	public String generateSalt() {

		keyFile = new File(dataPath.toString() + Symbol.SLASH_DELIMITER + databaseName
				+ FileEndings.SCHEMA_FILE_EXTENSION + FileEndings.SHA_KEY_FILE_EXTENSION);
		if (keyFile.exists())
			keyFile.delete();

		String hashsalt = null;
		try {
			keyFile.createNewFile();
			Writer keyFileOutput = new BufferedWriter(new FileWriter(keyFile, true));

			// generate salt
			SecureRandom random = new SecureRandom();
			byte[] salt = new byte[16];
			random.nextBytes(salt);
			hashsalt = salt.toString();
			keyFileOutput.write(Symbol.SHA_KEY_STRING);
			keyFileOutput.write(hashsalt);
			keyFileOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}

		return hashsalt;
	}

	/**
	 * @return Hash Salt that is stored in keyfile
	 */
	public String getKey() {
		String salt = null;
		keyFile = new File(dataPath.toString() + Symbol.SLASH_DELIMITER + databaseName
				+ FileEndings.SCHEMA_FILE_EXTENSION + FileEndings.SHA_KEY_FILE_EXTENSION);
		try {
			if (keyFile.exists()) {
				FileInputStream fStream = new FileInputStream(keyFile);
				DataInputStream in = new DataInputStream(fStream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine = "";

				while ((strLine = br.readLine()) != null) {
					strLine = strLine.trim();
					String[] tkns = strLine.split(":");

					if (tkns[0].equals(Symbol.SHA_KEY_STRING.substring(0, Symbol.SHA_KEY_STRING.length() - 2))) {
						salt = tkns[1];
					}

				}

				fStream.close();
				in.close();
				br.close();
			} else {
				generateSalt();
				getKey();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return salt;

	}

}
