/************************************************************
 * 
 * @author Archana Nanjundarao
 * Description: This module verifies the icdb sql query by
 * recalculating the signature for all the retrieved values
 * 
 ************************************************************/

import AES.helper.Symbol;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VerifyQueryModule
{
	private final static Logger log = Logger.getLogger( VerifyQueryModule.class.getName() );
	
	public static final Object STAR = "*";

	private static String databaseName;
	private static String folderPath;

	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://localhost:3333/";

	public static boolean selectQ = Boolean.FALSE;

	private static ArrayList<String> primaryKeyList = new ArrayList<String>();

	/**
	 * @return the folderPath
	 */
	public static String getFolderPath ()
	{
		return folderPath;
	}

	/**
	 * @return the selectQ
	 */
	public static boolean isSelectQ ()
	{
		return selectQ;
	}

	/**
	 * @param selectQ
	 *            the selectQ to set
	 */
	public static void setSelectQ ( boolean selectQ )
	{
		VerifyQueryModule.selectQ = selectQ;
	}

	/**
	 * @param folderPath
	 *            the folderPath to set
	 */
	public static void setFolderPath ( final String folderPath )
	{
		VerifyQueryModule.folderPath = folderPath;
	}

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
		VerifyQueryModule.databaseName = databaseName;
	}

	/**
	 * Main program for this module
	 * @param args
	 */
	public static void main ( String[] args )
	{
		String qFile = null;

		Scanner scan = new Scanner( System.in );
		
		if (args.length != 2)
		{
			System.out.println( "Enter the folderPath where schema file exists" );
			folderPath = scan.nextLine();
		}
		else
		{
			folderPath = args[0];
		}
		

		if ( folderPath.length() > 0 )
		{
			setFolderPath( folderPath );
			File fPath = new File( folderPath );
			if ( fPath.exists() )
			{
				if (args.length != 2)
				{
					System.out.println( "Enter the sql query file to be converted" );
					qFile = scan.nextLine();
				}
				else
				{
					qFile = args[1];
				}
				scan.close();

				if ( qFile.length() > 0 )
				{
					File queryFile = new File( qFile );
					if ( queryFile.exists() && queryFile.isFile() )
					{
						boolean verifiedSign = verifyICDBQuery( folderPath, queryFile );

						if ( verifiedSign )
						{
							System.out.println( "ICDB signature verified" );
						}
						else
						{
							System.out.println( "ICDB signature not verified" );
						}
					}
					else
					{
						System.out.println( "Query file doesnt exist" );
						System.exit( 1 );
					}
				}
			}
			else
			{
				System.out.println( "Folder Path doesnt exist" );
				System.exit( 1 );
			}
		}

	}

	/**
	 * 
	 * @param folderPath
	 * @param queryFile
	 * @return
	 */
	private static boolean verifyICDBQuery ( String folderPath, File queryFile )
	{
		boolean verified = false;
		boolean isVerify = false;
		
		StringBuffer convertedQuery = readQueryFile( queryFile );
		Connection connect = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		String queryString = null;
		
		ArrayList<String> tableNames = new ArrayList<String>();
		ArrayList<String> attrNames = new ArrayList<String>();
		ArrayList<String> attrValues = new ArrayList<String>();
		ArrayList<String> attr_SVC_Values = new ArrayList<String>();
		ArrayList<String> primaryKeyValue = new ArrayList<String>();
		ArrayList<String> attrSerialNumValues = new ArrayList<String>();
		ArrayList<String> reCalculatedValues = new ArrayList<String>();
		Map<String, String> primaryKeys = new HashMap<String, String>();

		primaryKeyList = populatePrimaryKeyList( folderPath, getDatabaseName() );

		String query = convertedQuery.toString();
		String[] queryTkns = query.split( ";" );

		for ( String qTkn : queryTkns )
		{
			qTkn = qTkn.trim();

			if ( qTkn.length() > 0 )
			{
				if ( qTkn.startsWith( "SELECT" ) )
				{
					selectQ = true;
					isVerify = true;
					queryString = qTkn + ";";
				}
			}
		}

		try
		{
			// This will load the MySQL driver, each DB has its own driver
			Class.forName( JDBC_DRIVER );

			// Setup the connection with the DB

			connect = DriverManager.getConnection( DB_URL + getDatabaseName() + "?allowMultiQueries=true" + "&user=root&password=spidermonkey" );

			if ( connect != null )
			{
				System.out.println( "Connected to database" );

				statement = connect.prepareStatement( queryString );

				if ( selectQ )
				{
					result = statement.executeQuery();
				}

				if ( isVerify && isSelectQ() )
				{
					while ( result.next() )
					{
						String[] attributes = queryString.split( Symbol.NEWLINE_DELIMITER );

						for ( String lineAttr : attributes )
						{
							if ( lineAttr.startsWith( "SELECT" ) )
							{
								String[] indAttr = lineAttr.trim().split( Symbol.COMMA_DELIMITER_WITHOUT_SPACE );
								for ( String selectAttr : indAttr )
								{
									boolean isKey = false;
									selectAttr = selectAttr.trim();

									if ( selectAttr.startsWith( "SELECT" ) )
									{
										String[] tmpAttr = selectAttr.split( "\\s+" );
										selectAttr = tmpAttr[1];
									}

									if ( !selectAttr.endsWith( Symbol.SVC_SUFFIX ) )
									{
										isKey = isPrimaryKey( selectAttr );
										if ( !isKey )
										{
											attrNames.add( selectAttr );
											attrValues.add( result.getString( selectAttr ) );
										}
										else if ( isKey )
										{
											primaryKeyValue.add( selectAttr + Symbol.SLASH_DELIMITER + result.getString( selectAttr ) );
										}
									}
									else
									{
										String tmp = result.getString( selectAttr );

										// Separate SVC value
										// and SNUM
										if ( tmp.contains( Symbol.SLASH_DELIMITER ) )
										{
											String[] tkns = tmp.split( Symbol.SLASH_DELIMITER );

											attr_SVC_Values.add( tkns[0] );
											attrSerialNumValues.add( tkns[1] );
										}
									}
								}
							}

							if ( lineAttr.startsWith( "FROM" ) )
							{
								String[] indAttr = lineAttr.trim().split( "\\s+" );

								for ( String fromAttr : indAttr )
								{
									fromAttr = fromAttr.trim();

									if ( fromAttr.equals( "FROM" ) || fromAttr.equals( "JOIN" ) )
									{
										continue;
									}
									else
									{
										if ( fromAttr.endsWith( Symbol.COMMA_DELIMITER_WITHOUT_SPACE ) )
										{
											fromAttr = fromAttr.replace( ",", "" );
										}

										if ( !tableNames.contains( fromAttr ) )
										{
											tableNames.add( fromAttr );
										}
									}
								}

							}
						}
					}

					primaryKeys = populateKeyValue( attrNames, primaryKeyValue, getDatabaseName(), getFolderPath() );

					reCalculatedValues = verifyQuery( attrNames, attrValues, primaryKeys, attrSerialNumValues, getDatabaseName(), getFolderPath() );

					if ( reCalculatedValues != null && !reCalculatedValues.isEmpty() && attr_SVC_Values != null && !attr_SVC_Values.isEmpty() )
					{
						for ( int i = 0; i < reCalculatedValues.size(); i++ )
						{
							for ( int j = 0; j < attr_SVC_Values.size(); j++ )
							{
								if ( reCalculatedValues.get( i ).equals( attr_SVC_Values.get( j ) ) )
								{
									verified = true;
									break;
								}
							}
							if ( !verified )
							{
								break;
							}
						}
					}
				}
			}
		}
		catch ( SQLException e )
		{
			e.printStackTrace();
		}
		catch ( ClassNotFoundException e )
		{
			e.printStackTrace();
		}
		finally
		{
			if ( statement != null )
			{
				try
				{
					statement.close();
				}
				catch ( SQLException e )
				{
					e.printStackTrace();
				}
			}

			if ( connect != null )
			{
				try
				{
					connect.close();
				}
				catch ( SQLException e )
				{
					e.printStackTrace();
				}
			}
		}

		return verified;
	}

	/**
	 * Method populatePrimaryKeyList
	 * 
	 * @param folderPath
	 * @param databaseName
	 * @return
	 */
	private static ArrayList<String> populatePrimaryKeyList ( String folderPath, String databaseName )
	{
		File primaryKeyFile = new File( folderPath + Symbol.SLASH_DELIMITER + databaseName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.PRIMARY_KEY_FILE_EXTENSION );
		ArrayList<String> primaryKeyList = new ArrayList<String>();
		try
		{
			if ( primaryKeyFile.exists() )
			{
				FileInputStream fStream = new FileInputStream( primaryKeyFile );
				DataInputStream in = new DataInputStream( fStream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";

				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim();
					String[] tkns = strLine.split( ":" );
					if ( tkns[2].contains( Symbol.SLASH_DELIMITER ) )
					{
						String[] tmpTkns = tkns[2].split( Symbol.SLASH_DELIMITER );
						for ( String key : tmpTkns )
						{
							if ( !primaryKeyList.contains( key ) )
							{
								primaryKeyList.add( key );
							}
						}
					}
					else
					{
						if ( !primaryKeyList.contains( tkns[2] ) )
						{
							primaryKeyList.add( tkns[2] );
						}
					}
				}

				fStream.close();
				in.close();
				br.close();
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
	 * Method isPrimaryKey
	 * 
	 * @param selectAttr
	 * @return boolean
	 */
	public static boolean isPrimaryKey ( String selectAttr )
	{
		boolean isPrimaryKey = false;
		for ( String key : primaryKeyList )
		{
			if ( key.equalsIgnoreCase( selectAttr ) )
			{
				isPrimaryKey = true;
				break;
			}
		}
		return isPrimaryKey;
	}

	/**
	 * Method populateKeyValue
	 * 
	 * @param attrNames
	 * @param primaryKeyValue
	 * @param databaseName
	 * @param folderPath
	 * @return
	 */
	private static Map<String, String> populateKeyValue ( ArrayList<String> attrNames, ArrayList<String> primaryKeyValue, String databaseName,
	        String folderPath )
	{
		Map<String, String> pKeys = new HashMap<String, String>();
		File pKFile = null;

		if ( attrNames != null && attrNames.size() > 0 && primaryKeyValue != null && primaryKeyValue.size() > 0 )
		{
			FileInputStream fstream;

			if ( folderPath != null )
			{
				File fPath = new File( folderPath );

				if ( fPath != null )
				{
					pKFile = new File( fPath + Symbol.SLASH_DELIMITER + databaseName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.PRIMARY_KEY_FILE_EXTENSION );

					try
					{
						if ( pKFile.exists() )
						{
							for ( int i = 0; i < attrNames.size(); i++ )
							{
								String[] pTkns = null;
								String strLine = "";

								/* Get the object of DataInputStream */
								fstream = new FileInputStream( pKFile );
								DataInputStream in = new DataInputStream( fstream );
								BufferedReader br = new BufferedReader( new InputStreamReader( in ) );

								/* Read File Line By Line */
								while ( ( strLine = br.readLine() ) != null )
								{
									boolean singleKey = false;
									boolean multipleKey = false;

									String[] tkns = strLine.trim().split( ":" );
									String[] tmpTkns = tkns[1].split( Symbol.COMMA_DELIMITER_WITHOUT_SPACE );

									for ( int j = 0; j < tmpTkns.length; j++ )
									{
										if ( attrNames.get( i ).equalsIgnoreCase( tmpTkns[j] ) )
										{
											for ( int j2 = 0; j2 < primaryKeyValue.size(); j2++ )
											{
												pTkns = primaryKeyValue.get( j2 ).split( Symbol.SLASH_DELIMITER );
												if ( !tkns[2].contains( Symbol.SLASH_DELIMITER ) && tkns[2].equalsIgnoreCase( pTkns[0] ) )
												{
													singleKey = true;
													multipleKey = false;
													break;
												}
												else if ( tkns[2].contains( pTkns[0] ) )
												{
													singleKey = false;
													multipleKey = true;
													break;
												}

											}
											break;
										}
									}

									if ( singleKey && !multipleKey )
									{
										pKeys.put( attrNames.get( i ), pTkns[1] );
										break;
									}

									if ( multipleKey && !singleKey )
									{
										String pKey = "";
										for ( String key : primaryKeyValue )
										{
											String[] tmp = key.split( Symbol.SLASH_DELIMITER );
											if ( pKey.length() == 0 )
											{
												pKey = tmp[1];
											}
											else
											{
												pKey += Symbol.SLASH_DELIMITER + tmp[1];
											}
										}
										pKeys.put( attrNames.get( i ), pKey );
									}
								}
								br.close();
								in.close();
								fstream.close();
							}

						}
					}

					catch ( IOException e )
					{
						e.printStackTrace();
					}
				}
			}
		}
		return pKeys;
	}

	/**
	 * Method verifyQuery
	 * @param attrNames
	 * @param attrValues
	 * @param primaryKeysMap
	 * @param attrSerialNumValues
	 * @param dbName
	 * @param folderPath
	 * @return
	 */
	public static ArrayList<String> verifyQuery ( ArrayList<String> attrNames, ArrayList<String> attrValues, Map<String, String> primaryKeysMap, ArrayList<String> attrSerialNumValues, String dbName, String folderPath )
	{
		String rsaKeyFile = dbName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.RSA_KEY_FILE_EXTENSION;
		BigInteger message, encrypt = null;
		BigInteger privateKey = findRSAPrivateKey( folderPath, rsaKeyFile );
		BigInteger modulus = findRSAModulus( folderPath, rsaKeyFile );
		ArrayList<String> reCalculatedValues = new ArrayList<String>();
		String primaryKey = "";

		//System.out.println( " Private Key : " + privateKey + " \n modulus : " + modulus );

		if ( attrNames != null && attrNames.size() > 0 && attrValues != null && attrValues.size() > 0 && attrSerialNumValues != null
		        && attrSerialNumValues.size() > 0 )
		{
			for ( int i = 0; i < attrNames.size(); )
			{
				for ( int j = 0; j < attrValues.size(); )
				{
					for ( int k = 0; k < attrSerialNumValues.size(); k++, i++, j++ )
					{
						if ( primaryKeysMap != null && primaryKeysMap.size() > 0 )
						{
							if ( primaryKeysMap.containsKey( attrNames.get( i ) ) )
							{
								primaryKey = primaryKeysMap.get( attrNames.get( i ) );
							}
						}
						message = generateRSASignature( attrNames.get( i ), primaryKey, attrValues.get( j ), attrSerialNumValues.get( k ), modulus );
						if ( message != null )
						{
							System.out.println( " verify query :: message :: " + message.toString() );
							encrypt = encrypt( message, privateKey, modulus );
							System.out.println( " verify query :: encrypt : " + encrypt.toString() );
							System.out.println( " verify query :: Encrypted message in hex:: " + encrypt.toString( 16 ) );
							reCalculatedValues.add( encrypt.toString( 16 ) );
						}
					}
				}
			}
		}
		return reCalculatedValues;

	}

	/**
	 * Method readQueryFile
	 * 
	 * @param queryFile
	 * @return
	 */
	private static StringBuffer readQueryFile ( File queryFile )
	{
		StringBuffer sb = new StringBuffer();
		FileInputStream fstream;

		try
		{
			fstream = new FileInputStream( queryFile );
			/* Get the object of DataInputStream */
			DataInputStream in = new DataInputStream( fstream );
			BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
			String strLine;

			/* Read File Line By Line */
			while ( ( strLine = br.readLine() ) != null )
			{
				if ( strLine.toUpperCase().startsWith( "USE" ) )
				{
					getDatabaseNameFromQuery( strLine );
				}
				else if ( strLine.equals( "" ) )
				{
					sb.append( Symbol.NEWLINE_DELIMITER );
				}
				else if ( strLine.toUpperCase().startsWith( "FROM" ) || strLine.toUpperCase().startsWith( "WHERE" ) )
				{
					sb.append( Symbol.NEWLINE_DELIMITER );
				}
				sb.append( strLine );

			}
			/* Close the input and output stream */
			br.close();
			in.close();
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return sb;
	}

	/**
	 * Method getDatabaseNameFromQuery
	 * 
	 * @param line
	 * @return
	 */
	private static String getDatabaseNameFromQuery ( String line )
	{
		String databaseName = null;
		String[] lineTkns = line.split( "\\s+" );
		if ( lineTkns[1].contains( "`" ) )
		{
			lineTkns[1] = lineTkns[1].replace( "`", "" );
			databaseName = lineTkns[1].trim().substring( 0, lineTkns[1].length() - 1 );
			setDatabaseName( databaseName.toLowerCase() );
		}
		else
		{
			databaseName = lineTkns[1].trim().substring( 0, lineTkns[1].length() - 1 );
			setDatabaseName( databaseName.toLowerCase() );
		}
		return databaseName;
	}
	
	/**
	 * Method to find RSA private key from the file
	 * 
	 * @param folderPath
	 * @param rsaKeyFile
	 * @return
	 */
	public static BigInteger findRSAPrivateKey ( final String folderPath, final String rsaKeyFile )
	{
		BigInteger privateKey = null;
		File rsaFile = new File( folderPath + Symbol.SLASH_DELIMITER + rsaKeyFile );

		try
		{
			if ( rsaFile.exists() )
			{
				FileInputStream fStream = new FileInputStream( rsaFile );
				DataInputStream in = new DataInputStream( fStream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";

				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim();
					String[] tkns = strLine.split( ":" );

					if ( tkns[0].equals( "privatekey" ) )
					{
						privateKey = new BigInteger( tkns[1] );
					}
				}

				fStream.close();
				in.close();
				br.close();
			}
		}
		catch ( FileNotFoundException e )
		{
			log.log( Level.INFO, "FileNotFoundException in findRSAPrivateKey( ) " );
		}
		catch ( IOException e )
		{
			log.log( Level.INFO, "IOException in findRSAPrivateKey( ) " );
		}
		return privateKey;
	}
	
	/**
	 * Method to find the RSA modulus from the file
	 * 
	 * @param folderPath
	 * @param rsaKeyFile
	 * @return
	 */
	public static BigInteger findRSAModulus ( final String folderPath, final String rsaKeyFile )
	{
		// Read p and q key from rsaKeyFile
		BigInteger modulus = null;
		File rsaFile = new File( folderPath + Symbol.SLASH_DELIMITER + rsaKeyFile );

		try
		{
			if ( rsaFile.exists() )
			{
				FileInputStream fStream = new FileInputStream( rsaFile );
				DataInputStream in = new DataInputStream( fStream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";

				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim();
					String[] tkns = strLine.split( ":" );

					if ( tkns[0].equals( "modulus" ) )
					{
						modulus = new BigInteger( tkns[1] );
					}
				}

				fStream.close();
				in.close();
				br.close();
			}
		}
		catch ( FileNotFoundException e )
		{
			log.log( Level.INFO, "FileNotFoundException in findRSAModulus( ) " );
		}
		catch ( IOException e )
		{
			log.log( Level.INFO, "IOException in findRSAModulus( ) " );
		}
		return modulus;
	}
	
	/**
	 * 
	 * @param individualAttr
	 * @param primaryKey
	 * @param individualAttrVal
	 * @param serialNum
	 * @param modulus
	 * @return
	 */
	private static BigInteger generateRSASignature ( String individualAttr, String primaryKey, String individualAttrVal, String serialNum,
	        BigInteger modulus )
	{
		BigInteger message = null;
		BigInteger pkBigInt, attrValBigInt, attrBigInt;

		System.out.println( " ******************************************* " );
		System.out.println( " verify query :: attr name :: " + individualAttr );
		System.out.println( " verify query :: pk name :: " + primaryKey );

		// Check if its a single primary key or combination of keys
		if ( !primaryKey.contains( Symbol.SLASH_DELIMITER ) )
		{
			// Check if primary key is a number or not
			if ( !isIntegerRegex( primaryKey ) )
			{
				pkBigInt = convertToInt( primaryKey );
			}
			else
			{
				pkBigInt = new BigInteger( primaryKey );
			}
		}
		else
		{
			pkBigInt = findPrimaryKeyProduct( primaryKey );
		}
		
		System.out.println( "verify query :: pkBigInt :: " + pkBigInt.toString() );

		// Convert attribute name token to integer
		attrBigInt = convertToInt( individualAttr );

		System.out.println( "verify query :: attrBigInt :: " + attrBigInt.toString() );

		// Check if data file token is a number or not
		if ( !isIntegerRegex( individualAttrVal ) )
		{
			attrValBigInt = convertToInt( individualAttrVal );
		}
		else
		{
			attrValBigInt = new BigInteger( individualAttrVal );
		}

		System.out.println( "verify query :: tknBigInt :: " + attrValBigInt.toString() );

		System.out.println( " verify query :: serial Number : " + serialNum );

		message = pkBigInt.multiply( attrValBigInt ).multiply( attrBigInt ).multiply( new BigInteger( serialNum ) );
		message = message.mod( modulus );

		return message;

	}
	
	/**
	 * Method to convert an input to integer
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
	 * Method to check if the attribute is having
	 * only numbers
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isIntegerRegex ( String str )
	{
		return str.matches( "^[0-9]+$" );
	}
	
	/**
	 * 
	 * @param message
	 * @param privateKey
	 * @param modulus
	 * @return
	 */
	public static BigInteger encrypt ( BigInteger message, BigInteger privateKey, BigInteger modulus )
	{
		return message.modPow( privateKey, modulus );
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

}
