

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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegenerateSignatureModule
{

	private final static Logger logger = Logger.getLogger( RegenerateSignatureModule.class.getName() );

	// JDBC driver name and database URL
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://localhost:3333/";

	public static String SPACE_DELIMITER = " ";

	public static String NEWLINE_DELIMITER = "\n";

	public static String SLASH_DELIMITER = "/";

	public static String COMMA_DELIMITER_WITH_SPACE = ", ";

	public static String COMMA_DELIMITER_WITHOUT_SPACE = ",";

	public static String COLON = ":";

	public static final Object STAR = "*";

	public static String ICRL_FILE_EXTENSION = "_icrlFile.txt";

	public static String PRIMARY_KEY_FILE_EXTENSION = "_pk.txt";

	public static String RSA_KEY_FILE_EXTENSION = "_rsa.txt";

	public static String UNL_FILE_EXTENSION = ".unl";

	public static String SQL_FILE_EXTENSION = ".sql";

	public static String SVC_SUFFIX = "_SVC";

	public static String SCHEMA_FILE_EXTENSION = "-schema";

	public static String SEMI_COLON = ";";

	public static ArrayList<String> primaryKeyList = new ArrayList<String>();

	public static long serialNum;

	public static long firstValidSerialNum;

	public static long currentValidSerialNum;

	public static int attributeCount = 0;

	/**
	 * @return the currentValidSerialNum
	 */
	public static long getCurrentValidSerialNum ()
	{
		return currentValidSerialNum;
	}

	/**
	 * @param currentValidSerialNum
	 *            the currentValidSerialNum to set
	 */
	public static void setCurrentValidSerialNum ( long currentValidSerialNum )
	{
		RegenerateSignatureModule.currentValidSerialNum = currentValidSerialNum;
	}

	/**
	 * @return the count
	 */
	public static int getCount ()
	{
		return attributeCount;
	}

	/**
	 * @param count
	 *            the count to set
	 */
	public static void setCount ( int count )
	{
		RegenerateSignatureModule.attributeCount = count;
	}

	/**
	 * @return the firstValidSerialNum
	 */
	public static long getFirstValidSerialNum ()
	{
		return firstValidSerialNum;
	}

	/**
	 * @param sNumber
	 *            the firstValidSerialNum to set
	 */
	public static void setFirstValidSerialNum ( long sNumber )
	{
		RegenerateSignatureModule.firstValidSerialNum = sNumber;
	}

	/**
	 * @return the serialNum
	 */
	public static long getSerialNum ()
	{
		return serialNum;
	}

	/**
	 * @param serialNum
	 *            the serialNum to set
	 */
	public static void setSerialNum ( long serialNum )
	{
		RegenerateSignatureModule.serialNum = serialNum;
	}

	public static void main ( String[] args )
	{
		String databaseName = "";
		ArrayList<String> tableNames = new ArrayList<String>();
		boolean verified = false;
		boolean generateSignatures = false;

		Scanner scan = new Scanner( System.in );
		System.out.println( "Enter the folderPath where schema file exists" );
		String folderPath = scan.nextLine();

		if ( folderPath.length() > 0 )
		{
			File fPath = new File( folderPath );
			if ( fPath.exists() )
			{
				System.out.println( "Enter the database name" );
				databaseName = scan.nextLine();

				if ( databaseName.length() > 0 )
				{
					Connection connect = null;
					PreparedStatement statement = null;

					try
					{
						connect = getConnection( databaseName );

						if ( connect != null )
						{
							System.out.println( "Connected to database" );
							DatabaseMetaData meta = connect.getMetaData();
							ResultSet result = meta.getTables( null, null, null, new String[] { "TABLE" } );

							while ( result.next() )
							{
								tableNames.add( result.getString( "TABLE_NAME" ) );
								System.out.println( result.getString( "TABLE_NAME" ) );
							}
						}
					}
					catch ( SQLException e1 )
					{
						e1.printStackTrace();
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
				}
			}

			if ( !tableNames.isEmpty() )
			{
				// Verify the signatures
				for ( String table : tableNames )
				{
					primaryKeyList = populatePrimaryKeyList( folderPath, databaseName, table );
					verified = verifySignatureInTable( folderPath, table, databaseName );

					if ( !verified )
					{
						System.out.println( "Verification Failed for table :: " + table );
						break;
					}
					else
					{
						System.out.println( "Verification success for table :: " + table );
					}
				}

				if ( verified )
				{
					System.out.println( " All tables verified " );
					for ( String table : tableNames )
					{
						primaryKeyList = populatePrimaryKeyList( folderPath, databaseName, table );
						generateSignatures = regenerateSignature( folderPath, table, databaseName );

						if ( !generateSignatures )
						{
							System.out.println( "Signature regeneration failed for table :: " + table );
						}
						else
						{
							System.out.println( "Signature generation success for table :: " + table );
						}

					}

					if ( generateSignatures )
					{
						// todo: write serial number to file
						saveLastValidSerialNumber( folderPath, databaseName );

					}
				}
			}

		}

	}

	/**
	 * 
	 * @param folderPath
	 * @param table
	 * @param databaseName
	 * @return
	 */
	private static boolean regenerateSignature ( String folderPath, String table, String databaseName )
	{
		boolean generateSignature = false;
		Connection connect = null;
		PreparedStatement statement = null;
		String attributeNames = null;
		ArrayList<String> attrNames = new ArrayList<String>();
		ArrayList<String> attrValues = new ArrayList<String>();
		ArrayList<String> primaryKeyValue = new ArrayList<String>();
		Map<String, String> primaryKeys = new HashMap<String, String>();
		String pkey = null;
		BigInteger message, encrypt = null;
		String rsaKeyFile = databaseName + SCHEMA_FILE_EXTENSION + RSA_KEY_FILE_EXTENSION;
		BigInteger privateKey = findRSAPrivateKey( folderPath, rsaKeyFile );
		BigInteger modulus = findRSAModulus( folderPath, rsaKeyFile );
		String sNumber = "";

		File modifiedDataFile = new File( folderPath + SLASH_DELIMITER + table + "-regenerated.unl" );
		try
		{
			if ( !modifiedDataFile.exists() )
			{
				modifiedDataFile.createNewFile();
			}
			else
			{
				modifiedDataFile.delete();
				modifiedDataFile.createNewFile();
			}

			Writer output = new BufferedWriter( new FileWriter( modifiedDataFile ) );

			connect = getConnection( databaseName );

			if ( connect != null )
			{
				attributeNames = createAttributeList( folderPath, databaseName, table );

				if ( attributeNames != null )
				{
					if ( attributeNames.endsWith( COMMA_DELIMITER_WITH_SPACE ) )
					{
						attributeNames = attributeNames.substring( 0, attributeNames.length() - 2 );
					}
					String selectQuery = buildQuery( attributeNames, table );
					System.out.println( "Connected to database again.. Get table : " + table );
					statement = connect.prepareStatement( selectQuery );
					ResultSet result = statement.executeQuery();

					while ( result.next() )
					{
						String[] attrTokens = attributeNames.trim().split( COMMA_DELIMITER_WITHOUT_SPACE );
						for ( String attrs : attrTokens )
						{
							attrs = attrs.trim();
							boolean isKey = false;

							isKey = isPrimaryKey( attrs );
							if ( !isKey )
							{
								attrNames.add( attrs );
								attrValues.add( result.getString( attrs ) );
							}
							else if ( isKey )
							{
								primaryKeyValue.add( attrs + SLASH_DELIMITER + result.getString( attrs ) );
								attrNames.add( attrs );
								attrValues.add( result.getString( attrs ) );
							}
						}

						if ( attrNames != null && attrNames.size() > 0 && primaryKeyValue != null && primaryKeyValue.size() > 0 )
						{
							primaryKeys = populateKeyValue( attrNames, primaryKeyValue, databaseName, folderPath );

							for ( int i = 0; i < attrNames.size(); )
							{
								attributeCount++;
								for ( int j = i; j < attrValues.size(); j++, i++ )
								{
									if ( primaryKeys != null && primaryKeys.size() > 0 )
									{
										if ( primaryKeys.containsKey( attrNames.get( i ) ) )
										{
											pkey = primaryKeys.get( attrNames.get( i ) );
										}
									}

									// Write the original content to file
									output.write( attrValues.get( j ) );
									output.write( "|" );

									sNumber = getCurrentValidSerialNumber( folderPath, attributeCount, databaseName );

									message = generateRSASignature( attrNames.get( i ), pkey, attrValues.get( j ), sNumber, modulus );
									if ( message != null )
									{
										System.out.println( " verify query :: message :: " + message.toString() );
										encrypt = encrypt( message, privateKey, modulus );
										System.out.println( " verify query :: encrypt : " + encrypt.toString() );
										System.out.println( " verify query :: Encrypted message in hex:: " + encrypt.toString( 16 ) );

										if ( encrypt != null )
										{
											generateSignature = true;
											output.write( encrypt.toString( 16 ) + SLASH_DELIMITER + sNumber );
											output.write( "|" );
										}
									}
									else
									{
										break;
									}
								}
								if ( !generateSignature )
								{
									break;
								}
							}
						}
						output.write( NEWLINE_DELIMITER );
						System.out.println( "Count :: " + attributeCount );
						setCurrentValidSerialNum( Long.parseLong( sNumber ) );
						attrNames.clear();
						attrValues.clear();
						primaryKeyValue.clear();

					}
					output.close();
				}

			}
		}
		catch ( SQLException e1 )
		{
			e1.printStackTrace();
		}
		catch ( IOException e )
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

		return generateSignature;
	}

	/**
	 * Method to get current valid serial number from the file
	 * 
	 * @param folderPath
	 * @param databaseName
	 * @return
	 */
	private static long getSerialNumberFromFile ( final String folderPath, String databaseName )
	{
		FileInputStream fstream = null;
		long lastValidSNum = 0;
		File icrlFile = new File( folderPath + SLASH_DELIMITER + databaseName + SCHEMA_FILE_EXTENSION + ICRL_FILE_EXTENSION );

		try
		{
			if ( icrlFile.exists() )
			{
				fstream = new FileInputStream( icrlFile );

				/* Get the object of DataInputStream */
				DataInputStream in = new DataInputStream( fstream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine;

				/* Read File Line By Line */
				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim();
					if ( strLine.startsWith( "First Valid Serial Number" ) )
					{
						continue;
					}
					if ( strLine.contains( "Current Valid Serial Number" ) )
					{
						String[] tkns = strLine.split( ":" );
						lastValidSNum = Long.parseLong( tkns[1].trim() );
					}
				}
				br.close();
				in.close();
				fstream.close();
			}
			else
			{
				icrlFile.createNewFile();
				Random prng = new Random();
				lastValidSNum = new Integer( prng.nextInt( Integer.MAX_VALUE ) );
				setSerialNum( lastValidSNum );
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return lastValidSNum;
	}

	/**
	 * 
	 * @param attributeNames
	 * @param table
	 * @return
	 */
	private static String buildQuery ( String attributeNames, String table )
	{
		StringBuffer selectQ = new StringBuffer();
		StringBuffer finalQuery = new StringBuffer();
		String[] tokens = attributeNames.split( COMMA_DELIMITER_WITH_SPACE );

		selectQ.append( "SELECT " );

		for ( String attrs : tokens )
		{
			selectQ.append( attrs );
			selectQ.append( COMMA_DELIMITER_WITH_SPACE );
		}

		String query = selectQ.toString().substring( 0, selectQ.toString().length() - 2 );
		System.out.println( "select query :: " + query );

		finalQuery.append( query );
		finalQuery.append( NEWLINE_DELIMITER );
		finalQuery.append( "FROM " + table );

		return finalQuery.toString();
	}

	/**
	 * 
	 * @param folderPath
	 * @param table
	 * @param databaseName
	 * @return
	 */
	private static boolean verifySignatureInTable ( String folderPath, String table, String databaseName )
	{
		boolean verified = false;
		Connection connect = null;
		PreparedStatement statement = null;
		String attributeNames = null;
		ArrayList<String> attrNames = new ArrayList<String>();
		ArrayList<String> attrValues = new ArrayList<String>();
		ArrayList<String> attr_SVC_Values = new ArrayList<String>();
		ArrayList<String> primaryKeyValue = new ArrayList<String>();
		ArrayList<String> attrSerialNumValues = new ArrayList<String>();
		ArrayList<String> reCalculatedValues = new ArrayList<String>();
		Map<String, String> primaryKeys = new HashMap<String, String>();

		try
		{
			connect = getConnection( databaseName );

			if ( connect != null )
			{
				attributeNames = getAllAttributes( folderPath, databaseName, table );
				System.out.println( "Connected to database again.. Get table : " + table );
				statement = connect.prepareStatement( "SELECT * FROM " + table );
				ResultSet result = statement.executeQuery();

				while ( result.next() )
				{
					String[] attrTokens = attributeNames.trim().split( COMMA_DELIMITER_WITHOUT_SPACE );
					for ( String attrs : attrTokens )
					{
						attrs = attrs.trim();
						System.out.println( attrs + " :: " + result.getString( attrs ) );

						boolean isKey = false;

						if ( !attrs.endsWith( SVC_SUFFIX ) )
						{
							isKey = isPrimaryKey( attrs );
							if ( !isKey )
							{
								attrNames.add( attrs );
								attrValues.add( result.getString( attrs ) );
							}
							else if ( isKey )
							{
								primaryKeyValue.add( attrs + SLASH_DELIMITER + result.getString( attrs ) );
							}
						}
						else
						{
							String tmp = result.getString( attrs );

							// Separate SVC value
							// and SNUM
							if ( tmp.contains( SLASH_DELIMITER ) )
							{
								String[] tkns = tmp.split( SLASH_DELIMITER );

								attr_SVC_Values.add( tkns[0] );
								attrSerialNumValues.add( tkns[1] );
							}
						}

					}

					if ( attrNames.size() > 0 && primaryKeyValue.size() > 0 )
					{
						primaryKeys = populateKeyValue( attrNames, primaryKeyValue, databaseName, folderPath );

						if ( primaryKeys != null && primaryKeys.size() > 0 )
						{
							reCalculatedValues = verifyQuery( attrNames, attrValues, primaryKeys, attrSerialNumValues, databaseName, folderPath );

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

								if ( verified )
								{
									attr_SVC_Values.clear();
									attrNames.clear();
									attrSerialNumValues.clear();
									attrValues.clear();
									primaryKeyValue.clear();
									System.out.println( "Signatures verified" );
								}
							}
						}
					}
					else
					{
						verified = true;
						attr_SVC_Values.clear();
						attrNames.clear();
						attrSerialNumValues.clear();
						attrValues.clear();
						primaryKeyValue.clear();
						System.out.println( "Signatures verified" );
					}

				}

			}
		}
		catch ( SQLException e1 )
		{
			e1.printStackTrace();
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
	 * 
	 * @param folderPath
	 * @param databaseName
	 * @param tableName
	 * @return
	 */
	private static String createAttributeList ( String folderPath, String databaseName, String tableName )
	{

		StringBuffer attrs = new StringBuffer();

		String pkFile = folderPath + SLASH_DELIMITER + databaseName + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION;
		File pkFileName = new File( pkFile );
		FileInputStream fstream;

		try
		{
			if ( pkFileName.exists() )
			{
				fstream = new FileInputStream( pkFileName );
				DataInputStream in = new DataInputStream( fstream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";

				/* Read File Line By Line */
				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim();
					String[] tkns = strLine.split( ":" );

					if ( tkns[0].equalsIgnoreCase( tableName ) )
					{
						String[] attrTkns = tkns[1].split( "," );
						for ( String attr : attrTkns )
						{
							attrs.append( attr );
							attrs.append( COMMA_DELIMITER_WITH_SPACE );
						}
					}
				}
				/* Close the input and output stream */
				br.close();
				in.close();
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
		return attrs.toString();

	}

	/**
	 * Method to get all attributes of a given tablename
	 * 
	 * @param folderPath
	 * @param databaseName
	 * @param tableName
	 * @return
	 */
	private static String getAllAttributes ( String folderPath, String databaseName, String tableName )
	{
		StringBuffer attrs = new StringBuffer();

		String pkFile = folderPath + SLASH_DELIMITER + databaseName + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION;
		File pkFileName = new File( pkFile );
		FileInputStream fstream;

		try
		{
			if ( pkFileName.exists() )
			{
				fstream = new FileInputStream( pkFileName );
				DataInputStream in = new DataInputStream( fstream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";

				/* Read File Line By Line */
				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim();
					String[] tkns = strLine.split( ":" );

					if ( tkns[0].equalsIgnoreCase( tableName ) )
					{
						String[] attrTkns = tkns[1].split( "," );
						for ( String attr : attrTkns )
						{
							if ( attr.equalsIgnoreCase( tkns[2] ) || tkns[2].contains( attr ) )
							{
								attrs.append( attr );
								attrs.append( COMMA_DELIMITER_WITH_SPACE );
							}
							else
							{
								attrs.append( attr );
								attrs.append( COMMA_DELIMITER_WITH_SPACE );
								attrs.append( attr + SVC_SUFFIX );
								attrs.append( COMMA_DELIMITER_WITH_SPACE );
							}
						}
					}
				}
				/* Close the input and output stream */
				br.close();
				in.close();
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
		return attrs.toString();
	}

	/**
	 * Method getConnection
	 * 
	 * @param dbName
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConnection ( String dbName ) throws SQLException
	{
		Connection connect = null;
		try
		{
			// This will load the MySQL driver, each DB has its own driver
			Class.forName( JDBC_DRIVER );

			// Setup the connection with the DB
			connect = DriverManager.getConnection( DB_URL + dbName + "?allowMultiQueries=true" + "&user=root&password=" );

		}
		catch ( SQLException | ClassNotFoundException e )
		{
			throw new SQLException( "Database Not Started" );
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}

		return connect;
	}

	/**
	 * Method to attach attribute and its respective key. Also stores the value
	 * for the key
	 * 
	 * @param attrNames
	 * @param primaryKeyValue
	 * @param dbName
	 * @param folderPath
	 * @return
	 */
	public static Map<String, String> populateKeyValue ( ArrayList<String> attrNames, ArrayList<String> primaryKeyValue, String dbName,
	        String folderPath )
	{
		Map<String, String> pKeys = new HashMap<String, String>();

		if ( attrNames != null && attrNames.size() > 0 && primaryKeyValue != null && primaryKeyValue.size() > 0 )
		{
			FileInputStream fstream;

			String pkFile = folderPath + SLASH_DELIMITER + dbName + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION;
			File pkFileName = new File( pkFile );

			try
			{
				if ( pkFileName.exists() )
				{
					for ( int i = 0; i < attrNames.size(); i++ )
					{
						String[] pTkns = null;
						String strLine = "";

						/* Get the object of DataInputStream */
						fstream = new FileInputStream( pkFileName );
						DataInputStream in = new DataInputStream( fstream );
						BufferedReader br = new BufferedReader( new InputStreamReader( in ) );

						/* Read File Line By Line */
						while ( ( strLine = br.readLine() ) != null )
						{
							boolean singleKey = false;
							boolean multipleKey = false;

							String[] tkns = strLine.trim().split( ":" );
							String[] tmpTkns = tkns[1].split( COMMA_DELIMITER_WITHOUT_SPACE );

							for ( int j = 0; j < tmpTkns.length; j++ )
							{
								if ( attrNames.get( i ).equalsIgnoreCase( tmpTkns[j] ) )
								{
									for ( int j2 = 0; j2 < primaryKeyValue.size(); j2++ )
									{
										pTkns = primaryKeyValue.get( j2 ).split( SLASH_DELIMITER );
										if ( !tkns[2].contains( SLASH_DELIMITER ) && tkns[2].equalsIgnoreCase( pTkns[0] ) )
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
									String[] tmp = key.split( SLASH_DELIMITER );
									if ( pKey.length() == 0 )
									{
										pKey = tmp[1];
									}
									else
									{
										pKey += SLASH_DELIMITER + tmp[1];
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
		return pKeys;
	}

	/**
	 * 
	 * @param attrNames
	 * @param attrValues
	 * @param primaryKeysMap
	 * @param attrSerialNumValues
	 * @param dbName
	 * @param folderPath
	 * @return
	 */

	public static ArrayList<String> verifyQuery ( ArrayList<String> attrNames, ArrayList<String> attrValues, Map<String, String> primaryKeysMap,
	        ArrayList<String> attrSerialNumValues, String dbName, String folderPath )
	{
		String rsaKeyFile = dbName + SCHEMA_FILE_EXTENSION + RSA_KEY_FILE_EXTENSION;
		BigInteger message, encrypt = null;
		BigInteger privateKey = findRSAPrivateKey( folderPath, rsaKeyFile );
		BigInteger modulus = findRSAModulus( folderPath, rsaKeyFile );
		ArrayList<String> reCalculatedValues = new ArrayList<String>();
		String primaryKey = "";
		// boolean found = false;

		System.out.println( " Private Key : " + privateKey + " \n modulus : " + modulus );

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
		File rsaFile = new File( folderPath + SLASH_DELIMITER + rsaKeyFile );

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
			logger.log( Level.INFO, "FileNotFoundException in findRSAModulus( ) " );
		}
		catch ( IOException e )
		{
			logger.log( Level.INFO, "IOException in findRSAModulus( ) " );
		}
		return modulus;
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
		File rsaFile = new File( folderPath + SLASH_DELIMITER + rsaKeyFile );

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
			// logger.log( Level.INFO,
			// "FileNotFoundException in findRSAPrivateKey( ) " );
		}
		catch ( IOException e )
		{
			// logger.log( Level.INFO, "IOException in findRSAPrivateKey( ) " );
		}
		return privateKey;
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
		if ( !primaryKey.contains( SLASH_DELIMITER ) )
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
	 * Method to get the product of primary keys
	 * 
	 * @param primaryKeys
	 * @return
	 */
	public static BigInteger findPrimaryKeyProduct ( String primaryKeys )
	{
		double pkInt = 0;
		BigInteger product = null;

		String[] primaryKeyTokens = primaryKeys.split( SLASH_DELIMITER );

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

	public static BigInteger encrypt ( BigInteger message, BigInteger privateKey, BigInteger modulus )
	{
		return message.modPow( privateKey, modulus );
	}

	/**
	 * Method populatePrimaryKeyList
	 * 
	 * @param folderPath
	 * @param databaseName
	 * @param table
	 * @return
	 */
	private static ArrayList<String> populatePrimaryKeyList ( String folderPath, String databaseName, String table )
	{
		File primaryKeyFile = new File( folderPath + SLASH_DELIMITER + databaseName + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION );
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

					if ( tkns[0].equalsIgnoreCase( table ) )
					{
						if ( tkns[2].contains( SLASH_DELIMITER ) )
						{
							String[] tmpTkns = tkns[2].split( SLASH_DELIMITER );
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
	 * @param folderPath
	 * @param count
	 * @param databaseName
	 * @return
	 */
	private static String getCurrentValidSerialNumber ( final String folderPath, int count, String databaseName )
	{
		long lastValidSNum;
		long currentValidSNum;
		String sNumber;
		if ( count == 1 )
		{
			lastValidSNum = getSerialNumberFromFile( folderPath, databaseName );
			setSerialNum( lastValidSNum );
			sNumber = Long.toString( getSerialNum() );
			setFirstValidSerialNum( lastValidSNum );
			System.out.println( "last valid serial number :: " + lastValidSNum );
		}
		else
		{
			currentValidSNum = getIncrementedSerialNum();
			setSerialNum( currentValidSNum );
			sNumber = Long.toString( getSerialNum() );
			System.out.println( "Current valid serial number :: " + currentValidSNum );
		}
		return sNumber;
	}

	/**
	 * Get incremented serial number
	 * 
	 * @return
	 */
	private static long getIncrementedSerialNum ()
	{
		long incSNum = getSerialNum();
		incSNum += 1;
		return incSNum;
	}

	/**
	 * Method to write the last valid serial number to file
	 * 
	 * @param folderPath
	 * @param databaseName
	 */
	private static void saveLastValidSerialNumber ( String folderPath, String databaseName )
	{
		Writer icrlFileOutput = null;
		File icrlFile = new File( folderPath + SLASH_DELIMITER + databaseName + SCHEMA_FILE_EXTENSION + ICRL_FILE_EXTENSION );

		try
		{
			if ( icrlFile != null && icrlFile.exists() )
			{
				icrlFile.delete();
				icrlFile.createNewFile();
				icrlFileOutput = new BufferedWriter( new FileWriter( icrlFile ) );
				icrlFileOutput.write( "First Valid Serial Number:" + getFirstValidSerialNum() );
				icrlFileOutput.write( NEWLINE_DELIMITER );
				icrlFileOutput.write( "Current Valid Serial Number:" + getCurrentValidSerialNum() );
				icrlFileOutput.close();
			}

		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

}
