/**
 * 
 */
package schemaConversion;

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

import symbols.Symbol;

/**
 * @author Dan
 *
 */
public class SchemaConverter {

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

				inputLine = CodeGenerator.addIntegrityCodes(strLine);

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
					strLine = strLine.trim().toUpperCase();

					if (strLine.length() > 0) {
						if (strLine.startsWith("DROP")) {
							continue;
						} else if (strLine.startsWith("USE")) {
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
						} else if (strLine.startsWith("PRIMARY KEY")
								|| strLine.contains("PRIMARY KEY")) {
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
						} else if (strLine.startsWith("FOREIGN KEY")) {
							continue;
						} else if (strLine.startsWith("CONSTRAINT")) {
							continue;
						} else if (strLine.startsWith("ALTER")) {
							continue;
						} else if (strLine.startsWith("UNIQUE")) {
							continue;
						} else if (strLine.startsWith("CREATE SCHEMA")) {
							continue;
						} else if (strLine.equals(");")) {
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
