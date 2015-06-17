/************************************************************
 * 
 * @author Archana Nanjundarao
 * Description: This module converts a given schema file to
 * ICDB specific schema file by inserting integrity codes to
 * each attribute.
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.StringTokenizer;

public class SchemaConversionModule {

	private static String modifiedSchemaFileName = null;
	private static File modifiedSchemaFile;
	private static String databaseName = null;

	public static final String TAB_DELIMITER = "\t";
	public static final Object STAR = "*";
	public static final String SEMI_COLON = ";";

	public static String SPACE_DELIMITER = " ";
	public static String NEWLINE_DELIMITER = "\n";
	public static String SLASH_DELIMITER = "/";

	public static String ICRL_FILE_EXTENSION = "_icrlFile.txt";
	public static String RSA_KEY_FILE_EXTENSION = "_rsa.txt";
	public static String SQL_FILE_EXTENSION = ".sql";
	public static String PRIMARY_KEY_FILE_EXTENSION = "_pk.txt";
	

	public static void main(String[] args) 
	{
		execute(args);
	}
	
	public static void execute(String[] args)
	{
		String selectedFileName = null;

		if (args.length != 1)
		{
			Scanner scan = new Scanner(System.in);
			System.out
					.println("Enter the schema file to be converted </folderpath/schemaFileName>");
			selectedFileName = scan.nextLine();
			scan.close();
		}
		else
		{
			selectedFileName = args[0];
		}
		

		if (selectedFileName.length() == 0) {
			System.out
					.println("Enter the schema file to be converted </folderpath/schemaFileName>");
			System.exit(1);
		} else {

			File schemaFile = new File(selectedFileName);

			if (schemaFile.isFile() && schemaFile.exists()) {
				/* Adjust the new data file name with "_ICDB" extension */
				modifiedSchemaFileName = generateModifiedSchemaFileName(selectedFileName);

				/* Create the modified schema file */
				modifiedSchemaFile = createModifiedSchemaFileName(modifiedSchemaFileName);

				System.out.println("Schema file conversion begins..");
				convertSchemaFile(selectedFileName, modifiedSchemaFile);
				createAttrKeyFile( selectedFileName );

				System.out.println("Schema file conversion ends..");
			} else {
				System.err.println("File doesn't exist");
			}
		}
	}

	/**
	 * Method to convertSchemaFile
	 * 
	 * @param schemaFileName
	 * @param modifiedSchemaFile
	 * @return
	 */
	private static String convertSchemaFile(String schemaFileName,
		File modifiedSchemaFile) {
		FileInputStream fstream;
		Writer output = null;
		String inputLine;
		StringBuffer outputContents = new StringBuffer();

		try {
			fstream = new FileInputStream(schemaFileName);

			/* Get the object of DataInputStream */
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			File outputFile = modifiedSchemaFile;
			output = new BufferedWriter(new FileWriter(outputFile));
			
			databaseName = outputFile.getName();
			databaseName = databaseName.substring(0, databaseName.indexOf('-'));

			/* Read File Line By Line */
			while ((strLine = br.readLine()) != null) {
				strLine = strLine.trim().toUpperCase();

				inputLine = addIntegrityCodes(strLine);

				if (inputLine != null) {
					output.write(inputLine.toUpperCase());
					outputContents.append(inputLine.toUpperCase());
				}
				output.write(System.getProperty("line.separator"));
				outputContents.append(System.getProperty("line.separator"));
			}

			/* Close the input and output stream */
			br.close();
			in.close();
			output.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return outputContents.toString();
	}

	/**
	 * Method to createModifiedSchemaFileName
	 * 
	 * @param modifiedSchemaFileName2
	 * @return
	 */
	private static File createModifiedSchemaFileName(
			String modifiedSchemaFileName) {
		File modifiedSchemaFile = null;

		try {
			modifiedSchemaFile = new File(modifiedSchemaFileName);

			if (modifiedSchemaFile.exists()) {
				modifiedSchemaFile.delete();
			}
			modifiedSchemaFile.createNewFile();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return modifiedSchemaFile;

	}

	/**
	 * Method to generateModifiedSchemaFileName
	 * 
	 * @param schemaFileName
	 * @return
	 */
	private static String generateModifiedSchemaFileName(String schemaFileName) {

		StringBuilder modifiedSchemaFileName = new StringBuilder();
		StringTokenizer token = new StringTokenizer(schemaFileName, ".");
		if (token.hasMoreTokens()) {
			modifiedSchemaFileName.append(token.nextToken());
			modifiedSchemaFileName.append("_ICDB.sql");
		}

		return modifiedSchemaFileName.toString();

	}

	/**
	 * Method to add integrity code
	 * 
	 * @param strLine
	 * @return
	 */
	public static String addIntegrityCodes(String strLine) {
		StringBuffer inputBuffer = new StringBuffer();
		inputBuffer.setLength(0);
		strLine = strLine.toUpperCase();

		/* Empty Line */
		if (strLine.equals("")) {
			inputBuffer.append(strLine);
		} else if (strLine.equals(");")) {
			inputBuffer.append(strLine);
		} else if (strLine.endsWith(";")) {
			strLine = strLine.toLowerCase();
			if (strLine.startsWith("drop")) {
				inputBuffer.append(strLine);
			}

			// Append ICDB to database name
			if (strLine.contains(databaseName))
				strLine = strLine.replace(databaseName, databaseName + "_ICDB");
			
			if (strLine.startsWith("create schema")) {
				String dbName = null;
				if (strLine.contains("`")) {
					strLine = strLine.replace("`", "");
				}
				inputBuffer.append(strLine);

				String[] tmp = strLine.trim().split("\\s+");
				for (int i = 0; i < tmp.length; i++) {
					if (tmp[i].equalsIgnoreCase("create") || tmp[i].equalsIgnoreCase("schema")) {
						continue;
					} else {
						if (tmp[i].contains("`")) {
							dbName = tmp[i].replace("`", "");
							dbName = dbName.substring(0, dbName.length() - 1);
						} else {
							dbName = tmp[i];
							dbName = dbName.substring(0, dbName.length() - 1)
									.toUpperCase();
						}
					}
				}

				inputBuffer.append(NEWLINE_DELIMITER);
				inputBuffer.append("ALTER DATABASE " + "`" + dbName + "`"
						+ " charset=utf8;");
			}

			if (strLine.startsWith("use")) {
				if (strLine.contains("`")) {
					strLine = strLine.replace("`", "");
				}
				inputBuffer.append(strLine);
			}
			if (strLine.startsWith("alter") || strLine.startsWith(")")) {
				inputBuffer.append(strLine);
			}
		} else if (strLine.endsWith("(")) {
			inputBuffer.append(strLine);
		} else if (strLine.endsWith(",")) {
			String tmp = null;
			Scanner tokens = new Scanner(strLine);
			inputBuffer.append(TAB_DELIMITER);
			inputBuffer.append(strLine); // insert the line as it is
			inputBuffer.append(NEWLINE_DELIMITER);

			while (tokens.hasNextLine()) {
				tmp = tokens.nextLine();
				
				if (tmp.endsWith("NOT NULL,") || tmp.endsWith("DEFAULT NULL,")) {
					if (strLine.contains("CHAR") || strLine.contains("DATE") || strLine.contains("TIMESTAMP") || 
							strLine.contains("INT") || strLine.contains("DECIMAL") || strLine.contains("FLOAT") || 
							strLine.contains("DOUBLE") || strLine.contains("BOOL")) {
						tmp = addIntegrityCode_SVC(strLine);
						inputBuffer.append(tmp);
					}
				} else if (tmp.endsWith("CHAR,") || tmp.endsWith("INT,") || tmp.endsWith("DATE,") ||
						tmp.endsWith("DATETIME,") || tmp.endsWith("TIMESTAMP,") || tmp.endsWith("BOOL,")) {
					tmp = addIntegrityCode_SVC(strLine);
					inputBuffer.append(tmp);
				} else if (tmp.endsWith("),")  || tmp.contains("DECIMAL") || tmp.contains("FLOAT")
						 || tmp.contains("DOUBLE")) {
					if (tmp.contains("CHAR")) {
						tmp = addIntegrityCode_SVC(strLine);
						tmp = tmp + ",";
						inputBuffer.append(tmp);
					}
				}
			}
			tokens.close();
		} else {
			inputBuffer.append(strLine);
		}

		return inputBuffer.toString();
	}

	/**
	 * Method to add SVC integrity codes to schema file
	 * 
	 * @param strLine
	 * @return String
	 */
	private static String addIntegrityCode_SVC(String strLine) {
		String tmpStr = null;
		StringBuffer tmpBuffer = new StringBuffer();
		StringTokenizer tokens = new StringTokenizer(strLine.trim().replace("`", ""));
		
		String text = "TEXT";
		String textComma = "TEXT,";
		
		boolean first = true;

		while (tokens.hasMoreElements()) {
			tmpStr = tokens.nextToken();

			if (tmpStr.equalsIgnoreCase("NOT") || tmpStr.equalsIgnoreCase("NULL") || tmpStr.equalsIgnoreCase("DEFAULT") || tmpStr.equalsIgnoreCase("NULL,")) {
				tmpBuffer.append(tmpStr);
				tmpBuffer.append(SPACE_DELIMITER);
			} else if (tmpStr.equalsIgnoreCase("CHAR,") || tmpStr.equalsIgnoreCase("INT,") 
					|| tmpStr.equalsIgnoreCase("TINYINT,") || tmpStr.equalsIgnoreCase("SMALLINT,")
					|| tmpStr.equalsIgnoreCase("MEDIUMINT,") || tmpStr.equalsIgnoreCase("BIGINT,")
					|| tmpStr.equalsIgnoreCase("DATE,")) {
				tmpBuffer.append(textComma);
				tmpBuffer.append(SPACE_DELIMITER);
			} else if (tmpStr.endsWith(" ),") || tmpStr.startsWith("VARCHAR(")
					|| tmpStr.startsWith("CHAR(") || tmpStr.startsWith("DECIMAL(")
					|| tmpStr.startsWith("INT(") || tmpStr.startsWith("TINYINT(")
					|| tmpStr.startsWith("SMALLINT(") || tmpStr.startsWith("MEDIUMINT(") 
					|| tmpStr.startsWith("BIGINT(") || tmpStr.equalsIgnoreCase("INT")
					|| tmpStr.equalsIgnoreCase("CHAR") || tmpStr.equalsIgnoreCase("TINYINT")
					|| tmpStr.equalsIgnoreCase("TIMESTAMP") || tmpStr.equalsIgnoreCase("DATETIME")
					|| tmpStr.equalsIgnoreCase("DATE")) {
				tmpBuffer.append(text);
				tmpBuffer.append(SPACE_DELIMITER);
			} else if (first) {
				first = false;
				tmpStr = TAB_DELIMITER + "`" + tmpStr + "_SVC`";
				tmpBuffer.append(tmpStr);
				tmpBuffer.append(SPACE_DELIMITER);
			} else if (tmpStr.contains(",")) {
				tmpBuffer.append(",");
			}
		}
		return tmpBuffer.toString();
	}
	
	/**
	 * Method to create a file containing attributes and primary key for a table
	 * 
	 * @param fileName2
	 */
	private static void createAttrKeyFile ( String fileName )
	{
		String dataFile = "";
		String attr = "";
		String key = "";
		String schemaFileName = fileName.substring( 0, fileName.length() - 4 );
		Writer output = null;

		File schemaFile = new File( fileName );
		if ( schemaFile.exists() )
		{
			try
			{
				FileInputStream fStream = new FileInputStream( schemaFile );
				DataInputStream in = new DataInputStream( fStream );
				BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
				String strLine = "";

				File primaryKeyFile = new File( schemaFileName
				        + PRIMARY_KEY_FILE_EXTENSION );

				if ( !primaryKeyFile.exists() )
				{
					primaryKeyFile.createNewFile();
				}
				else
				{
					primaryKeyFile.delete();
					primaryKeyFile.createNewFile();
				}

				output = new BufferedWriter( new FileWriter( primaryKeyFile ) );

				while ( ( strLine = br.readLine() ) != null )
				{
					strLine = strLine.trim().toUpperCase();

					if ( strLine.length() > 0 )
					{
						if ( strLine.startsWith( "DROP" ) || strLine.startsWith("USE"))
						{
							continue;
						}
						else if ( strLine.startsWith( "CREATE TABLE" ) )
						{
							String[] tmp = strLine.trim().split( "\\s+" );

							if ( tmp[2].contains( "(" ) )
							{
								tmp[2] = tmp[2].substring( 0, tmp[2].length() - 1 );
								dataFile = tmp[2];
							}
							else
							{
								dataFile = tmp[2];
							}
						}
						else if (strLine.contains( "PRIMARY KEY" ) )
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
						}
						else if ( strLine.startsWith("FOREIGN KEY") || strLine.startsWith("CONSTRAINT")
								|| strLine.startsWith( "ALTER" ) || strLine.startsWith( "UNIQUE" ) || strLine.startsWith( "CREATE SCHEMA" ))
						{
							continue;
						}
						else if ( strLine.equals( ");" ) )
						{
							StringBuffer sbuff = new StringBuffer();
							// System.out.println( "attr :: " + attr );
							sbuff.append( dataFile + ":" + attr + ":" + key );
							// System.out.println( sbuff.toString() );

							// write to output file
							try
							{
								output.write( sbuff.toString() );
								output.write( System.getProperty( "line.separator" ) );
							}
							catch ( IOException e )
							{
								e.printStackTrace();
							}
							attr = "";
							key = "";
						}
						else
						{
							// System.out.println( " FInally :: " + strLine );
							String[] tkns = strLine.trim().split( "\\s+" );

							if ( attr.length() == 0 )
							{
								attr = tkns[0];
							}
							else
							{
								attr += "," + tkns[0];
							}
						}

					}
				}

				/* Close the input and output stream */
				br.close();
				in.close();
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
	}

}
