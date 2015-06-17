/************************************************************
 * 
 * @author Archana Nanjundarao
 * Description: This module converts a given data file to
 * ICDB specific data file by generating and  inserting 
 * integrity codes to each attribute.
 * 
 ************************************************************/

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
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
	private final static Logger log = Logger.getLogger( QueryConversionModule.class.getName() );

	public static String SPACE_DELIMITER = " ";

	public static String NEWLINE_DELIMITER = "\n";

	public static String SLASH_DELIMITER = "/";

	public static String COMMA_DELIMITER_WITH_SPACE = ", ";

	public static String COMMA_DELIMITER_WITHOUT_SPACE = ",";

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

	public static boolean isUpdateQ ()
	{
		return updateQ;
	}

	public static void setUpdateQ ( boolean updateQ )
	{
		QueryConversionModule.updateQ = updateQ;
	}

	public static boolean isDeleteQ ()
	{
		return deleteQ;
	}

	public static void setDeleteQ ( boolean deleteQ )
	{
		QueryConversionModule.deleteQ = deleteQ;
	}

	public static boolean isSelectQ ()
	{
		return selectQ;
	}

	public static void setSelectQ ( boolean selectQ )
	{
		QueryConversionModule.selectQ = selectQ;
	}

	public static boolean isInsertQ ()
	{
		return insertQ;
	}

	public static void setInsertQ ( boolean insertQ )
	{
		QueryConversionModule.insertQ = insertQ;
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
						System.out.println( convertedQuery );
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
		String databaseName = "";

		String dataFile = "";
		BigInteger encrypt = null;
		boolean isStar = false;
		String tableName = "";
		StringBuffer originalQuery = new StringBuffer();
		StringBuffer deleteQuery = new StringBuffer();

		Map<String, String> attributeMap = new HashMap<String, String>();

		// String query = readQueryFile( queryFile );

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
										selectLine.add( SPACE_DELIMITER );
										selectLine.add( tkns );
										selectLine.add( COMMA_DELIMITER_WITH_SPACE );

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
											selectLine.add( tkns );
											selectLine.add( SPACE_DELIMITER );
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
									File fPath = new File( folderPath ).getParentFile();
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
						else if ( queryLines[i].startsWith( "Update" ) || queryLines[i].startsWith( "update" ) || queryLines[i].startsWith( "UPDATE" ) )
						{ // Update query
							String[] updateLineTkns = queryLines[i].split( "\\s+" );
							tableName = updateLineTkns[1];
							fromLine.append( "FROM" + " " + tableName );

						}
						else if ( queryLines[i].startsWith( "Set" ) || queryLines[i].startsWith( "set" ) || queryLines[i].startsWith( "SET" ) )
						{ // Update query
							String[] setTkns = queryLines[i].split( "\\s+" );
							for ( int j = 1; j < setTkns.length; j++ )
							{
								if ( setTkns[j].endsWith( COMMA_DELIMITER_WITHOUT_SPACE ) )
								{
									setTkns[j] = setTkns[j].replace( ',', ' ' );
								}
								String[] tkns = setTkns[j].split( "=" );

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
								// Update query
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
			// retrieveDeletedSNums( folderPath, deleteQuery, databaseName );
			convertedQ.append( originalQuery.toString() );
		}

		return convertedQ.toString();
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
			log.log( Level.INFO, "FileNotFoundException in findRSAModulus( ) " );
		}
		catch ( IOException e )
		{
			log.log( Level.INFO, "IOException in findRSAModulus( ) " );
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
			log.log( Level.INFO, "FileNotFoundException in findRSAPrivateKey( ) " );
		}
		catch ( IOException e )
		{
			log.log( Level.INFO, "IOException in findRSAPrivateKey( ) " );
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

}
