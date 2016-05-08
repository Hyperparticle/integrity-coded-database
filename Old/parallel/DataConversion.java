package parallel;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

public class DataConversion
{
	private int bitSize = 1024;
	private final BigInteger one = new BigInteger("1");

	public void execute(DataObj dataObject)
	{
		String[] tmp = dataObject.getSchemaFilePath().getFileName().toString().split( "-" );
		String databaseName = tmp[0];

		File dataFile = new File(dataObject.getUnlFilePath().toString());

		if ( dataFile.isFile() && dataFile.exists() )
		{
			generateSerialNum( databaseName, dataObject );
			System.out.println( " Serial Number generated.." + dataObject.getSerialNumber());

			generateRSASignature(databaseName, dataObject);
			System.out.println( "RSA keys generated.. " );

			dataObject.setPrimaryKeyList(findPrimaryKey( dataObject));
			findPrimaryKeyPosition( dataObject, new File( dataObject.getSchemaFilePath().toString()));

			convertDataFile( databaseName, dataFile, dataObject );
			System.out.println( " Final Serial Number :: " + dataObject.getSerialNumber());

			saveLastValidSerialNumber( databaseName, dataFile.getParent(), dataObject );
		}
		else
		{
			System.out.println( "DataFile doesn't exist" );
			System.exit( 1 );
		}
	}
	
	private void generateSerialNum ( String inputDataFile, DataObj dataObject)
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

			File icrlFile = new File( dataObject.getSchemaFilePath().getParent() + Symbol.SLASH_DELIMITER + inputDataFile + Symbol.ICRL_FILE_EXTENSION );

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
			dataObject.setSerialNumber(serialNumber);	
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

