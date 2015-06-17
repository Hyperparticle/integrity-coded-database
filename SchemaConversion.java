/**
 * 
 */


import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import schemaConversion.SchemaConverter;
import symbols.Symbol;

/**
 * @author Dan
 *
 */
public class SchemaConversion {

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
			SchemaConverter.convertSchemaFile(fileName, modifiedSchemaFile);
			SchemaConverter.createAttrKeyFile(fileName);

			System.out.println("Finished converting Schema File.");
		} else {
			System.err.println("Schema file doesn't exist.");
			System.exit(1);
		}
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
		while (token.hasMoreTokens()) {
			modifiedSchemaFileName.append(token.nextToken());
			modifiedSchemaFileName.append(Symbol.ICDB_FILE_EXTENSION);
			break;
		}

		return modifiedSchemaFileName.toString();
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

}
