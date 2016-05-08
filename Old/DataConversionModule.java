/************************************************************
 * 
 * @author Archana Nanjundarao
 * 
 * Description: This module converts a given data file to
 * ICDB specific data file by generating and  inserting 
 * integrity codes to each attribute.
 * 
 ************************************************************/

import AES.helper.Symbol;

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
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

public class DataConversionModule
{
	// Set this to false to use hashing
	private static boolean RSA = false;
	
	public static String databaseName = null;

	private static int bitSize = 1024;
	private static long serialNumber;

	public final static SecureRandom random = new SecureRandom();

	private final static BigInteger one = new BigInteger( "1" );
	private static BigInteger privateKey;
	private static BigInteger publicKey;
	private static BigInteger modulus;

	public static ArrayList<String> primaryKeyList = new ArrayList<String>();
	public static ArrayList<String> atrList = new ArrayList<String>();

	public static Map<String, String> primaryKeyListWithUnl = new HashMap<String, String>();
	public static Map<String, String> keyPositionMap = new HashMap<String, String>();
	public static Map<String, String> attributeMap = new HashMap<String, String>();


	/**
	 * @return the databaseName
	 */
	public static String getDatabaseName ()
	{
		return databaseName;
	}
	/**
	 * @param databaseName
	 *            the databaseName to set
	 */
	public static void setDatabaseName ( String databaseName )
	{
		DataConversionModule.databaseName = databaseName;
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

	public static void main ( String[] args )
	{
		execute(args);
	}

	public static void execute(String[] args)
	{
		String inputDataFile = null;
		String schemaFileName = null;

		if (args.length != 2)
		{
			Scanner scan = new Scanner( System.in );
			System.out.println( "Enter the schema file name: </folderpath/schemaFileName>" );
			schemaFileName = scan.nextLine();
			System.out.println( "Enter the data file to be converted </folderpath/dataFileName>" );
			inputDataFile = scan.next();
			scan.close();
		}
		else
		{
			schemaFileName = args[0];
			inputDataFile = args[1];
		}

		if ( inputDataFile.length() < 2 )
		{
			System.out.println( "Enter the schema file name: </folderpath/schemaFileName>" );
			System.out.println( "Enter the data file to be converted </folderpath/dataFileName>" );
			System.exit( 1 );
		}
		else
		{
			Path schemaFile = Paths.get( schemaFileName );
			String[] tmp = schemaFile.getFileName().toString().split( "-" );
			databaseName = tmp[0];

			File dataFile = new File( inputDataFile );

			if ( dataFile.isFile() && dataFile.exists() )
			{
				/* Generate ICRL serial number */
				generateSerialNum( databaseName, schemaFile );
				System.out.println( " Serial Number generated.." + getSerialNumber() );
				
				if (RSA) {
					/* Generate RSA keys */
					generateRSASignature( bitSize, databaseName, schemaFile );
					System.out.println( "RSA keys generated.. " );
				}

				// Find the Primary Key
				primaryKeyList = findPrimaryKey( schemaFileName, inputDataFile );
				findPrimaryKeyPosition( primaryKeyList, new File( schemaFileName ) );

				convertDataFile( databaseName, dataFile );
				System.out.println( " Final Serial Number :: " + getSerialNumber() );

				saveLastValidSerialNumber( dataFile.getParent() );
			}
			else
			{
				System.out.println( "DataFile doesn't exist" );
				System.exit( 1 );
			}

		}
	}

	/**
	 * Method to write the last valid serial number to file
	 * 
	 * @param folderPath
	 */
	private static void saveLastValidSerialNumber ( String folderPath )
	{
		FileInputStream fstream;
		File fPath = new File( folderPath );

		if ( folderPath.length() > 0 )
		{
			File[] files = fPath.listFiles();
			File icrlFile = null;
			StringBuffer sb = new StringBuffer();

			for ( File indFile : files )
			{
				if ( getDatabaseName() != null && getDatabaseName().length() > 0 )
				{
					if ( indFile.getAbsoluteFile().getName().startsWith( getDatabaseName() )
							&& indFile.getAbsoluteFile().getName().contains( Symbol.ICRL_FILE_EXTENSION ) )
					{
						icrlFile = new File( folderPath + Symbol.SLASH_DELIMITER + indFile.getAbsoluteFile().getName() );
						break;
					}
				}
			}

			try
			{
				if ( icrlFile != null && icrlFile.exists() )
				{
					fstream = new FileInputStream( icrlFile );

					/* Get the object of DataInputStream */
					DataInputStream in = new DataInputStream( fstream );
					BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
					String strLine;
					Writer icrlFileOutput = null;
					boolean hasLastValidSerialNum = Boolean.FALSE;

					/* Read File Line By Line */
					while ( ( strLine = br.readLine() ) != null )
					{
						strLine = strLine.trim();
						if ( strLine.startsWith( "First Valid Serial Number" ) )
						{
							sb.append( strLine );
							sb.append( "\n" );
							continue;
						}
						else if ( strLine.startsWith( "Current Valid Serial Number" ) )
						{
							hasLastValidSerialNum = Boolean.TRUE;
							String[] tkns = strLine.split( ":" );
							sb.append( tkns[0] + ":" );
						}

					}

					if ( hasLastValidSerialNum )
					{
						icrlFile.delete();
						icrlFile.createNewFile();
						icrlFileOutput = new BufferedWriter( new FileWriter( icrlFile ) );
						icrlFileOutput.write( sb.toString() );
						String lastValidSNum = Long.toString( getSerialNumber() );
						icrlFileOutput.write( lastValidSNum );
						icrlFileOutput.close();
					}

					else if ( !hasLastValidSerialNum )
					{
						String lastValidSNum = Long.toString( getSerialNumber() );
						icrlFileOutput = new BufferedWriter( new FileWriter( icrlFile, true ) );
						icrlFileOutput.write( "\n" );
						icrlFileOutput.write( "Current Valid Serial Number" + ":" + lastValidSNum );
						icrlFileOutput.close();
					}

					br.close();
					in.close();
					fstream.close();
				}

			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method to convert data file and generate signature for each attribute
	 * 
	 * @param databaseName
	 * @param dataFile
	 */
	private static void convertDataFile ( String databaseName, File dataFile )
	{
		// For each data file
		// 1. Read data file and using key position map find keys
		// 2. Read data file and using attribute list, find the signature
		String primaryKeys = "";
		BigInteger message, encrypt = null;
		Writer output = null;
		String fileLocation = dataFile.getParent();
		Path dFile = Paths.get( dataFile.toString() );
		String unlFile = dFile.getFileName().toString();
		unlFile = unlFile.replace( Symbol.UNL_FILE_EXTENSION, "" );
		
		String encr = null;

		try
		{
			// Write the 1st serial number to ICRL file
			File folderPath = new File( fileLocation );
			File[] files = folderPath.listFiles();
			File icrlFile = null;

			for ( File indFile : files )
			{
				if ( indFile.getAbsoluteFile().getName().startsWith( databaseName )
						&& indFile.getAbsoluteFile().getName().endsWith( Symbol.ICRL_FILE_EXTENSION ) )
				{
					icrlFile = new File( folderPath + Symbol.SLASH_DELIMITER + indFile.getAbsoluteFile().getName() );
					break;
				}
			}

			if ( icrlFile != null && icrlFile.exists() )
			{
				serialNumber = getSerialNumber();
			}

			FileInputStream fStream = new FileInputStream( dataFile );
			DataInputStream in = new DataInputStream( fStream );
			BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
			String strLine = "";

			File modifiedDataFile = new File( generateModifiedDataFileName( dataFile.toString() ) );
			if ( !modifiedDataFile.exists() )
			{
				modifiedDataFile.createNewFile();
			}
			else
			{
				modifiedDataFile.delete();
				modifiedDataFile.createNewFile();
			}

			output = new BufferedWriter( new FileWriter( modifiedDataFile ) );

			while ( ( strLine = br.readLine() ) != null )
			{
//				System.out.println( "Serial Number :: " + serialNumber );
				// Get the primary keys position
				primaryKeys = getPrimaryKeys( strLine.trim(), fileLocation, unlFile );

				/*
				 * Generate RSA signature for each line of data file and write
				 * the output to new .unl file
				 */

				String[] dataFileTokens; 
				if(strLine.contains("\\") || strLine.contains("|")) {
					dataFileTokens = strLine.split("\\|");
				} else {
					dataFileTokens = strLine.split(" ");
				}

				int pos;
				for ( int j = 0; j < dataFileTokens.length; j++ )
				{
					if ( dataFileTokens[j].length() > 0 )
					{
						// write the original data to datafile_ICDB.unl
						output.write( dataFileTokens[j] );
						output.write( "|" );

						// write the integrity coded data to
						// datafile_ICDB.unl
						pos = j;
						
						if (RSA) {
							message = generateRSASignature( Integer.toString( pos + 1 ), dataFileTokens[j], primaryKeys, unlFile,
									Long.toString( getSerialNumber() ), fileLocation );
							
							if ( message != null )
							{
								System.out.println( " Data file :: message :: " + message.toString() );
								encrypt = encrypt( message );
							}
						} else {
							encr = PasswordHash.hashDB(Integer.toString( pos + 1 ), dataFileTokens[j], primaryKeys, unlFile,
									Long.toString(getSerialNumber()), attributeMap );
						}
						
						if (RSA) {
							System.out.println( " Data file :: encrypt :: " + encrypt.toString( 16 ) );
							output.write( encrypt.toString( 16 ) + Symbol.SLASH_DELIMITER + Long.toString( getSerialNumber() ) );
							System.out.println( " Final data file value :: " + encrypt.toString( 16 ) + Symbol.SLASH_DELIMITER
									+ Long.toString( getSerialNumber() ) );
						} else {
//							System.out.println( " Data file :: encrypt :: " + encr );
							output.write( encr + Symbol.SLASH_DELIMITER + Long.toString( getSerialNumber() ) );
//							System.out.println( " Final data file value :: " + encr + Symbol.SLASH_DELIMITER
//									+ Long.toString( getSerialNumber() ) );
						}

						if (j != dataFileTokens.length-1)
							output.write( "|" );

						setSerialNumber( getIncrementedSerialNum() );
					}
				}
				output.write( System.getProperty( "line.separator" ) );
			}

			br.close();
			in.close();
			fStream.close();
			output.close();
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

	}

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
			rsaKeyFile.delete();
		}
		try
		{
			rsaKeyFile.createNewFile();
			Writer rsaKeyFileOutput = new BufferedWriter( new FileWriter( rsaKeyFile, true ) );
			rsaKeyFileOutput.write( "p:" );
			rsaKeyFileOutput.write( p.toString() );
			rsaKeyFileOutput.write( Symbol.NEWLINE_DELIMITER );
			rsaKeyFileOutput.write( "q:" );
			rsaKeyFileOutput.write( q.toString() );
			rsaKeyFileOutput.write( Symbol.NEWLINE_DELIMITER );
			rsaKeyFileOutput.write( "publickey:" );
			rsaKeyFileOutput.write( publicKey.toString() );
			rsaKeyFileOutput.write( Symbol.NEWLINE_DELIMITER );
			rsaKeyFileOutput.write( "privatekey:" );
			rsaKeyFileOutput.write( privateKey.toString() );
			rsaKeyFileOutput.write( Symbol.NEWLINE_DELIMITER );
			rsaKeyFileOutput.write( "modulus:" );
			rsaKeyFileOutput.write( modulus.toString() );
			rsaKeyFileOutput.write( Symbol.NEWLINE_DELIMITER );
			rsaKeyFileOutput.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Method to generate the first valid serial Number
	 * 
	 * @param inputDataFile
	 * @param schemaFile
	 */
	private static void generateSerialNum ( String inputDataFile, Path schemaFile )
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

			File icrlFile = new File( schemaFile.getParent() + Symbol.SLASH_DELIMITER + inputDataFile + Symbol.ICRL_FILE_EXTENSION );

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
	 * Get incremented serial number
	 * 
	 * @return
	 */
	private static long getIncrementedSerialNum ()
	{
		long incSNum = getSerialNumber();
		incSNum += 1;
		return incSNum;
	}

	/**
	 * This method selects the data file and adds an "_ICDB" as suffix and
	 * returns it as a new data file name.
	 * 
	 * @param dataFileName
	 * @return modifiedDataFileName
	 */
	public static String generateModifiedDataFileName ( String dataFileName )
	{
		StringBuilder modifiedDataFileName = new StringBuilder();
		StringTokenizer token = new StringTokenizer( dataFileName, "." );
		if ( token.hasMoreTokens() )
		{
			modifiedDataFileName.append( token.nextToken() );
			modifiedDataFileName.append( Symbol.ICDB_UNL_EXTENSION );
		}
		
		return modifiedDataFileName.toString();
	}

	/**
	 * Method to find primary key for each dataFile
	 * 
	 * @param strLine
	 * @param dataFile
	 * @return
	 */
	public static ArrayList<String> findPrimaryKey ( String schemaFile, String dataFile )
	{
		String key;
		File sFile = new File( schemaFile );
		Path dFile = Paths.get( dataFile );
		FileInputStream fStream;
		String unlFile = dFile.getFileName().toString();
		boolean dataFileMatched = false;

		if ( unlFile.endsWith( Symbol.UNL_FILE_EXTENSION ) )
		{
			unlFile = unlFile.replace( Symbol.UNL_FILE_EXTENSION, "" );
		}
		try
		{
			if ( sFile.exists() )
			{
				fStream = new FileInputStream( schemaFile );
				DataInputStream in = new DataInputStream( fStream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";

				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.toUpperCase().trim().replace("`", "");

					if ( strLine.trim().startsWith( "CREATE" ) )
					{
						String[] tmp = strLine.split( "\\s+" );

						if ( tmp[2].equalsIgnoreCase( unlFile ) )
						{
							dataFileMatched = true;
						}

					}

					if ( dataFileMatched )
					{
						if ( strLine.trim().contains( "PRIMARY KEY" ))
						{
							String tmp = strLine;
							String[] tokens = tmp.split( "\\(" );

							if ( !tokens[1].endsWith( "," ) )
							{
								key = tokens[1].substring( 0, tokens[1].length() - 1 );
							}
							else
							{
								key = tokens[1].substring( 0, tokens[1].length() - 2 );
							}

							if ( key.contains( "," ) )
							{
								key = key.replace( ",", Symbol.SLASH_DELIMITER );
								key = key.replace( "/ ", Symbol.SLASH_DELIMITER );
							}

							primaryKeyList.add( key );
							primaryKeyListWithUnl.put( unlFile.toUpperCase(), key );
							break;
						}
					}
				}
				br.close();
				in.close();
				fStream.close();
			}
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
		return primaryKeyList;
	}

	/**
	 * Method to get the position of the primary key from the schema file
	 * 
	 * @param primaryKeyList
	 * @param schemaFile
	 */
	public static void findPrimaryKeyPosition ( ArrayList<String> primaryKeyList, File schemaFile )
	{
		int position = 0;
		String unlFile = "";

		if ( schemaFile.exists() && primaryKeyList != null && !primaryKeyList.isEmpty() && primaryKeyListWithUnl != null
				&& !primaryKeyListWithUnl.isEmpty() )
		{
			try
			{
				FileInputStream fStream = new FileInputStream( schemaFile );
				DataInputStream in = new DataInputStream( fStream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";
				String pKey = "";

				// clear attribute list and then add
				atrList.clear();
				attributeMap.clear();

				while ( ( strLine = br.readLine() ) != null )
				{
					if ( !strLine.equals( "" ) && !strLine.startsWith("/*"))
					{
						strLine = strLine.trim().toUpperCase().replace("`", "");

						// Filter all the lines except data types
						if ( strLine.startsWith( "DROP SCHEMA IF EXISTS" ) || strLine.startsWith( "CREATE SCHEMA" )
								|| strLine.startsWith( "USE" ) || strLine.startsWith( "CREATE TABLE" )
								|| strLine.startsWith( "CONSTRAINT" ) || strLine.startsWith( "ALTER" ) || strLine.equals( "" )
								|| strLine.equals( ");" ) )
						{
							if ( strLine.startsWith( "CREATE TABLE" ))
							{
								String[] str = strLine.replaceAll( "(^\\s+|\\s+$)", "" ).split( "\\s+" );
								if ( str[str.length - 1].equals( "(" ) )
								{
									unlFile = str[str.length - 2];
								}
								else
								{
									unlFile = str[str.length - 1];
								}
								unlFile = unlFile.concat( Symbol.UNL_FILE_EXTENSION );

							}
							position = 0;
							continue;
						}
						else
						{
							// Include more data types and also handle case
							// sensitive
							if ( strLine.contains( "CHAR" ) || strLine.contains( "INT" ) || strLine.contains( "DECIMAL" ) || strLine.contains( "DATE" ) )
							{
								String[] lineTkns = strLine.trim().split( "\\s+" );
								pKey = lineTkns[0].trim();
								position += 1;
								unlFile = unlFile.replace( ".unl", "" );

								String primaryKey = primaryKeyListWithUnl.get( unlFile );

								if ( primaryKey != null )
								{
									if(pKey.startsWith("(")) {
										pKey = pKey.substring(1, pKey.length());
									}

									if ( primaryKey.contains( Symbol.SLASH_DELIMITER ) )
									{
										String[] keyTokens = primaryKey.split( Symbol.SLASH_DELIMITER );

										for ( int j = 0; j < keyTokens.length; j++ )
										{
											if ( pKey.equalsIgnoreCase( keyTokens[j] ) )
											{
												if ( !keyPositionMap.containsKey( unlFile ) )
												{
													keyPositionMap.put( unlFile, Integer.toString( position ) );
												}
												else
												{
													String value = keyPositionMap.get( unlFile );
													if ( value.equals( Integer.toString( position ) ) )
													{
														value = Integer.toString( position );
													}
													else
													{
														value = value + Symbol.SLASH_DELIMITER + Integer.toString( position );
													}
													keyPositionMap.put( unlFile, value );
												}
											}
										}
									}
									else
									{
										if ( pKey.equalsIgnoreCase( primaryKey ) )
										{
											if ( !keyPositionMap.containsKey( unlFile ) )
											{
												keyPositionMap.put( unlFile, Integer.toString( position ) );
											}
										}
									}
								}
								String[] attributeTokens = strLine.trim().split( "\\s+" );
								for ( int k = 0; k < attributeTokens.length;)
								{
									attributeMap.put( position + Symbol.SLASH_DELIMITER + unlFile, attributeTokens[k] );
									break;
								}
							}

						}
					}
				}

				br.close();
				in.close();
				fStream.close();
			}
			catch ( FileNotFoundException e )
			{
				e.printStackTrace();
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method to get the primary keys from the data file
	 * 
	 * @param line
	 * @param fileLocation
	 * @param dataFile
	 * @return
	 */
	private static String getPrimaryKeys ( String line, String fileLocation, String dataFile )
	{
		String primaryKeysPosition = "";
		String primaryKeys = "";
		// dataFile = dataFile.replace( ".unl", "" );
		dataFile = dataFile.toUpperCase();

		if ( keyPositionMap.containsKey( dataFile ) )
		{
			primaryKeysPosition = keyPositionMap.get( dataFile );
		}

		// Tokenize each line from data file
		String[] tokens;
		if(line.contains("\\") || line.contains("|")) {
			tokens = line.split("\\|");
		} else {
			tokens = line.split(" ");
		}

		for ( int i = 0; i < tokens.length; i++ )
		{
			if ( primaryKeysPosition.contains(Integer.toString(i) ) )
			{
				if ( primaryKeys.length() == 0 )
				{
					primaryKeys = tokens[i];
				}
				else
				{
					primaryKeys += Symbol.SLASH_DELIMITER + tokens[i];
				}
			}
		}
		return primaryKeys;
	}

	/**
	 * Method to generate RSA signature for each attribute
	 * 
	 * @param attrPosition
	 * 
	 * @param primaryKeys
	 * @param dataFile
	 * @param firstPass
	 * @param fileLocation
	 * @param dataFile2
	 * @return
	 */
	private static BigInteger generateRSASignature ( String attrPosition, String individualToken, String primaryKeys, String dataFile,
			String sNumber, String fileLocation )
	{
		BigInteger message = null;
		BigInteger pkBigInt, tknBigInt, attrBigInt;
		dataFile = dataFile.toUpperCase();

		// Loop through each attribute
		if ( attributeMap.containsKey( attrPosition + Symbol.SLASH_DELIMITER + dataFile ) )
		{

			// Attribute name
			String attrNameTokens = attributeMap.get( attrPosition + Symbol.SLASH_DELIMITER + dataFile );

			System.out.println( " ******************************************* " );
			System.out.println( " data file :: " + dataFile + " attr name :: " + attrNameTokens + " attr val :: " + individualToken + " pk name :: "
					+ primaryKeys + " serial Number : " + sNumber );

			// Check if its a single primary key or combination of keys
			if ( !primaryKeys.contains( Symbol.SLASH_DELIMITER ) )
			{
				// Check if primary key is a number or not
				if ( !isIntegerRegex( primaryKeys ) )
				{
					pkBigInt = convertToInt( primaryKeys );
				}
				else
				{
					pkBigInt = new BigInteger( primaryKeys );
				}
			}
			else
			{
				pkBigInt = findPrimaryKeyProduct( primaryKeys );
			}

			System.out.println( "Data File :: pkBigInt :: " + pkBigInt.toString() );

			// Check if data file token is a number or not
			if ( !isIntegerRegex( individualToken ) )
			{
				tknBigInt = convertToInt( individualToken );
			}
			else
			{
				tknBigInt = new BigInteger( individualToken );
			}

			System.out.println( "Data File :: tknBigInt :: " + tknBigInt.toString() );

			// Convert attribute name token to integer
			attrBigInt = convertToInt( attrNameTokens );

			System.out.println( "Data File :: attrBigInt :: " + attrBigInt.toString() );

			// Find the product of (s * attr name * attr value * primary
			// keys)

			message = pkBigInt.multiply( tknBigInt ).multiply( attrBigInt ).multiply( new BigInteger( sNumber ) );
			message = message.mod( getModulus() );
		}
		return message;

	}

	/**
	 * Method to get the product of primary keys
	 * 
	 * @param primaryKeys
	 * @return
	 */
	public static BigInteger findPrimaryKeyProduct ( String primaryKeys )
	{
		double pkInt = 0;
		BigInteger product = null;

		String[] primaryKeyTokens = primaryKeys.split( Symbol.SLASH_DELIMITER );

		for ( int i = 0; i < primaryKeyTokens.length; i++ )
		{
			if ( isIntegerRegex( primaryKeyTokens[i] ) )
			{
				if ( pkInt == 0 )
				{
					pkInt = Long.parseLong( primaryKeyTokens[i] );
					product = new BigInteger( primaryKeyTokens[i] );
				}
				else
				{
					product = product.multiply( new BigInteger( primaryKeyTokens[i] ) );
				}
			}
			else
			{
				if ( pkInt == 0 )
				{
					pkInt = convertToInt( primaryKeyTokens[i] ).intValue();
					product = convertToInt( primaryKeyTokens[i] );
				}
				else
				{
					product = product.multiply( convertToInt( primaryKeyTokens[i] ) );
				}
			}
		}

		return product;
	}

	/**
	 * Method to check if the attribute is having only numbers
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isIntegerRegex ( String str )
	{
		return str.matches( "^[0-9]+$" );
	}

	/**
	 * Method to convert an input to integer
	 * 
	 * @param input
	 * @return
	 */
	public static BigInteger convertToInt ( String input )
	{
		StringBuilder sBuilder = new StringBuilder();
		if ( !input.equals( "" ) )
		{
			for ( char c : input.toCharArray() )
			{
				sBuilder.append( ( int ) c );
			}
			return new BigInteger( sBuilder.toString() );
		}
		return null;
	}

	/**
	 * 
	 * @param message
	 * @return
	 */
	public static BigInteger encrypt ( BigInteger message )
	{
		return message.modPow( getPrivateKey(), getModulus() );
	}
	
}
