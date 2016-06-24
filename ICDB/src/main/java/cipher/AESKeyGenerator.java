/**
* @author ujwal-cipher.mac
*   <p>
*     A Generates the key and stores in the file for AES algorithm
*   </p>
* Created 5/18/2016
*/

package cipher;

public class AESKeyGenerator {
//	private File keyFile;
//
//	/**
//	 * Setup AESKeyGenerator
//	 *
//	 * @param dataPath
//	 * @param databaseName
//	 */
//	public AESKeyGenerator(Path dataPath, String databaseName) {
//		super(dataPath, databaseName);
//		// TODO Auto-generated constructor stub
//	}
//
//	/**
//	 * Securely generate a SecretKey and keep it in a file.
//	 */
//	public SecretKey generateKey() {
//		AESCipher cipher = new AESCipher();
//		SecretKey key = cipher.generateKey();
//
//		keyFile = new File(dataPath.toBase64() + Symbol.SLASH_DELIMITER + databaseName
//				+ FileEndings.SCHEMA_FILE_EXTENSION + FileEndings.AES_KEY_FILE_EXTENSION);
//		if (keyFile.exists())
//			keyFile.delete();
//
//		try {
//			keyFile.createNewFile();
//			Writer keyFileOutput = new BufferedWriter(new FileWriter(keyFile, true));
//
//			keyFileOutput.write(Symbol.AES_KEY_STRING);
//			keyFileOutput.write(cipher.keyToString(key));
//			keyFileOutput.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(2);
//		}
//
//		return key;
//	}
//
//	/**
//	 * @return Secret Key that is stored in keyfile
//	 */
//	public String getKey() {
//		String Key = null;
//		keyFile = new File(dataPath.toBase64() + Symbol.SLASH_DELIMITER + databaseName
//				+ FileEndings.SCHEMA_FILE_EXTENSION + FileEndings.AES_KEY_FILE_EXTENSION);
//		try {
//			if (keyFile.exists()) {
//				FileInputStream fStream = new FileInputStream(keyFile);
//				DataInputStream in = new DataInputStream(fStream);
//				BufferedReader br = new BufferedReader(new InputStreamReader(in));
//				String strLine = "";
//
//				while ((strLine = br.readLine()) != null) {
//					strLine = strLine.trim();
//					String[] tkns = strLine.split(":");
//
//					if (tkns[0].equals(Symbol.AES_KEY_STRING.substring(0, Symbol.AES_KEY_STRING.length() - 2))) {
//						Key = tkns[1].trim();
//					}
//
//				}
//
//				fStream.close();
//				in.close();
//				br.close();
//			} else {
//				generateKey();
//				getKey();
//			}
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return Key;
//
//	}

}
