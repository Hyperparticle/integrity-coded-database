/**
* @author ujwal-mac
*   <p>
*     A Generates the key and stores in the file for RSA algorithm
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
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.SecureRandom;

import helper.FileEndings;

public class RSAKeyGenerator extends BaseKeyGenerator {

	/**
	 * Setup RSAKeyGenerator
	 * 
	 * @param dataPath
	 * @param databaseName
	 */
	public RSAKeyGenerator(Path dataPath, String databaseName) {
		super(dataPath, databaseName);
		// TODO Auto-generated constructor stub
	}

	private static BigInteger privateKey;
	private static BigInteger publicKey;
	private static BigInteger modulus;
	public final static SecureRandom random = new SecureRandom();
	private static int bitSize = 1024;
	private final static BigInteger one = new BigInteger("1");

	private File keyFile;

	/**
	 * @return returns the private key to be used for RSA
	 */
	public static BigInteger getPrivateKey() {
		return privateKey;
	}

	/**
	 * @param privateKey
	 *            is the private key used for RSA
	 */
	public static void setPrivateKey(BigInteger privateKey) {
		RSAKeyGenerator.privateKey = privateKey;
	}

	/**
	 * @return returns the public key to be used for RSA
	 */
	public static BigInteger getPublicKey() {
		return publicKey;
	}

	/**
	 * @param privateKey
	 *            is the public key used for RSA
	 */
	public static void setPublicKey(BigInteger publicKey) {
		RSAKeyGenerator.publicKey = publicKey;
	}

	public static BigInteger getModulus() {
		return modulus;
	}

	public static void setModulus(BigInteger modulus) {
		RSAKeyGenerator.modulus = modulus;
	}

	/**
	 * This method generates public and private keys, modulus for the RSA
	 * encryption and decryption
	 */
	public void generateKey() {

		keyFile = new File(dataPath.toString() + Symbol.SLASH_DELIMITER + databaseName
				+ FileEndings.SCHEMA_FILE_EXTENSION + FileEndings.RSA_KEY_FILE_EXTENSION);
		if (keyFile.exists())
			keyFile.delete();

		try {
			// Generate public and private keys
			BigInteger p = BigInteger.probablePrime(bitSize, random);
			BigInteger q = BigInteger.probablePrime(bitSize, random);
			BigInteger phi = (p.subtract(one)).multiply(q.subtract(one)); // (p-1)
																			// *
																			// (q-1)
			modulus = p.multiply(q); // p*q
			setModulus(modulus);
			publicKey = new BigInteger("65537"); // common value in practice
													// =
													// 2^16 + 1
			setPublicKey(publicKey);
			privateKey = publicKey.modInverse(phi);
			setPrivateKey(privateKey);

			System.out.println(" Data file :: Private Key : " + privateKey);
			System.out.println(" Data file :: Modulus : " + modulus);

			keyFile.createNewFile();
			Writer keyFileOutput = new BufferedWriter(new FileWriter(keyFile, true));
			keyFileOutput.write(Symbol.RSA_KEY_STRING);
			keyFileOutput.write("p:");
			keyFileOutput.write(p.toString());
			keyFileOutput.write(Symbol.NEWLINE_DELIMITER);
			keyFileOutput.write("q:");
			keyFileOutput.write(q.toString());
			keyFileOutput.write(Symbol.NEWLINE_DELIMITER);
			keyFileOutput.write("publickey:");
			keyFileOutput.write(publicKey.toString());
			keyFileOutput.write(Symbol.NEWLINE_DELIMITER);
			keyFileOutput.write("privatekey:");
			keyFileOutput.write(privateKey.toString());
			keyFileOutput.write(Symbol.NEWLINE_DELIMITER);
			keyFileOutput.write("modulus:");
			keyFileOutput.write(modulus.toString());
			keyFileOutput.write(Symbol.NEWLINE_DELIMITER);
			keyFileOutput.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(2);
		}

	}

	/**
	 * Get the private key and modulus required for encryption
	 */
	public void getKeys() {
		keyFile = new File(dataPath.toString() + Symbol.SLASH_DELIMITER + databaseName
				+ FileEndings.SCHEMA_FILE_EXTENSION + FileEndings.RSA_KEY_FILE_EXTENSION);
		try {
			if (keyFile.exists()) {
				FileInputStream fStream = new FileInputStream(keyFile);
				DataInputStream in = new DataInputStream(fStream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine = "";

				while ((strLine = br.readLine()) != null) {
					strLine = strLine.trim();
					String[] tkns = strLine.split(":");

					if (tkns[0].equals("modulus")) {
						modulus = new BigInteger(tkns[1]);
					}
					if (tkns[0].equals("privatekey")) {
						privateKey = new BigInteger(tkns[1]);
					}

					if (tkns[0].equals("publicKey")) {
						publicKey = new BigInteger(tkns[1]);
					}
				}

				fStream.close();
				in.close();
				br.close();
			} else {
				generateKey();
				getKeys();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
