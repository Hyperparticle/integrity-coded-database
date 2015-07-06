/************************************************************
 * 
 * @author Archana Nanjundarao
 * Description: This module converts a given data file to
 * ICDB specific data file by generating and  inserting 
 * integrity codes to each attribute.
 * 
 ************************************************************/


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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueryConversionModule
{
	private final static Logger logger = Logger.getLogger( QueryConversionModule.class.getName() );

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

	private static String databaseName;

	public static long serialNum;

	public static boolean selectQ = Boolean.FALSE;
	public static boolean insertQ = Boolean.FALSE;
	public static boolean updateQ = Boolean.FALSE;
	public static boolean deleteQ = Boolean.FALSE;

	public static ArrayList<String> primaryKeyList = new ArrayList<String>();

	// JDBC driver name and database URL
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/";

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
		QueryConversionModule.selectQ = selectQ;
	}

	/**
	 * @return the insertQ
	 */
	public static boolean isInsertQ ()
	{
		return insertQ;
	}

	/**
	 * @param insertQ
	 *            the insertQ to set
	 */
	public static void setInsertQ ( boolean insertQ )
	{
		QueryConversionModule.insertQ = insertQ;
	}

	/**
	 * @return the updateQ
	 */
	public static boolean isUpdateQ ()
	{
		return updateQ;
	}

	/**
	 * @param updateQ
	 *            the updateQ to set
	 */
	public static void setUpdateQ ( boolean updateQ )
	{
		QueryConversionModule.updateQ = updateQ;
	}

	/**
	 * @return the deleteQ
	 */
	public static boolean isDeleteQ ()
	{
		return deleteQ;
	}

	/**
	 * @param deleteQ
	 *            the deleteQ to set
	 */
	public static void setDeleteQ ( boolean deleteQ )
	{
		QueryConversionModule.deleteQ = deleteQ;
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
		QueryConversionModule.databaseName = databaseName;
	}

	/**
	 * Getter method for serial number
	 * 
	 * @return
	 */
	public static long getSerialNum ()
	{
		return serialNum;
	}

	/**
	 * Setter method for serial number
	 * 
	 * @param serialNumber
	 */
	public static void setSerialNum ( long serialNumber )
	{
		serialNum = serialNumber;
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
	 * @param args
	 */
	public static void main ( String[] args )
	{
		String initialQuery = "";
		String folderPath = "";

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
			File fPath = new File( folderPath );
			if ( fPath.exists() )
			{
				if (args.length != 2)
				{
					System.out.println( "Enter the sql query file to be converted" );
					initialQuery = scan.nextLine();
				}
				else
				{
					initialQuery = args[1];
				}
				

				if ( initialQuery.length() > 0 )
				{
					File queryFile = new File( initialQuery );
					if ( queryFile.exists() && queryFile.isFile() )
					{
						String convertedQuery = convertSQLQuery( queryFile, folderPath );

						if ( convertedQuery.length() > 0 )
						{
							System.out.println( "*********************************************************" );
							System.out.println( convertedQuery );
						}
						else
						{
							System.out.println( " Query conversion failed " );
						}
					}
					else
					{
						System.out.println( "QUery file doesnt exist" );
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
	 * Method to convert the sql query to ICDB query. This method processes line
	 * by line of the query
	 * 
	 * @param query
	 * @param folderPath
	 * @return
	 */
	private static String convertSQLQuery ( final File queryFile, final String folderPath )
	{
		StringBuffer convertedQ = new StringBuffer();
		LinkedHashSet<String> selectLine = new LinkedHashSet<String>();
		StringBuffer fromLine = new StringBuffer();
		StringBuffer whereLine = new StringBuffer();
		StringBuffer insertLine = new StringBuffer();
		StringBuffer valuesLine = new StringBuffer();
		StringBuffer updateLine = new StringBuffer();
		StringBuffer setLine = new StringBuffer();
		StringBuffer intermediateQ = new StringBuffer();
		String databaseName = "";

		String dataFile = "";
		BigInteger encrypt = null;
		boolean isStar = false;
		String tableName = "";
		StringBuffer originalQuery = new StringBuffer();
		StringBuffer deleteQuery = new StringBuffer();

		Map<String, String> attributeMap = new HashMap<String, String>();

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

				String[] queryLines = strLine.split( "\n" );

				for ( int i = 0; i < queryLines.length; i++ )
				{
					if ( queryLines[i].length() > 0 )
					{
						if ( queryLines[i].startsWith( "use" ) || queryLines[i].startsWith( "USE" ) || queryLines[i].startsWith( "Use" ) )
						{
							databaseName = convertUseStatement( convertedQ, queryLines[i] );

						}
						else if ( queryLines[i].startsWith( "Select" ) || queryLines[i].startsWith( "select" ) || queryLines[i].startsWith( "SELECT" ) )
						{
							// Select clause
							selectQ = Boolean.TRUE;
							setSelectQ( Boolean.TRUE );
							String[] selectTkns = queryLines[i].split( "\\s+" );
							for ( String tkns : selectTkns )
							{
								tkns = tkns.toUpperCase();
								if ( tkns.equalsIgnoreCase( "Select" ) || tkns.equalsIgnoreCase( "select" ) || tkns.equalsIgnoreCase( "SELECT" ) )
								{
									selectLine.add( tkns );
									selectLine.add( SPACE_DELIMITER );
								}
								else if ( tkns.equalsIgnoreCase( "DISTINCT" ) )
								{
									continue;
								}
								else if ( tkns.equals( STAR ) )
								{
									isStar = Boolean.TRUE;
								}
								else
								{
									if ( tkns.endsWith( "," ) )
									{
										tkns = tkns.substring( 0, tkns.length() - 1 );
									}

									if ( tkns.startsWith( "MIN(" ) || tkns.startsWith( "MAX(" ) || tkns.startsWith( "AVG(" ) )
									{
										String[] tokens = tkns.split( "\\(" );
										String tmp = tokens[1].substring( 0, tokens[1].length() - 1 );

										selectLine.add( tmp );
										selectLine.add( COMMA_DELIMITER_WITHOUT_SPACE );
										selectLine.add( tmp + SVC_SUFFIX );
										selectLine.add( COMMA_DELIMITER_WITHOUT_SPACE );

									}
									else
									{
										if ( tkns.equals( "MIN" ) || tkns.equals( "MAX" ) || tkns.equals( "AVG" ) )
										{
											continue;
										}
										else
										{
											selectLine.add( tkns );
											selectLine.add( COMMA_DELIMITER_WITH_SPACE );
											selectLine.add( tkns + SVC_SUFFIX );
										}
									}
								}

							}
						}
						else if ( queryLines[i].startsWith( "Insert" ) || queryLines[i].startsWith( "insert" ) || queryLines[i].startsWith( "INSERT" ) )
						{
							// Insert query
							insertQ = Boolean.TRUE;
							setInsertQ( Boolean.TRUE );
							int count = 0;

							insertLine.append( NEWLINE_DELIMITER );

							String[] insTkns = queryLines[i].split( "\\(" );
							for ( String indTkns : insTkns )
							{
								indTkns = indTkns.trim();
								if ( indTkns.length() > 0 )
								{
									if ( indTkns.startsWith( "Insert " ) || indTkns.startsWith( "INSERT " ) || indTkns.startsWith( "insert " ) )
									{
										insertLine.append( indTkns );
										insertLine.append( SPACE_DELIMITER );
										insertLine.append( '(' );

										String[] dTkns = indTkns.split( "\\s+" );
										dataFile = dTkns[2];
									}
									else
									{
										String[] attTkns = indTkns.split( "," );

										for ( String attNames : attTkns )
										{
											attNames = attNames.trim();
											count++;
											// Remove the last ')'
											if ( attNames.charAt( attNames.length() - 1 ) == ')' )
											{
												attNames = attNames.substring( 0, attNames.length() - 1 );
											}

											attributeMap.put( Integer.toString( count ), attNames );

											insertLine.append( attNames );
											insertLine.append( COMMA_DELIMITER_WITH_SPACE );
											insertLine.append( attNames + SVC_SUFFIX );

											if ( attTkns.length != count )
											{
												insertLine.append( COMMA_DELIMITER_WITH_SPACE );
											}
											else
											{
												insertLine.append( " )" );
											}
										}
									}
								}
							}

						}
						else if ( queryLines[i].startsWith( "Values" ) || queryLines[i].startsWith( "values" ) || queryLines[i].startsWith( "VALUES" ) )
						{
							int count = 0;
							String pkFile = databaseName + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION;
							String rsaKeyFile = databaseName + SCHEMA_FILE_EXTENSION + RSA_KEY_FILE_EXTENSION;
							BigInteger modulus = findRSAModulus( folderPath, rsaKeyFile );
							BigInteger privateKey = findRSAPrivateKey( folderPath, rsaKeyFile );
							String primaryKeys = readPrimaryKeyFile( folderPath, pkFile, dataFile );

							long lastValidSNum, currentValidSNum;
							String sNumber = "";

							// Values clause - insert query
							String[] valTkns = queryLines[i].split( "\\(" );
							for ( String tkns : valTkns )
							{
								tkns = tkns.trim();

								if ( tkns.equalsIgnoreCase( "values" ) || tkns.startsWith( "Values" ) || tkns.startsWith( "VALUES" ) )
								{
									valuesLine.append( tkns );
									valuesLine.append( SPACE_DELIMITER );
									valuesLine.append( '(' );
								}
								else
								{
									String[] insValTkns = tkns.split( "'" );
									for ( String values : insValTkns )
									{
										values = values.trim();
										if ( values.length() > 0 && !values.equals( "," ) && !values.equals( ");" ) )
										{
											count++;
											values = values.trim();

											if ( count == 1 )
											{
												lastValidSNum = getSerialNumberFromFile( folderPath );
												// System.out.println(
												// "last valid serial number from file :: "
												// + lastValidSNum );
												setSerialNum( lastValidSNum );
												sNumber = Long.toString( getSerialNum() );
											}
											else
											{
												currentValidSNum = getIncrementedSerialNum();
												setSerialNum( currentValidSNum );
												sNumber = Long.toString( getSerialNum() );
												System.out.println( "Current valid serial number :: " + currentValidSNum );
											}

											BigInteger message = generateRSASignature( count, values, primaryKeys, sNumber, attributeMap, modulus );

											if ( message != null )
											{
												encrypt = encrypt( message, privateKey, modulus );
											}

											valuesLine.append( "'" );
											valuesLine.append( values );
											valuesLine.append( "'" );
											valuesLine.append( COMMA_DELIMITER_WITH_SPACE );
											valuesLine.append( "'" );
											valuesLine.append( encrypt + SLASH_DELIMITER + sNumber );
											valuesLine.append( "'" );

											if ( count != attributeMap.size() )
											{
												valuesLine.append( COMMA_DELIMITER_WITH_SPACE );
											}
										}
									}

									valuesLine.append( ");" );
								}
							}
						}
						else if ( queryLines[i].startsWith( "Update" ) || queryLines[i].startsWith( "update" ) || queryLines[i].startsWith( "UPDATE" ) )
						{
							// Update query
							updateQ = Boolean.TRUE;
							setUpdateQ( Boolean.TRUE );
							String[] updateLineTkns = queryLines[i].split( "\\s+" );
							tableName = updateLineTkns[1];
							updateLine.append( queryLines[i].toUpperCase() );
							updateLine.append( NEWLINE_DELIMITER );

						}
						else if ( queryLines[i].startsWith( "Set" ) || queryLines[i].startsWith( "set" ) || queryLines[i].startsWith( "SET" ) )
						{
							// Update query
							updateQ = Boolean.TRUE;
							setUpdateQ( Boolean.TRUE );
							setLine.append( queryLines[i].toUpperCase() );
							String pkFile = getDatabaseName() + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION;
							String pKey = readPrimaryKeyFile( folderPath, pkFile, tableName );

							String[] setTkns = queryLines[i].split( "Set" );
							for ( int j = 0; j < setTkns.length; j++ )
							{
								setTkns[j] = setTkns[j].trim();

								if ( setTkns[j].length() > 0 )
								{
									if ( !setTkns[j].contains( COMMA_DELIMITER_WITHOUT_SPACE ) )
									{
										String[] tmp = setTkns[j].split( "=" );
										intermediateQ.append( "SELECT " );
										intermediateQ.append( tmp[0].toUpperCase() );
										intermediateQ.append( COMMA_DELIMITER_WITH_SPACE );
										intermediateQ.append( tmp[0].toUpperCase() + SVC_SUFFIX );
										intermediateQ.append( COMMA_DELIMITER_WITH_SPACE );
										intermediateQ.append( pKey );
										intermediateQ.append( NEWLINE_DELIMITER );
										intermediateQ.append( "FROM " + tableName.toUpperCase() );
										intermediateQ.append( NEWLINE_DELIMITER );
									}
									else
									{
										String[] tmp = setTkns[j].split( COMMA_DELIMITER_WITHOUT_SPACE );
										intermediateQ.append( "SELECT " );
										for ( String str : tmp )
										{
											String[] tkns = str.trim().split( "=" );

											intermediateQ.append( tkns[0].trim().toUpperCase() );
											intermediateQ.append( COMMA_DELIMITER_WITH_SPACE );
											intermediateQ.append( tkns[0].trim().toUpperCase() + SVC_SUFFIX );
											intermediateQ.append( COMMA_DELIMITER_WITH_SPACE );
										}
										intermediateQ.append( pKey );
										intermediateQ.append( NEWLINE_DELIMITER );
										intermediateQ.append( "FROM " + tableName.toUpperCase() );
										intermediateQ.append( NEWLINE_DELIMITER );
									}
								}
							}
						}
						else if ( queryLines[i].startsWith( "From" ) || queryLines[i].startsWith( "from" ) || queryLines[i].startsWith( "FROM" ) )
						{
							// From clause - insert line as is
							fromLine.append( queryLines[i].toUpperCase() );
							fromLine.append( SPACE_DELIMITER );

							String[] fromTkns = queryLines[i].split( "\\s+" );
							for ( String tkns : fromTkns )
							{
								tkns = tkns.toUpperCase();
								if ( tkns.equalsIgnoreCase( "From" ) || tkns.equalsIgnoreCase( "from" ) || tkns.equalsIgnoreCase( "FROM" )
								        || tkns.equalsIgnoreCase( "Join" ) || tkns.equalsIgnoreCase( "join" ) || tkns.equalsIgnoreCase( "JOIN" ) )
								{
									continue;
								}
								else
								{
									File fPath = new File( folderPath );
									String pkFile = databaseName + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION;
									for ( File file : fPath.listFiles() )
									{
										if ( file.getAbsoluteFile().getName().equals( pkFile ) )
										{
											File pkFileName = new File( folderPath + SLASH_DELIMITER + file.getAbsoluteFile().getName() );
											FileInputStream fstream1;
											try
											{
												if ( pkFileName.exists() )
												{
													fstream1 = new FileInputStream( pkFileName );
													DataInputStream in1 = new DataInputStream( fstream1 );
													BufferedReader br1 = new BufferedReader( new InputStreamReader( in1 ) );
													String Line = "";

													/* Read File Line By Line */
													while ( ( Line = br1.readLine() ) != null )
													{
														Line = Line.trim();

														if ( tkns.endsWith( COMMA_DELIMITER_WITHOUT_SPACE ) )
														{
															tkns = tkns.replace( ",", "" );
														}

														if ( tkns.endsWith( SEMI_COLON ) )
														{
															tkns = tkns.replace( ";", "" );
														}

														if ( Line.startsWith( tkns + ":" ) )
														{
															String[] pkTkns = Line.trim().split( ":" );

															// 3rd token is
															// always the
															// primary key/keys
															if ( !pkTkns[2].contains( SLASH_DELIMITER ) )
															{
																if ( !primaryKeyList.contains( pkTkns[2] ) )
																{
																	primaryKeyList.add( pkTkns[2] );
																}
																selectLine.add( COMMA_DELIMITER_WITH_SPACE );
																selectLine.add( pkTkns[2].toUpperCase() );
																selectLine.add( COMMA_DELIMITER_WITH_SPACE );
															}
															else
															{
																String[] pKeyTkns = pkTkns[2].split( SLASH_DELIMITER );
																for ( String keys : pKeyTkns )
																{
																	if ( !primaryKeyList.contains( keys ) )
																	{
																		primaryKeyList.add( keys );
																	}
																	selectLine.add( keys.toUpperCase() );
																	selectLine.add( COMMA_DELIMITER_WITH_SPACE );
																}

															}
															if ( isStar )
															{
																String[] tmp = pkTkns[1].split( COMMA_DELIMITER_WITHOUT_SPACE );

																for ( int j = 0; j < tmp.length; j++ )
																{
																	selectLine.add( tmp[j] );
																	selectLine.add( COMMA_DELIMITER_WITH_SPACE );

																	if ( !primaryKeyList.contains( tmp[j] ) )
																	{
																		selectLine.add( tmp[j] + SVC_SUFFIX );
																		selectLine.add( COMMA_DELIMITER_WITH_SPACE );
																	}
																}
															}

															break;
														}
													}
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
											break;
										}
									}
								}
							}
						}
						else if ( queryLines[i].startsWith( "DELETE FROM" ) || queryLines[i].startsWith( "delete from" )
						        || queryLines[i].startsWith( "Delete From" ) )
						{
							// Delete query
							String[] deleteTkns = queryLines[i].split( "\\s+" );
							tableName = deleteTkns[2];
							deleteQ = Boolean.TRUE;
							setDeleteQ( Boolean.TRUE );

							originalQuery.append( queryLines[i].toUpperCase() );
							originalQuery.append( NEWLINE_DELIMITER );
						}
						else
						{
							if ( deleteQ )
							{
								String attr = getAllAttributes( folderPath, databaseName, tableName );
								attr = attr.substring( 0, attr.length() - 2 );

								deleteQuery.append( "SELECT " + attr );
								deleteQuery.append( NEWLINE_DELIMITER );
								deleteQuery.append( "FROM " + tableName.toUpperCase() );
								deleteQuery.append( NEWLINE_DELIMITER );
								deleteQuery.append( queryLines[i].toUpperCase() );

								System.out.println( "Intermediate delete query::  \n" + deleteQuery.toString() + NEWLINE_DELIMITER );

								originalQuery.append( queryLines[i].toUpperCase() );
							}
							else if ( updateQ )
							{
								intermediateQ.append( queryLines[i].toUpperCase() );
								whereLine.append( NEWLINE_DELIMITER );
								whereLine.append( queryLines[i].toUpperCase() );

								System.out.println( "Intermediate delete query::  \n" + updateLine.toString() + setLine.toString()
								        + whereLine.toString() );
							}
							else
							{
								// Where clause
								whereLine.append( queryLines[i].toUpperCase() );
								whereLine.append( SPACE_DELIMITER );
								String[] tokens = queryLines[i].split( "[\\<=+\\=+\\>=+\\<+\\>+]" );

								for ( int j = 0; j < tokens.length; j++ )
								{
									String tmp = tokens[j].trim();

									if ( !tmp.equals( "" ) )
									{
										String conditionTkn = tmp.substring( tmp.lastIndexOf( " " ) + 1 ).toUpperCase();

										if ( conditionTkn.endsWith( ";" ) )
										{
											conditionTkn = conditionTkn.replace( ";", "" );
										}

										boolean isAttribute = isAttribute( conditionTkn, folderPath );

										if ( isAttribute && !primaryKeyList.contains( conditionTkn ) )
										{
											selectLine.add( COMMA_DELIMITER_WITH_SPACE );
											selectLine.add( conditionTkn );
											selectLine.add( COMMA_DELIMITER_WITH_SPACE );
											selectLine.add( conditionTkn + SVC_SUFFIX );
										}
									}
								}
							}
						}
					}
				}

			}
			/* Close the input and output stream */
			br.close();
			in.close();
		}
		catch ( FileNotFoundException e1 )
		{
			e1.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		StringBuffer cQuery = new StringBuffer();

		if ( selectQ )
		{
			System.out.println( "Its a SELECT statement" );
			Iterator<String> itr = selectLine.iterator();

			while ( itr.hasNext() )
			{
				String element = itr.next();

				if ( element.equalsIgnoreCase( "Select" ) )
				{
					cQuery.append( element );
					cQuery.append( SPACE_DELIMITER );
				}
				else if ( element.length() == 0 )
				{
					continue;
				}
				else if ( element.length() == 1 )
				{
					continue;
				}
				else if ( element.equals( "," ) || element.equals( ", " ) || element.equals( " ," ) )
				{
					continue;
				}
				else if ( element.equalsIgnoreCase( "MIN" ) || element.equalsIgnoreCase( "MAX" ) || element.equalsIgnoreCase( "AVG" ) )
				{
					cQuery.append( element );
					cQuery.append( SPACE_DELIMITER );
				}
				else
				{
					cQuery.append( element );
					cQuery.append( COMMA_DELIMITER_WITH_SPACE );
				}
			}

			// Remove the last comma
			String finalQuery = cQuery.toString().trim();
			finalQuery = finalQuery.substring( 0, finalQuery.length() - 1 );

			convertedQ.append( finalQuery );
			convertedQ.append( NEWLINE_DELIMITER );
			convertedQ.append( fromLine.toString() );
			convertedQ.append( NEWLINE_DELIMITER );
			convertedQ.append( whereLine.toString() );
			convertedQ.append( NEWLINE_DELIMITER );
		}
		else if ( insertQ )
		{
			valuesLine = valuesLine.replace( valuesLine.toString().length() - 4, valuesLine.toString().length() - 2, "" );
			cQuery.append( insertLine.toString() );
			cQuery.append( NEWLINE_DELIMITER );
			cQuery.append( valuesLine.toString() );

			convertedQ.append( cQuery.toString() );
		}
		else if ( deleteQ )
		{
			convertedQ.append( originalQuery.toString() );
		}
		else if ( updateQ )
		{
			String primaryKeyValue = "";
			primaryKeyValue = executeIntermediateQ( intermediateQ, folderPath );
			if ( !primaryKeyValue.equals( "" ) )
			{
				System.out.println( updateLine.toString() + setLine.toString() + whereLine.toString() );
				setUpdateQ( true );
				String setline = generateSignature( setLine, primaryKeyValue, folderPath );

				convertedQ.append( updateLine.toString() );
				convertedQ.append( setline );
				convertedQ.append( whereLine.toString() );
				saveCurrentSerialNumber( folderPath );
			}
			else
			{
				convertedQ.setLength( 0 );
			}
		}

		return convertedQ.toString();
	}

	/**
	 * Method generateSignature
	 * 
	 * @param string
	 * @param primaryKeyValue
	 * @param folderPath
	 * @return
	 */
	private static String generateSignature ( StringBuffer setLine, String primaryKeyValue, String folderPath )
	{
		BigInteger encrypt = null;
		int count = 0;
		String rsaKeyFile = getDatabaseName() + SCHEMA_FILE_EXTENSION + RSA_KEY_FILE_EXTENSION;
		BigInteger modulus = findRSAModulus( folderPath, rsaKeyFile );
		BigInteger privateKey = findRSAPrivateKey( folderPath, rsaKeyFile );

		long lastValidSNum, currentValidSNum;
		String sNumber = "";

		String[] tkns = setLine.toString().trim().split( "SET" );
		for ( int i = 0; i < tkns.length; i++ )
		{
			tkns[i] = tkns[i].trim();

			if ( tkns[i].length() > 0 )
			{
				if ( tkns[i].contains( COMMA_DELIMITER_WITHOUT_SPACE ) )
				{
					String[] str = tkns[i].trim().split( COMMA_DELIMITER_WITHOUT_SPACE );
					for ( String tmpTkns : str )
					{
						count++;
						// Find the serial number
						if ( count == 1 )
						{
							lastValidSNum = getSerialNumberFromFile( folderPath, getDatabaseName() );
							System.out.println( "last valid serial number from file :: " + lastValidSNum );
							setSerialNum( lastValidSNum );
							sNumber = Long.toString( getSerialNum() );
						}
						else
						{
							currentValidSNum = getIncrementedSerialNum();
							setSerialNum( currentValidSNum );
							sNumber = Long.toString( getSerialNum() );
							System.out.println( "Current valid serial number :: " + currentValidSNum );
							// updateCurrentValidSerialNumber( folderPath,
							// sNumber );
						}

						String[] tmp = tmpTkns.trim().split( "=" );
						String attrName = tmp[0].trim();
						String attrValue = tmp[1].trim();
						attrValue = attrValue.substring( 1, attrValue.length() - 1 );

						BigInteger message = generateRSASignature( count, attrValue, primaryKeyValue, sNumber, attrName, modulus );

						if ( message != null )
						{
							encrypt = encrypt( message, privateKey, modulus );

							if ( encrypt != null )
							{
								setLine.append( COMMA_DELIMITER_WITH_SPACE );
								setLine.append( attrName + SVC_SUFFIX + "=" + "'" );
								setLine.append( encrypt.toString( 16 ) + SLASH_DELIMITER + sNumber );
								setLine.append( "'" );
							}
						}
					}
				}
				else
				{
					count++;
					// Find the serial number
					if ( count == 1 )
					{
						lastValidSNum = getSerialNumberFromFile( folderPath, getDatabaseName() );
						System.out.println( "last valid serial number from file :: " + lastValidSNum );
						setSerialNum( lastValidSNum );
						sNumber = Long.toString( getSerialNum() );
					}
					String[] tmp = tkns[i].split( "=" );
					String attrName = tmp[0];
					String attrValue = tmp[1];
					attrValue = attrValue.substring( 1, attrValue.length() - 1 );

					BigInteger message = generateRSASignature( count, attrValue, primaryKeyValue, sNumber, attrName, modulus );

					if ( message != null )
					{
						encrypt = encrypt( message, privateKey, modulus );

						if ( encrypt != null )
						{
							setLine.append( COMMA_DELIMITER_WITH_SPACE );
							setLine.append( attrName + SVC_SUFFIX + "=" + "'" );
							setLine.append( encrypt.toString( 16 ) + SLASH_DELIMITER + sNumber );
							setLine.append( "'" );
						}
					}
				}
			}
		}

		return setLine.toString();
	}

	/**
	 * Method updateCurrentValidSerialNumber
	 * 
	 * @param folderPath
	 * @param sNumber
	 */
	private static void updateCurrentValidSerialNumber ( String folderPath, String sNumber )
	{
		logger.log( Level.INFO, "Inside updateCurrentValidSerialNumber()" );
		StringBuffer tmp = new StringBuffer();
		File icrlFileName = null;
		File fPath = new File( folderPath );
		String icrlFile = getDatabaseName() + SCHEMA_FILE_EXTENSION + ICRL_FILE_EXTENSION;
		for ( File file : fPath.listFiles() )
		{
			if ( file.getAbsoluteFile().getName().equals( icrlFile ) )
			{
				icrlFileName = new File( folderPath + SLASH_DELIMITER + file.getAbsoluteFile().getName() );
				break;
			}
		}
		try
		{
			if ( icrlFileName != null && icrlFileName.exists() )
			{
				FileInputStream fstream = new FileInputStream( icrlFileName );
				DataInputStream in = new DataInputStream( fstream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";

				/* Read File Line By Line */
				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim();

					if ( strLine.startsWith( "Current Valid Serial Number:" ) )
					{
						String[] tkn = strLine.trim().split( COLON );
						strLine = tkn[0] + COLON + sNumber;

					}
					tmp.append( strLine );
					tmp.append( NEWLINE_DELIMITER );
				}

				FileWriter fileWriter = new FileWriter( icrlFileName, true );
				BufferedWriter bufferWritter = new BufferedWriter( fileWriter );

				if ( tmp.length() > 0 )
				{
					bufferWritter.append( tmp.toString() );
				}
				bufferWritter.close();
				fileWriter.close();
			}

		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	/**
	 * Method generateRSASignature
	 * 
	 * @param count
	 * @param individualToken
	 * @param primaryKeys
	 * @param sNumber
	 * @param attrName
	 * @param modulus
	 * @return
	 */
	public static BigInteger generateRSASignature ( int count, String individualToken, String primaryKeys, String sNumber, String attrName,
	        BigInteger modulus )
	{
		BigInteger message = null;
		BigInteger pkBigInt, tknBigInt, attrBigInt;

		// Check if its a single primary key or combination of keys
		if ( !primaryKeys.contains( SLASH_DELIMITER ) )
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

		// Check if data file token is a number or not
		if ( !isIntegerRegex( individualToken ) )
		{
			tknBigInt = convertToInt( individualToken );
		}
		else
		{
			tknBigInt = new BigInteger( individualToken );
		}

		// Convert attribute name token to integer
		attrBigInt = convertToInt( attrName );

		message = pkBigInt.multiply( tknBigInt ).multiply( attrBigInt ).multiply( new BigInteger( sNumber ) );
		message = message.mod( modulus );

		return message;
	}

	/**
	 * Method to get current valid serial number from the file
	 * 
	 * @param folderPath
	 * @param dbName
	 * @return
	 */
	private static long getSerialNumberFromFile ( final String folderPath, String dbName )
	{
		FileInputStream fstream = null;
		File fPath = new File( folderPath );
		// File[] files = fPath.listFiles();
		String[] files = fPath.list();
		File icrlFile = null;
		long lastValidSNum = 0;

		for ( String indFile : files )
		{
			if ( indFile.startsWith( dbName ) && indFile.contains( ICRL_FILE_EXTENSION ) )
			{
				icrlFile = new File( folderPath + SLASH_DELIMITER + indFile );
				break;
			}
		}

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
	 * Method executeIntermediateQ
	 * 
	 * @param intermediateQ
	 * @param folderPath
	 * @return
	 */
	private static String executeIntermediateQ ( StringBuffer intermediateQ, String folderPath )
	{
		String primaryKeyValue = "";
		String dbName = getDatabaseName();
		Connection connect = null;
		PreparedStatement statement = null;

		String primaryKey = populatePrimaryKey( intermediateQ, dbName, folderPath );
		try
		{
			connect = getConnection( dbName );

			if ( connect != null )
			{
				System.out.println( "Connected to database" );
				statement = connect.prepareStatement( intermediateQ.toString() );
				ResultSet result = statement.executeQuery();

				primaryKeyValue = retrieveUpdateQueryValues( intermediateQ.toString(), result, dbName, folderPath, primaryKey );
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

		return primaryKeyValue;
	}

	/**
	 * Method retrieveUpdateQueryValues
	 * 
	 * @param intermediateQ
	 * @param result
	 * @param dbName
	 * @param folderPath
	 * @param primaryKey
	 * @return
	 * @throws SQLException
	 */
	private static String retrieveUpdateQueryValues ( String intermediateQ, ResultSet result, String dbName, String folderPath, String primaryKey )
	        throws SQLException
	{
		boolean verified = false;
		String pKey = null;
		ArrayList<String> attrNames = new ArrayList<String>();
		ArrayList<String> attrValues = new ArrayList<String>();
		ArrayList<String> tableNames = new ArrayList<String>();
		ArrayList<String> primaryKeyValue = new ArrayList<String>();
		ArrayList<String> attr_SVC_Values = new ArrayList<String>();
		ArrayList<String> attrSerialNumValues = new ArrayList<String>();
		ArrayList<String> reCalculatedValues = new ArrayList<String>();
		Map<String, String> primaryKeys = new HashMap<String, String>();

		while ( result.next() )
		{
			System.out.println( " Inside while for verify intermediate update query " );
			String[] attributes = intermediateQ.split( NEWLINE_DELIMITER );
			for ( String lineAttr : attributes )
			{
				if ( lineAttr.startsWith( "SELECT" ) )
				{
					String[] indAttr = lineAttr.trim().split( COMMA_DELIMITER_WITHOUT_SPACE );
					for ( String selectAttr : indAttr )
					{
						boolean isKey = false;
						selectAttr = selectAttr.trim();

						if ( selectAttr.startsWith( "SELECT" ) )
						{
							String[] tmpAttr = selectAttr.split( "\\s+" );
							selectAttr = tmpAttr[1];
						}

						if ( !selectAttr.endsWith( SVC_SUFFIX ) )
						{
							isKey = isPrimaryKey( selectAttr, primaryKey );
							if ( !isKey )
							{
								attrNames.add( selectAttr );
								attrValues.add( result.getString( selectAttr ) );
							}
							else if ( isKey )
							{
								primaryKeyValue.add( selectAttr + SLASH_DELIMITER + result.getString( selectAttr ) );
							}
						}
						else
						{
							isKey = isPrimaryKey( selectAttr, primaryKey );
							if ( !isKey )
							{
								String tmp = result.getString( selectAttr );

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
							if ( fromAttr.endsWith( COMMA_DELIMITER_WITHOUT_SPACE ) )
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

		primaryKeys = populateKeyValue( attrNames, primaryKeyValue, dbName.toUpperCase(), folderPath );

		reCalculatedValues = verifyQuery( attrNames, attrValues, primaryKeys, attrSerialNumValues, dbName, folderPath );

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

		if ( verified )
		{
			pKey = primaryKeyValue.toString();
			updateRevokedSerialNumbers( folderPath, attrSerialNumValues );
		}

		return pKey;
	}

	/**
	 * Method populatePrimaryKey
	 * 
	 * @param query
	 * @param dbName
	 * @param folderPath
	 * @return
	 */
	private static String populatePrimaryKey ( StringBuffer query, String dbName, String folderPath )
	{
		File primaryKeyFile = new File( folderPath + SLASH_DELIMITER + dbName + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION );
		String dataFile = null;
		String primaryKeys = null;

		String[] dTkns = query.toString().split( NEWLINE_DELIMITER );
		for ( String tkns : dTkns )
		{
			if ( tkns.startsWith( "FROM" ) || tkns.startsWith( "From" ) )
			{
				String[] fromTkns = tkns.split( "\\s+" );
				dataFile = fromTkns[1];
			}
		}

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
					if ( strLine.startsWith( dataFile ) )
					{
						String[] tkns = strLine.split( ":" );
						primaryKeys = tkns[2];
						break;
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

		return primaryKeys;
	}

	/**
	 * Method to convert "use" query statement
	 * 
	 * @param convertedQ
	 * @param queryLine
	 * @param i
	 * @return
	 */
	private static String convertUseStatement ( StringBuffer convertedQ, String queryLine )
	{
		String databaseName;
		convertedQ.append( queryLine.toUpperCase() );
		convertedQ.append( NEWLINE_DELIMITER );
		convertedQ.append( NEWLINE_DELIMITER );
		String[] useTkns = queryLine.trim().split( "\\s+" );

		if ( useTkns[1].contains( "`" ) )
		{
			useTkns[1] = useTkns[1].replace( "`", "" );
			databaseName = useTkns[1].trim().substring( 0, useTkns[1].length() - 1 );
			setDatabaseName( databaseName );
		}
		else
		{
			databaseName = useTkns[1].trim().substring( 0, useTkns[1].length() - 1 );
			setDatabaseName( databaseName );
		}
		return databaseName;
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
			logger.log( Level.INFO, "FileNotFoundException in findRSAPrivateKey( ) " );
		}
		catch ( IOException e )
		{
			logger.log( Level.INFO, "IOException in findRSAPrivateKey( ) " );
		}
		return privateKey;
	}

	/**
	 * Method to read the primary key from the file
	 * 
	 * @param folderPath
	 * @param pkFile
	 * @param dataFile
	 * 
	 * @return primaryKey
	 */
	public static String readPrimaryKeyFile ( final String folderPath, final String pkFile, final String dataFile )
	{
		String primaryKeys = "";
		File primaryKeyFile = new File( folderPath + SLASH_DELIMITER + pkFile );

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
					if ( strLine.startsWith( dataFile.toUpperCase() ) )
					{
						String[] tkns = strLine.split( ":" );
						primaryKeys = tkns[2];
						break;
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

		return primaryKeys;
	}

	/**
	 * Method to get current valid serial number from the file
	 * 
	 * @param folderPath
	 * @return
	 */
	private static long getSerialNumberFromFile ( final String folderPath )
	{
		FileInputStream fstream = null;
		File fPath = new File( folderPath );
		File[] files = fPath.listFiles();
		File icrlFile = null;
		long lastValidSNum = 0;

		for ( File indFile : files )
		{
			if ( indFile.getAbsoluteFile().getName().contains( ICRL_FILE_EXTENSION ) )
			{
				icrlFile = new File( folderPath + SLASH_DELIMITER + indFile.getAbsoluteFile().getName() );
				break;
			}
		}

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
	 * Method generateRSASignature
	 * 
	 * @param count
	 * @param individualToken
	 * @param primaryKeys
	 * @param sNumber
	 * @param attributeMap
	 * @param modulus
	 * @return
	 */
	public static BigInteger generateRSASignature ( int count, String individualToken, String primaryKeys, String sNumber,
	        Map<String, String> attributeMap, BigInteger modulus )
	{
		BigInteger message = null;
		BigInteger pkBigInt, tknBigInt, attrBigInt;

		// Attribute name
		String attrName = attributeMap.get( Integer.toString( count ) );

		// Check if its a single primary key or combination of keys
		if ( !primaryKeys.contains( SLASH_DELIMITER ) )
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

		// Check if data file token is a number or not
		if ( !isIntegerRegex( individualToken ) )
		{
			tknBigInt = convertToInt( individualToken );
		}
		else
		{
			tknBigInt = new BigInteger( individualToken );
		}

		// Convert attribute name token to integer
		attrBigInt = convertToInt( attrName );

		message = pkBigInt.multiply( tknBigInt ).multiply( attrBigInt ).multiply( new BigInteger( sNumber ) );
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
	 * Method to encrypt the message
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

		File fPath = new File( folderPath );
		String pkFile = getDatabaseName() + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION;
		for ( File file : fPath.listFiles() )
		{
			if ( file.getAbsoluteFile().getName().equals( pkFile ) )
			{
				File pkFileName = new File( folderPath + SLASH_DELIMITER + file.getAbsoluteFile().getName() );
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
									attrs.append( attr + SVC_SUFFIX );
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
			}
		}
		return attrs.toString();
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

	/**
	 * Method to check if the given token is an attribute
	 * 
	 * @param tmp
	 * @param folderPath
	 * @return true or false
	 */
	private static boolean isAttribute ( final String tmp, final String folderPath )
	{
		File fPath = new File( folderPath );
		String pkFile = getDatabaseName() + SCHEMA_FILE_EXTENSION + PRIMARY_KEY_FILE_EXTENSION;
		for ( File file : fPath.listFiles() )
		{
			if ( file.getAbsoluteFile().getName().equals( pkFile ) )
			{
				File pkFileName = new File( folderPath + SLASH_DELIMITER + file.getAbsoluteFile().getName() );
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
							String[] attrTkns = tkns[1].split( "," );

							for ( String attr : attrTkns )
							{
								if ( attr.equalsIgnoreCase( tmp ) )
								{
									return true;
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
			}
		}
		return false;
	}

	/**
	 * Method isPrimaryKey
	 * 
	 * @param selectAttr
	 * @param primaryKey
	 * @return boolean
	 */
	private static boolean isPrimaryKey ( String selectAttr, String primaryKey )
	{
		boolean isPrimaryKey = false;

		if ( primaryKey.contains( SLASH_DELIMITER ) && primaryKey.contains( selectAttr ) )
		{
			isPrimaryKey = true;
		}
		else if ( primaryKey.equalsIgnoreCase( selectAttr ) )
		{
			isPrimaryKey = true;
		}

		return isPrimaryKey;
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

			if ( folderPath != null )
			{
				File fPath = new File( folderPath );

				if ( fPath != null )
				{
					File[] files = fPath.listFiles();
					File pKFile = null;

					for ( File indFile : files )
					{
						if ( getDatabaseName() != null && getDatabaseName().length() > 0 )
						{
							if ( indFile.getAbsoluteFile().getName().toUpperCase().startsWith( dbName )
							        && indFile.getAbsoluteFile().getName().contains( PRIMARY_KEY_FILE_EXTENSION ) )
							{
								pKFile = new File( fPath + SLASH_DELIMITER + indFile.getAbsoluteFile().getName() );
								break;
							}
						}
					}

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
			}
		}
		return pKeys;
	}

	/**
	 * Method updateRevokedSerialNumbers
	 * 
	 * @param folderPath
	 * @param attrSerialNumValues
	 */
	private static void updateRevokedSerialNumbers ( String folderPath, ArrayList<String> attrSerialNumValues )
	{
		logger.log( Level.INFO, "Inside updateRevokedSerialNumbers()" );
		boolean found = false;
		File icrlFileName = null;
		File fPath = new File( folderPath );
		String icrlFile = getDatabaseName() + SCHEMA_FILE_EXTENSION + ICRL_FILE_EXTENSION;
		for ( File file : fPath.listFiles() )
		{
			if ( file.getAbsoluteFile().getName().equals( icrlFile ) )
			{
				icrlFileName = new File( folderPath + SLASH_DELIMITER + file.getAbsoluteFile().getName() );
				break;
			}
		}
		try
		{
			if ( icrlFileName != null && icrlFileName.exists() )
			{
				FileInputStream fstream = new FileInputStream( icrlFileName );
				DataInputStream in = new DataInputStream( fstream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";
				ArrayList<String> sNumToFile = new ArrayList<String>();
				boolean isRevoked = false;

				/* Read File Line By Line */
				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim();

					if ( strLine.startsWith( "Revoked Serial Numbers:" ) )
					{
						isRevoked = true;
						String[] tmp = strLine.split( COLON );
						if ( tmp[1].contains( COMMA_DELIMITER_WITHOUT_SPACE ) )
						{
							String[] sNumTknsFromFile = tmp[1].trim().split( COMMA_DELIMITER_WITHOUT_SPACE );
							for ( String serialNumber : attrSerialNumValues )
							{
								serialNumber = serialNumber.trim();

								for ( String sNums : sNumTknsFromFile )
								{
									sNums = sNums.trim();
									if ( sNums.equals( serialNumber ) )
									{
										found = true;
										break;
									}
								}

								if ( !found && !sNumToFile.contains( serialNumber ) )
								{
									sNumToFile.add( serialNumber );
								}
							}
						}
					}
				}

				FileWriter fileWriter = new FileWriter( icrlFileName, true );
				BufferedWriter bufferWritter = new BufferedWriter( fileWriter );

				if ( !sNumToFile.isEmpty() )
				{
					if ( !isRevoked )
					{
						bufferWritter.append( NEWLINE_DELIMITER );
						bufferWritter.append( "Revoked Serial Numbers: " );
					}
					for ( String serialNumber : sNumToFile )
					{
						bufferWritter.append( serialNumber );
						bufferWritter.append( COMMA_DELIMITER_WITHOUT_SPACE );
					}

				}
				bufferWritter.close();
				fileWriter.close();
			}

		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
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
	 * Method verifyQuery
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
	 * Method generateRSASignature
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
	 * Method to save the current valid serial number
	 * 
	 * @param folderPath
	 */
	public static void saveCurrentSerialNumber ( String folderPath )
	{
		FileInputStream fstream;

		if ( folderPath != null && !folderPath.equals( "" ) )
		{
			File fPath = new File( folderPath );
			File icrlFileName = null;
			StringBuffer sb = new StringBuffer();
			StringBuffer rStrBuff = new StringBuffer();

			String icrlFile = getDatabaseName() + SCHEMA_FILE_EXTENSION + ICRL_FILE_EXTENSION;
			for ( File file : fPath.listFiles() )
			{
				if ( file.getAbsoluteFile().getName().equals( icrlFile ) )
				{
					icrlFileName = new File( folderPath + SLASH_DELIMITER + file.getAbsoluteFile().getName() );
					break;
				}
			}

			try
			{
				if ( icrlFileName.exists() )
				{
					fstream = new FileInputStream( icrlFileName );

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
							// continue;
						}
						else if ( strLine.startsWith( "Current Valid Serial Number" ) )
						{
							hasLastValidSerialNum = Boolean.TRUE;
							String[] tkns = strLine.split( ":" );
							sb.append( tkns[0] + ":" );
						}
						else
						{
							rStrBuff.append( strLine );
						}

					}

					if ( hasLastValidSerialNum )
					{
						icrlFileName.delete();
						icrlFileName.createNewFile();
						icrlFileOutput = new BufferedWriter( new FileWriter( icrlFileName ) );
						icrlFileOutput.write( sb.toString() );
						String lastValidSNum = Long.toString( getSerialNum() );
						icrlFileOutput.write( lastValidSNum );
						icrlFileOutput.write( NEWLINE_DELIMITER );
						icrlFileOutput.write( rStrBuff.toString() );
						icrlFileOutput.close();
					}

					if ( !hasLastValidSerialNum )
					{
						String lastValidSNum = Long.toString( getSerialNum() );
						icrlFileOutput = new BufferedWriter( new FileWriter( icrlFileName, true ) );
						icrlFileOutput.write( NEWLINE_DELIMITER );
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

			System.out.println( " Last serial number :: " + getSerialNum() );

		}
	}

}
