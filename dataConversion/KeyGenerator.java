package dataConversion;

import DataConversionModule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Random;

import symbols.Symbol;

public class KeyGenerator {
	
	private static int bitSize = 1024;
	private static long serialNumber;

	/**
	 * Method to generate the RSA keys
	 * 
	 * @param N
	 * @param databaseName
	 */
	private static void generateRSASignature ( int N, String databaseName, Path schemaFile )
	{
		// Write public and private key to file
		File rsaKeyFile = new File( schemaFile.getParent() + Symbol.SLASH_DELIMITER + databaseName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.RSA_KEY_FILE_EXTENSION );

		BigInteger p = BigInteger.probablePrime( N, random );
		BigInteger q = BigInteger.probablePrime( N, random );
		BigInteger phi = ( p.subtract( one ) ).multiply( q.subtract( one ) ); // (p-1)
		                                                                      // *
		                                                                      // (q-1)
		modulus = p.multiply( q ); // p*q
		setModulus( modulus );
		publicKey = new BigInteger( "65537" ); // common value in practice =
		                                       // 2^16 + 1
		setPublicKey( publicKey );
		privateKey = publicKey.modInverse( phi );
		setPrivateKey( privateKey );

		System.out.println( " Data file :: Private Key : " + privateKey );
		System.out.println( " Data file :: Modulus : " + modulus );

		if ( rsaKeyFile.exists() )
		{

			try
			{
				rsaKeyFile.delete();
				rsaKeyFile.createNewFile();

				// Write rsa keys to file
				Writer rsaKeyFileOutput = new BufferedWriter( new FileWriter( rsaKeyFile, true ) );
				rsaKeyFileOutput.write( "p:" );
				rsaKeyFileOutput.write( p.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.write( "q:" );
				rsaKeyFileOutput.write( q.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.write( "publickey:" );
				rsaKeyFileOutput.write( publicKey.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.write( "privatekey:" );
				rsaKeyFileOutput.write( privateKey.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.write( "modulus:" );
				rsaKeyFileOutput.write( modulus.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.close();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				rsaKeyFile.createNewFile();
				Writer rsaKeyFileOutput = new BufferedWriter( new FileWriter( rsaKeyFile, true ) );
				rsaKeyFileOutput.write( "p:" );
				rsaKeyFileOutput.write( p.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.write( "q:" );
				rsaKeyFileOutput.write( q.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.write( "publickey:" );
				rsaKeyFileOutput.write( publicKey.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.write( "privatekey:" );
				rsaKeyFileOutput.write( privateKey.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.write( "modulus:" );
				rsaKeyFileOutput.write( modulus.toString() );
				rsaKeyFileOutput.write( NEWLINE_DELIMITER );
				rsaKeyFileOutput.close();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method to generate the first valid serial Number
	 * 
	 * @param inputDataFile
	 * @param schemaFile
	 */
	public static void generateSerialNum ( String inputDataFile, Path schemaFile )
	{
		Random prng;
		Writer icrlFileOutput = null;
		try
		{
			prng = new Random();
			if ( inputDataFile.endsWith( ".sql" ) )
			{
				inputDataFile = inputDataFile.replace( ".sql", "" );
			}

			File icrlFile = new File( schemaFile.getParent() + SLASH_DELIMITER + inputDataFile + ICRL_FILE_EXTENSION );

			if ( icrlFile.exists() )
			{
				icrlFile.delete();
				icrlFile.createNewFile();
			}
			else
			{
				icrlFile.createNewFile();
			}

			int serialNumber = new Integer( prng.nextInt( Integer.MAX_VALUE ) );
			setSerialNumber( serialNumber );
			icrlFileOutput = new BufferedWriter( new FileWriter( icrlFile ) );

			if ( icrlFileOutput != null )
			{
				icrlFileOutput.write( "First Valid Serial Number: " );
				icrlFileOutput.write( Integer.toString( serialNumber ) );
			}
			icrlFileOutput.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * @return the privateKey
	 */
	public static BigInteger getPrivateKey ()
	{
		return privateKey;
	}

	/**
	 * @param privateKey
	 *            the privateKey to set
	 */
	public static void setPrivateKey ( BigInteger privateKey )
	{
		DataConversionModule.privateKey = privateKey;
	}

	/**
	 * @return the publicKey
	 */
	public static BigInteger getPublicKey ()
	{
		return publicKey;
	}

	/**
	 * @param publicKey
	 *            the publicKey to set
	 */
	public static void setPublicKey ( BigInteger publicKey )
	{
		DataConversionModule.publicKey = publicKey;
	}

	/**
	 * @return the modulus
	 */
	public static BigInteger getModulus ()
	{
		return modulus;
	}

	/**
	 * @param modulus
	 *            the modulus to set
	 */
	public static void setModulus ( BigInteger modulus )
	{
		DataConversionModule.modulus = modulus;
	}

	/**
	 * @return the serialNumber
	 */
	public static long getSerialNumber ()
	{
		return serialNumber;
	}

	/**
	 * @param serialNumber
	 *            the serialNumber to set
	 */
	public static void setSerialNumber ( long sNum )
	{
		serialNumber = sNum;
	}
	
}
