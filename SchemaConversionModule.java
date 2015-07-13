/************************************************************
 * 
 * @author Archana Nanjundarao
 * Description: This module converts a given schema file to
 * ICDB specific schema file by inserting integrity codes to
 * each attribute.
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
import java.util.Scanner;
import java.util.StringTokenizer;

public class SchemaConversionModule {
	
	public static void main(String[] args) {
		if (args.length != 1 || args[0].length() == 0) {
			System.out.println("Enter the schema file to be converted </folderpath/schemaFileName>");
			System.exit(1);
		}

		convert(args[0]);
	}
	
	public static void convert(String fileName) {
		File schemaFile = new File(fileName);

		if (schemaFile.isFile() && schemaFile.exists()) {
			// Adjust the new data file name with "_ICDB" extension
			String modifiedSchemaFileName = generateModifiedSchemaFileName(fileName);

			// Create the modified schema file
			File modifiedSchemaFile = createModifiedSchemaFileName(modifiedSchemaFileName);

			System.out.println("Converting Schema File...");
			convertSchemaFile(fileName, modifiedSchemaFile);
			createAttrKeyFile(fileName);

			System.out.println("Finished converting Schema File.");
		} else {
			System.err.println("Schema file doesn't exist.");
			System.exit(1);
		}
	}

	/**
	 * Method to convertSchemaFile
	 * 
	 * @param schemaFileName
	 * @param modifiedSchemaFile
	 * @return
	 */
	public static String convertSchemaFile(String schemaFileName, File modifiedSchemaFile) {
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

			/* Read File Line By Line */
			while ((strLine = br.readLine()) != null) {
				strLine = strLine.trim().toUpperCase();

				inputLine = addIntegrityCodes(strLine);

				if (inputLine != null) {
					output.write(inputLine);
					outputContents.append(inputLine);
				}
				output.write(System.lineSeparator());
				outputContents.append(System.lineSeparator());
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
	 * @param modifiedSchemaFileName
	 * @return
	 */
	private static File createModifiedSchemaFileName(String modifiedSchemaFileName) {
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
			modifiedSchemaFileName.append(Symbol.ICDB_SQL_EXTENSION);
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
		if (strLine.equals("") || strLine.equals(");")) {
			inputBuffer.append(strLine);
		} else if (strLine.endsWith(";")) {
			if (strLine.startsWith("DROP")) {
				inputBuffer.append(strLine);
			}
			else if (strLine.startsWith("CREATE SCHEMA")) {
				inputBuffer.append(strLine);

				String dbName = null;
				String[] tmp = strLine.split("\\s+");
				
				dbName = tmp[3];	// Third token = database name

				inputBuffer.append(Symbol.NEWLINE_DELIMITER);
				inputBuffer.append("ALTER DATABASE ");
				inputBuffer.append("`" + dbName + "`");
				inputBuffer.append(" charset=utf8;");
			}
			else if (strLine.startsWith("USE") || strLine.startsWith("ALTER") || 
					(strLine.startsWith(")")) || strLine.endsWith("(")) {
				inputBuffer.append(strLine);
			}
			else
			{
				inputBuffer.append(strLine);
			}
		} else if (strLine.endsWith(",")) {
			String tmp = null;
			Scanner tokens = new Scanner(strLine);
			inputBuffer.append(Symbol.TAB_DELIMITER);
			inputBuffer.append(strLine); // insert the line as it is
			inputBuffer.append(Symbol.NEWLINE_DELIMITER);

			while (tokens.hasNextLine()) {
				tmp = tokens.nextLine();

				if (tmp.endsWith("NOT NULL,") || tmp.endsWith("DEFAULT NULL,") || tmp.endsWith("AUTO_INCREMENT,")) {
					if (tmp.contains("VARCHAR") || strLine.contains("CHAR") || 
							strLine.contains("DATE") || strLine.contains("DATETIME") || 
							strLine.contains("TIMESTAMP") || strLine.contains("INT") || 
							strLine.contains("DECIMAL") || strLine.contains("FLOAT") || 
							strLine.contains("DOUBLE") || strLine.contains("BOOL")) {
						tmp = addIntegrityCode_SVC(strLine);
						inputBuffer.append(tmp);
					}
				} else if (tmp.endsWith("CHAR,") || tmp.endsWith("INT,") || 
						tmp.endsWith("DATE,") || tmp.endsWith("DATETIME,") || 
						tmp.endsWith("TIMESTAMP,") || tmp.endsWith("BOOL,")) {
					tmp = addIntegrityCode_SVC(strLine);
					inputBuffer.append(tmp);
				} else if (tmp.endsWith("),")) {
					if (tmp.contains("VARCHAR") || tmp.contains("CHAR") || 
							tmp.contains("DECIMAL") || tmp.contains("FLOAT") || 
							tmp.contains("DOUBLE")) {
						tmp = addIntegrityCode_SVC(strLine);
						inputBuffer.append(tmp);
						inputBuffer.append(",");
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
	public static String addIntegrityCode_SVC(String strLine) {
		String tmpStr = null;
		StringBuffer tmpBuffer = new StringBuffer();
		StringTokenizer tokens = new StringTokenizer(strLine);

		String text = "TEXT";
		String textComma = "TEXT,";

		while (tokens.hasMoreElements()) {
			tmpStr = tokens.nextToken();
			
			if (tmpStr.startsWith("`")) {
				tmpBuffer.append(Symbol.TAB_DELIMITER + "`");
				tmpBuffer.append(tmpStr.substring(1,tmpStr.length()-1));
				tmpBuffer.append("_SVC`");
				tmpBuffer.append(Symbol.SPACE_DELIMITER);
			} else if (tmpStr.equalsIgnoreCase("NOT")
					|| tmpStr.equalsIgnoreCase("NULL")
					|| (tmpStr.equalsIgnoreCase("DEFAULT") && strLine.contains("DEFAULT NULL"))) {
				tmpBuffer.append(tmpStr);
				tmpBuffer.append(Symbol.SPACE_DELIMITER);
			} else if (tmpStr.equalsIgnoreCase("NULL,")) {
				tmpBuffer.append(tmpStr);
				tmpBuffer.append(Symbol.SPACE_DELIMITER);
			} else if (tmpStr.equalsIgnoreCase("CHAR,")
					|| tmpStr.equalsIgnoreCase("INT,")
					|| tmpStr.equalsIgnoreCase("TINYINT,")
					|| tmpStr.equalsIgnoreCase("SMALLINT,")
					|| tmpStr.equalsIgnoreCase("MEDIUMINT,")
					|| tmpStr.equalsIgnoreCase("BIGINT,")
					|| tmpStr.equalsIgnoreCase("DATE,")) {
				tmpBuffer.append(textComma);
				tmpBuffer.append(Symbol.SPACE_DELIMITER);
			} else if (tmpStr.endsWith(" ),") 
					|| tmpStr.startsWith("VARCHAR(")
					|| tmpStr.startsWith("CHAR(")
					|| tmpStr.startsWith("DECIMAL(")
					|| tmpStr.startsWith("INT(")
					|| tmpStr.startsWith("TINYINT(")
					|| tmpStr.startsWith("SMALLINT(")
					|| tmpStr.startsWith("MEDIUMINT(")
					|| tmpStr.startsWith("BIGINT(")
					|| tmpStr.equalsIgnoreCase("CHAR")
					|| tmpStr.equalsIgnoreCase("INT")
					|| tmpStr.equalsIgnoreCase("TINYINT")
					|| tmpStr.equalsIgnoreCase("SMALLINT")
					|| tmpStr.equalsIgnoreCase("MEDIUMINT")
					|| tmpStr.equalsIgnoreCase("BIGINT")
					|| tmpStr.equalsIgnoreCase("TIMESTAMP")
					|| tmpStr.equalsIgnoreCase("DATETIME")
					|| tmpStr.equalsIgnoreCase("DATE")) {
				tmpBuffer.append(text);
				tmpBuffer.append(Symbol.SPACE_DELIMITER);
			} else if (tmpStr.contains(",")) {
				tmpBuffer.append(",");
			}
		}
		
		return tmpBuffer.toString();
	}
	
	/**
	 * Method to create a file containing attributes and primary key for a table
	 * 
	 * @param fileName
	 */
	public static void createAttrKeyFile(String fileName) {
		String dataFile = "";
		String attr = "";
		String key = "";
		String schemaFileName = fileName.substring(0, fileName.length() - 4);
		Writer output = null;

		File schemaFile = new File(fileName);
		if (schemaFile.exists()) {
			try {
				FileInputStream fStream = new FileInputStream(schemaFile);
				DataInputStream in = new DataInputStream(fStream);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(in));
				String strLine = "";

				File primaryKeyFile = new File(schemaFileName
						+ Symbol.PRIMARY_KEY_FILE_EXTENSION);

				if (!primaryKeyFile.exists()) {
					primaryKeyFile.createNewFile();
				} else {
					primaryKeyFile.delete();
					primaryKeyFile.createNewFile();
				}

				output = new BufferedWriter(new FileWriter(primaryKeyFile));

				while ((strLine = br.readLine()) != null) {
					strLine = strLine.trim().toUpperCase().replace("`", "");

					if (strLine.length() > 0) {
						if (strLine.startsWith("DROP") || strLine.startsWith("USE") || strLine.startsWith("/*")) {
							continue;
						} else if (strLine.startsWith("CREATE TABLE")) {
							String[] tmp = strLine.trim().split("\\s+");

							if (tmp[2].contains("(")) {
								tmp[2] = tmp[2].substring(0,
										tmp[2].length() - 1);
								dataFile = tmp[2];
							} else {
								dataFile = tmp[2];
							}
						} else if (strLine.contains("PRIMARY KEY")) {
							String tmp = strLine;
							String[] tokens = tmp.split("\\(");

							if (!tokens[1].endsWith(",")) {
								key = tokens[1].substring(0,
										tokens[1].length() - 1);
							} else {
								key = tokens[1].substring(0,
										tokens[1].length() - 2);
							}

							if (key.contains(",")) {
								key = key.replace(",", Symbol.SLASH_DELIMITER);
								key = key.replace("/ ", Symbol.SLASH_DELIMITER);
							}
						} else if (strLine.startsWith("FOREIGN KEY") || strLine.startsWith("CONSTRAINT") || 
						strLine.startsWith("ALTER") || strLine.startsWith("UNIQUE") || 
						strLine.startsWith("CREATE SCHEMA")) {
							continue;
						} else if (strLine.equals(");") || (strLine.startsWith(")") && strLine.endsWith(";"))) {
							StringBuffer sbuff = new StringBuffer();
							// System.out.println( "attr :: " + attr );
							sbuff.append(dataFile + ":" + attr + ":" + key);
							// System.out.println( sbuff.toString() );

							// write to output file
							try {
								output.write(sbuff.toString());
								output.write(System
										.getProperty("line.separator"));
							} catch (IOException e) {
								e.printStackTrace();
							}
							attr = "";
							key = "";
						} else {
							// System.out.println( " FInally :: " + strLine );
							String[] tkns = strLine.trim().split("\\s+");

							if (attr.length() == 0) {
								attr = tkns[0];
							} else {
								attr += "," + tkns[0];
							}
						}

					}
				}

				/* Close the input and output stream */
				br.close();
				in.close();
				output.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