	private void generateRSASignature (String databaseName, DataObj dataObject)
	{
		File rsaKeyFile = new File( dataObject.getSchemaFilePath().getParent() + Symbol.SLASH_DELIMITER + databaseName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.RSA_KEY_FILE_EXTENSION );

		BigInteger p = BigInteger.probablePrime( bitSize, dataObject.getRandom() );
		BigInteger q = BigInteger.probablePrime( bitSize, dataObject.getRandom() );
		BigInteger phi = ( p.subtract( one ) ).multiply( q.subtract( one ) ); 
		
		dataObject.setModulus(p.multiply( q ));
		dataObject.setPublicKey(new BigInteger( "65537" ));
		dataObject.setPrivateKey(dataObject.getPublicKey().modInverse(phi));

		System.out.println( " Data file :: Private Key : " + dataObject.getPrivateKey());
		System.out.println( " Data file :: Modulus : " + dataObject.getModulus());

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
			rsaKeyFileOutput.write( dataObject.getPublicKey().toString() );
			rsaKeyFileOutput.write( Symbol.NEWLINE_DELIMITER );
			rsaKeyFileOutput.write( "privatekey:" );
			rsaKeyFileOutput.write( dataObject.getPrivateKey().toString() );
			rsaKeyFileOutput.write( Symbol.NEWLINE_DELIMITER );
			rsaKeyFileOutput.write( "modulus:" );
			rsaKeyFileOutput.write( dataObject.getModulus().toString() );
			rsaKeyFileOutput.write( Symbol.NEWLINE_DELIMITER );
			rsaKeyFileOutput.close();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> findPrimaryKey ( DataObj dataObject )
	{
		String key;
		File sFile = new File( dataObject.getSchemaFilePath().toString());
		Path dFile = Paths.get( dataObject.getUnlFilePath().toString());
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
				fStream = new FileInputStream( dataObject.getSchemaFilePath().toString());
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

							dataObject.getPrimaryKeyList().add(key);
							dataObject.getPrimaryKeyListWithUnl().put(unlFile.toUpperCase(), key);
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
		return dataObject.getPrimaryKeyList();
	}

	public void findPrimaryKeyPosition ( DataObj dataObject, File schemaFile )
	{
		int position = 0;
		String unlFile = "";

		if ( schemaFile.exists() && dataObject.getPrimaryKeyList() != null && !dataObject.getPrimaryKeyList().isEmpty() && dataObject.getPrimaryKeyListWithUnl() != null
				&& !dataObject.getPrimaryKeyListWithUnl().isEmpty() )
		{
			try
			{
				FileInputStream fStream = new FileInputStream( schemaFile );
				DataInputStream in = new DataInputStream( fStream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";
				String pKey = "";

				dataObject.getAtrList().clear();;
				dataObject.getAttributeMap().clear();

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

								String primaryKey = dataObject.getPrimaryKeyListWithUnl().get( unlFile );

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
												if ( !dataObject.getKeyPositionMap().containsKey( unlFile ) )
												{
													dataObject.getKeyPositionMap().put( unlFile, Integer.toString( position ) );
												}
												else
												{
													String value = dataObject.getKeyPositionMap().get( unlFile );
													if ( value.equals( Integer.toString( position ) ) )
													{
														value = Integer.toString( position );
													}
													else
													{
														value = value + Symbol.SLASH_DELIMITER + Integer.toString( position );
													}
													dataObject.getKeyPositionMap().put( unlFile, value );
												}
											}
										}
									}
									else
									{
										if ( pKey.equalsIgnoreCase( primaryKey ) )
										{
											if ( !dataObject.getKeyPositionMap().containsKey( unlFile ) )
											{
												dataObject.getKeyPositionMap().put( unlFile, Integer.toString( position ) );
											}
										}
									}
								}
								String[] attributeTokens = strLine.trim().split( "\\s+" );
								for ( int k = 0; k < attributeTokens.length;)
								{
									dataObject.getAttributeMap().put( position + Symbol.SLASH_DELIMITER + unlFile, attributeTokens[k] );
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
	
	private void convertDataFile ( String databaseName, File dataFile, DataObj dataObject )
	{
		String primaryKeys = "";
		BigInteger message, encrypt = null;
		Writer output = null;
		String fileLocation = dataFile.getParent();
		Path dFile = Paths.get( dataFile.toString() );
		String unlFile = dFile.getFileName().toString();
		unlFile = unlFile.replace( Symbol.UNL_FILE_EXTENSION, "" );

		try
		{
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
				System.out.println( "Serial Number :: " + dataObject.getSerialNumber() );
				primaryKeys = getPrimaryKeys( strLine.trim(), fileLocation, unlFile, dataObject);

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
						output.write( dataFileTokens[j] );
						output.write( "|" );

						pos = j;
						message = generateRSASignature( Integer.toString( pos + 1 ), dataFileTokens[j], primaryKeys, unlFile,
								Long.toString( dataObject.getSerialNumber() ), fileLocation, dataObject);

						if ( message != null )
						{
							System.out.println( " Data file :: message :: " + message.toString() );
							encrypt = encrypt( message, dataObject );
						}
						
						if ( encrypt.toString() != null )
						{
							System.out.println( " Data file :: encrypt :: " + encrypt.toString( 16 ) );
							output.write( encrypt.toString( 16 ) + Symbol.SLASH_DELIMITER + Long.toString( dataObject.getSerialNumber() ) );
							System.out.println( " Final data file value :: " + encrypt.toString( 16 ) + Symbol.SLASH_DELIMITER
									+ Long.toString( dataObject.getSerialNumber() ) );

							if (j != dataFileTokens.length-1)
								output.write( "|" );
						}
						dataObject.setSerialNumber(getIncrementedSerialNum(dataObject));
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
		} catch (Exception e) {
			System.out.println("You hit this exception");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public String generateModifiedDataFileName ( String dataFileName )
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
	
	private String getPrimaryKeys ( String line, String fileLocation, String dataFile, DataObj dataObject)
	{
		String primaryKeysPosition = "";
		String primaryKeys = "";
		dataFile = dataFile.toUpperCase();

		if ( dataObject.getKeyPositionMap().containsKey( dataFile ) )
		{
			primaryKeysPosition = dataObject.getKeyPositionMap().get( dataFile );
		}

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
	
	private BigInteger generateRSASignature ( String attrPosition, String individualToken, String primaryKeys, String dataFile,
			String sNumber, String fileLocation, DataObj dataObject )
	{
		BigInteger message = null;
		BigInteger pkBigInt, tknBigInt, attrBigInt;
		dataFile = dataFile.toUpperCase();

		if ( dataObject.getAttributeMap().containsKey( attrPosition + Symbol.SLASH_DELIMITER + dataFile ) )
		{

			String attrNameTokens = dataObject.getAttributeMap().get( attrPosition + Symbol.SLASH_DELIMITER + dataFile );

			System.out.println( " ******************************************* " );
			System.out.println( " data file :: " + dataFile + " attr name :: " + attrNameTokens + " attr val :: " + individualToken + " pk name :: "
					+ primaryKeys + " serial Number : " + sNumber );

			if ( !primaryKeys.contains( Symbol.SLASH_DELIMITER ) )
			{
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

			if ( !isIntegerRegex( individualToken ) )
			{
				tknBigInt = convertToInt( individualToken );
			}
			else
			{
				tknBigInt = new BigInteger( individualToken );
			}

			System.out.println( "Data File :: tknBigInt :: " + tknBigInt.toString() );

			attrBigInt = convertToInt( attrNameTokens );

			System.out.println( "Data File :: attrBigInt :: " + attrBigInt.toString() );

			message = pkBigInt.multiply( tknBigInt ).multiply( attrBigInt ).multiply( new BigInteger( sNumber ) );
			message = message.mod( dataObject.getModulus() );
		}
		return message;

	}

	public boolean isIntegerRegex ( String str )
	{
		return str.matches( "^[0-9]+$" );
	}
	
	public BigInteger convertToInt ( String input )
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
	
	public BigInteger findPrimaryKeyProduct ( String primaryKeys )
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
	
	public BigInteger encrypt ( BigInteger message, DataObj dataObject)
	{
		return message.modPow( dataObject.getPrivateKey(), dataObject.getModulus() );
	}
	
	private long getIncrementedSerialNum (DataObj dataObject)
	{
		long incSNum = dataObject.getSerialNumber();
		incSNum += 1;
		return incSNum;
	}
	
	private void saveLastValidSerialNumber (String dataBaseName, String folderPath, DataObj dataObject )
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
				if ( dataBaseName != null && dataBaseName.length() > 0 )
				{
					if ( indFile.getAbsoluteFile().getName().startsWith( dataBaseName)
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
						String lastValidSNum = Long.toString( dataObject.getSerialNumber() );
						icrlFileOutput.write( lastValidSNum );
						icrlFileOutput.close();
					}

					else if ( !hasLastValidSerialNum )
					{
						String lastValidSNum = Long.toString( dataObject.getSerialNumber() );
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
}
