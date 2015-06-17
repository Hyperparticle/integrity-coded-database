package dataConversion;

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
import java.util.ArrayList;
import java.util.StringTokenizer;

import symbols.Symbol;

public class DataConverter {

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
				serialNumber = KeyGenerator.getSerialNumber();
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
				System.out.println( "Serial Number :: " + serialNumber );
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
						message = generateRSASignature( Integer.toString( pos + 1 ), dataFileTokens[j], primaryKeys, unlFile,
						        Long.toString( getSerialNumber() ), fileLocation );

						if ( message != null )
						{
							System.out.println( " Data file :: message :: " + message.toString() );
							encrypt = encrypt( message );
						}

						if ( encrypt.toString() != null )
						{
							System.out.println( " Data file :: encrypt :: " + encrypt.toString( 16 ) );
							output.write( encrypt.toString( 16 ) + Symbol.SLASH_DELIMITER + Long.toString( KeyGenerator.getSerialNumber() ) );
							System.out.println( " Final data file value :: " + encrypt.toString( 16 ) + Symbol.SLASH_DELIMITER
							        + Long.toString( KeyGenerator.getSerialNumber() ) );
							
							if (j != dataFileTokens.length-1)
								output.write( "|" );
						}

						setSerialNumber( KeyGenerator.getIncrementedSerialNum() );
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
		while ( token.hasMoreTokens() )
		{
			modifiedDataFileName.append( token.nextToken() );
			modifiedDataFileName.append( "_ICDB" );
			modifiedDataFileName.append( ".unl" );
			break;
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

		if ( unlFile.endsWith( UNL_FILE_EXTENSION ) )
		{
			unlFile = unlFile.replace( UNL_FILE_EXTENSION, "" );
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
						if ( strLine.trim().contains( "PRIMARY KEY" ) || strLine.trim().contains( "primary key" )
						        || strLine.trim().contains( "Primary key" ) )
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
								key = key.replace( ",", SLASH_DELIMITER );
								key = key.replace( "/ ", SLASH_DELIMITER );
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
						if ( strLine.trim().startsWith( "DROP SCHEMA IF EXISTS" ) || strLine.trim().startsWith( "CREATE SCHEMA" )
						        || strLine.trim().startsWith( "USE" ) || strLine.trim().startsWith( "CREATE TABLE" )
						        || strLine.trim().startsWith( "CONSTRAINT" ) || strLine.trim().startsWith( "ALTER" ) || strLine.trim().equals( "" )
						        || strLine.trim().equals( ");" ) )
						{
							if ( strLine.trim().startsWith( "CREATE TABLE" ) || strLine.trim().startsWith( "Create Table" ) )
							{
								String[] str = strLine.replaceAll( "(^\\s+|\\s+$)", "" ).split( "\\s+" );
								if ( str[str.length - 1].equals( "(" ) )
								{
									unlFile = str[str.length - 2];
									unlFile = unlFile.concat( UNL_FILE_EXTENSION );
								}
								else
								{
									unlFile = str[str.length - 1];
									unlFile = unlFile.concat( UNL_FILE_EXTENSION );
								}

							}
							position = 0;
							continue;
						}
						else
						{
							// Include more data types and also handle case
							// sensitive
							if ( strLine.contains( "CHAR" ) || strLine.contains( "INT" ) || strLine.contains( "VARCHAR" )
							        || strLine.contains( "DECIMAL" ) || strLine.contains( "DATE" ) )
							{
								String[] lineTkns = strLine.trim().split( "\\s+" );
								pKey = lineTkns[0].trim();
								position += 1;
								unlFile = unlFile.replace( ".unl", "" );

								String primaryKey = primaryKeyListWithUnl.get( unlFile );

								if ( primaryKey != null )
								{
									if ( primaryKey.contains( SLASH_DELIMITER ) )
									{
										String[] keyTokens = primaryKey.split( SLASH_DELIMITER );

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
													else if ( !value.contains( Integer.toString( position ) ) )
													{
														value = value + SLASH_DELIMITER + Integer.toString( position );
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
									attributeMap.put( position + SLASH_DELIMITER + unlFile, attributeTokens[k] );
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
			if ( primaryKeysPosition.contains( Integer.toString( i + 1 ) ) )
			{
				if ( primaryKeys.length() == 0 )
				{
					primaryKeys = tokens[i + 1];
				}
				else
				{
					primaryKeys += SLASH_DELIMITER + tokens[i];
				}
			}
		}

		return primaryKeys;

	}
	
}
