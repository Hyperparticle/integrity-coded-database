package schemaConversion;

import java.util.Scanner;
import java.util.StringTokenizer;

import symbols.Symbol;

public class CodeGenerator {

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
}
