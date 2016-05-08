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
import java.util.ArrayList;
import java.util.Random;

import AES.helper.Symbol;

public class ConvertQuery {

	private static String publicKey, privateKey, modulus;
	private static long serialNum = 0;

	public static void main(String[] args) {

		File schemaFile;
		File queryFile;
		
		if (args.length == 2) {
			schemaFile = new File(args[0]);
			queryFile = new File(args[1]);
		} else {
			schemaFile = new File("C:/Users/Dan/Downloads/queries/world-schema.sql");
			queryFile = new File("C:/Users/Dan/Downloads/queries/select.sql");
		}
		
		ArrayList<String> queryList = new ArrayList<String>();

//		double startTime, stopTime, numTests = 10;
//		for(int z = 0; z < numTests; z++) {
//			startTime = System.nanoTime();
			if(schemaFile.exists() && queryFile.exists()) {
				queryList = convertQuerys(schemaFile, queryFile);
			}
//			
//			stopTime = System.nanoTime();
//			System.out.println((stopTime-startTime)/1000000);
//		}
		
		System.out.println(queryList.get(0));
	}

	private static ArrayList<String> convertQuerys(File schemaFile, File queryFile) {

		ArrayList<String> queryList = readQueryFile(queryFile);
		ArrayList<String> modQueryList = new ArrayList<String>();
		String modQuery = null;;
		String query = null;;

		for(int i = 0; i < queryList.size(); i++) 
		{
			modQuery = null;
			query = queryList.get(i).toUpperCase();

			if(query.startsWith("SELECT")) 
			{
				if(query.startsWith("SELECT " + Symbol.STAR)) {
					modQuery = query;
				} 
				else 
				{
					modQuery = convSelectQuery(query, schemaFile);
				}
			}
			else if(query.startsWith("INSERT"))
			{
				modQuery = convInsertQuery(query, schemaFile);
			}
			else if(query.startsWith("DELETE"))
			{
				modQuery = convDeleteQuery(query, schemaFile);
			}
			else if(query.startsWith("UPDATE"))
			{
				modQuery = convUpdateQuery(query, schemaFile);
			} else
			{
				modQuery = query;
			}
			modQueryList.add(modQuery);
		}
		return modQueryList;
	}

	private static ArrayList<String> readQueryFile(File queryFile) {

		ArrayList<String> queryList = new ArrayList<String>();
		try 
		{
			FileInputStream fileStream = new FileInputStream(queryFile);
			DataInputStream in = new DataInputStream(fileStream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			StringBuffer curQuery = new StringBuffer();
			String strLine;

			while((strLine = br.readLine()) != null) 
			{				
				curQuery.append(strLine);
				if(!strLine.endsWith(" ")) {
					curQuery.append(" ");
				}

				if(strLine.endsWith(";")) {
					queryList.add(curQuery.toString().trim());
					curQuery.delete(0, curQuery.length());
				}
			}
			br.close();
			in.close();
			fileStream.close();
		} 
		catch(FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return queryList;
	}

	private static String retrieveTableNm(String queryType, String query) {

		String dataTable = null;

		if(queryType == "Select" || queryType == "Delete") {
			String[] tokenList = query.split("\\s+");

			for(int i = 0; i < tokenList.length; i++) {
				if(tokenList[i].equals("FROM")) {
					dataTable = tokenList[i + 1].replace(";", "");
					break;
				}
			}
		} else if(queryType == "Insert") {
			String[] tokenList = query.split("\\s+");
			dataTable = tokenList[2].replace("`", "");
		}
		return dataTable;
	}

	private static String convSelectQuery(String query, File schemaFile) {

		String modQuery = "";
		String dataTable = retrieveTableNm("Select", query);
		String[] keyList = primaryKeyList(dataTable, schemaFile).split("/");
		String[] tokenList = query.replace(",", "").split("\\s+");

		boolean attr = false;

		for(int i = 0; i< tokenList.length; i++) 
		{
			if(tokenList[i].equals("SELECT") || tokenList[i].equals("DISTINCT")) 
			{
				attr = true;
				modQuery += tokenList[i] + " ";
			}
			else if(tokenList[i].equals("FROM")) 
			{
				attr = false;
				for(int k = 0; k < keyList.length; k++) {
					if(!keyList[k].equals("")) {
						modQuery += keyList[k] + ", ";
					}
				}
				modQuery = modQuery.substring(0, modQuery.length() - 2) + " ";
				modQuery += Symbol.NEWLINE_DELIMITER + tokenList[i] + " ";
			}
			else if(attr)
			{
				modQuery += tokenList[i] + ", ";
				modQuery += tokenList[i] + "_SVC, ";
				for(int j = 0; j < keyList.length; j++) {
					if(tokenList[i].equals(keyList[j])) {
						keyList[j] = "";
					}
				}
			} 
			else 
			{
				modQuery += tokenList[i] + " ";
			}
		}
		return modQuery;
	}

	private static String convInsertQuery(String query, File schemaFile) {

		StringBuffer modQuery = new StringBuffer();
		String dataTable = retrieveTableNm("Insert", query);
		String primaryKey = primaryKeyList(dataTable, schemaFile);
		String[] tokenList = query.split("\\(|\\)");
		String[] attrList = tokenList[1].split(",");
		String[] attrValList = tokenList[3].replace("'", "").split(",");

		readRSAKeyFile(schemaFile);
		if(serialNum == 0) {
			readCurrentSerialNum(schemaFile);
		}
		modQuery.append(tokenList[0] + "(");

		for(int i = 0; i < attrList.length; i++) 
		{
			modQuery.append(attrList[i].trim() + ", ");
			modQuery.append(attrList[i].trim() + "_SVC, ");
		}
		modQuery.delete(modQuery.length() - 2, modQuery.length());

		modQuery.append(")" + tokenList[2] + "(");

		for (int i = 0; i < attrList.length; i++) 
		{
			modQuery.append("'" + attrValList[i].trim() + "', ");
			BigInteger message = generateRSACodes(attrList[i], attrValList[i], primaryKey);
			BigInteger encrypt = encrypt(message);
			modQuery.append("'" + encrypt.toString() + "/" + serialNum + "', ");
			serialNum++;
		}
		modQuery.delete(modQuery.length() - 2,  modQuery.length());

//		modQuery.append(tokenList[4]);
		modQuery.append(");");

		saveCurrentSerialNum(schemaFile);

		return modQuery.toString();
	}

	private static String convDeleteQuery(String query, File schemaFile) {

		String modQuery = null;
		ArrayList<String> attrSerialVal = new ArrayList<String>();

		saveRevokedSerialNum(schemaFile, attrSerialVal);

		return modQuery;
	}

	private static String convUpdateQuery(String query, File schemaFile) {
		String modQuery = null;

		return modQuery;
	}

	private static void readCurrentSerialNum (File schemaFile)
	{
		FileInputStream fstream;
		File icrlFile = new File(schemaFile.toString().replace("-schema.sql", "_icrlFile.txt"));
		long lastValidSNum = 0;

		try
		{
			if (icrlFile.exists())
			{
				fstream = new FileInputStream(icrlFile);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;

				while ((strLine = br.readLine()) != null)
				{
					strLine = strLine.trim();
					if (strLine.contains("Current Valid Serial Number"))
					{
						String[] tkns = strLine.split(":");
						lastValidSNum = Long.parseLong(tkns[1].trim());
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
				lastValidSNum = new Integer(prng.nextInt(Integer.MAX_VALUE));
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		serialNum = lastValidSNum;
	}

	private static void saveRevokedSerialNum (File schemaFile, ArrayList<String> attrSerialValues)
	{
		boolean found = false;
		File icrlFile = new File(schemaFile.toString().replace(".sql", "_icrlFile.txt"));

		try
		{
			if (icrlFile != null && icrlFile.exists())
			{
				FileInputStream fstream = new FileInputStream(icrlFile);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine = "";
				ArrayList<String> sNumToFile = new ArrayList<String>();
				boolean isRevoked = false;

				while ((strLine = br.readLine()) != null)
				{
					strLine = strLine.trim();

					if (strLine.startsWith("Revoked Serial Numbers:"))
					{
						isRevoked = true;
						String[] tmp = strLine.split(Symbol.COLON);
						if (tmp[1].contains(Symbol.COMMA_DELIMITER_WITHOUT_SPACE))
						{
							String[] sNumTknsFromFile = tmp[1].trim().split(Symbol.COMMA_DELIMITER_WITHOUT_SPACE);
							for (String serialNumber : attrSerialValues)
							{
								serialNumber = serialNumber.trim();

								for (String sNums : sNumTknsFromFile)
								{
									sNums = sNums.trim();
									if (sNums.equals(serialNumber))
									{
										found = true;
										break;
									}
								}

								if (!found && !sNumToFile.contains(serialNumber))
								{
									sNumToFile.add(serialNumber);
								}
							}
						}
					}
				}

				FileWriter fileWriter = new FileWriter(icrlFile, true);
				BufferedWriter bufferWritter = new BufferedWriter(fileWriter);

				if (!sNumToFile.isEmpty())
				{
					if (!isRevoked)
					{
						bufferWritter.append(Symbol.NEWLINE_DELIMITER);
						bufferWritter.append("Revoked Serial Numbers: ");
					}
					for (String serialNumber : sNumToFile)
					{
						bufferWritter.append(serialNumber);
						bufferWritter.append(Symbol.COMMA_DELIMITER_WITHOUT_SPACE);
					}

				}
				bufferWritter.close();
				fileWriter.close();
				br.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private static void saveCurrentSerialNum (File schemaFile)
	{
		FileInputStream fstream;
		StringBuffer sb = new StringBuffer();
		StringBuffer rStrBuff = new StringBuffer();
		File icrlFile = new File(schemaFile.toString().replace("-schema.sql", "_icrlFile.txt"));

		try
		{
			if (icrlFile != null && icrlFile.exists())
			{
				fstream = new FileInputStream(icrlFile);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				String strLine;
				Writer icrlFileOutput = null;
				boolean hasLastValidSerialNum = false;

				while ((strLine = br.readLine()) != null)
				{
					strLine = strLine.trim();
					if (strLine.startsWith("First Valid Serial Number"))
					{
						sb.append(strLine);
						sb.append("\n");
					}
					else if (strLine.startsWith("Current Valid Serial Number"))
					{
						hasLastValidSerialNum = true;
						String[] tkns = strLine.split(":");
						sb.append(tkns[0] + ":");
					}
					else
					{
						rStrBuff.append(strLine);
					}

				}
				if (hasLastValidSerialNum)
				{
					icrlFile.delete();
					icrlFile.createNewFile();
					icrlFileOutput = new BufferedWriter(new FileWriter(icrlFile));
					icrlFileOutput.write(sb.toString());
					String lastValidSNum = Long.toString(serialNum);
					icrlFileOutput.write(lastValidSNum);
					icrlFileOutput.write(Symbol.NEWLINE_DELIMITER);
					icrlFileOutput.write(rStrBuff.toString());
					icrlFileOutput.close();
				}
				else 
				{
					String lastValidSNum = Long.toString(serialNum);
					icrlFileOutput = new BufferedWriter(new FileWriter(icrlFile, true));
					icrlFileOutput.write(Symbol.NEWLINE_DELIMITER);
					icrlFileOutput.write("Current Valid Serial Number" + ":" + lastValidSNum);
					icrlFileOutput.close();
				}
				br.close();
				in.close();
				fstream.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
//		System.out.println(" Last serial number :: " + serialNum);

	}




	private static String primaryKeyList(String dataTable, File schemaFile) {

		String primaryKeyList = null;
		String pkFile = schemaFile.toString().replace(".sql", "_pk.txt");
		String[] pkList;
		try 
		{
			FileInputStream fileStream = new FileInputStream(pkFile);
			DataInputStream in = new DataInputStream(fileStream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			while((strLine = br.readLine()) != null) 
			{
				strLine = strLine.replace("`", "");
				if(strLine.startsWith(dataTable)) {
					pkList = strLine.replace("`", "").split(":");
					primaryKeyList = pkList[2];
					break;
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return primaryKeyList;
	}

	private static BigInteger generateRSACodes (String attr, String attrVal, String primaryKey)
	{
		BigInteger message = null;
		BigInteger pkInt, attrValInt, attrInt;

		if (primaryKey.contains(Symbol.SLASH_DELIMITER))
		{
			pkInt = findPrimaryKeyProduct(primaryKey);
		}
		else
		{
			if (isIntegerRegex(primaryKey))
			{
				pkInt = new BigInteger(primaryKey);
			}
			else
			{
				pkInt = convertToInt(primaryKey);
			}
		}

		attrInt = convertToInt(attr);

		if (isIntegerRegex(attrVal))
		{
			attrValInt = new BigInteger(attrVal);
		}
		else
		{
			attrValInt = convertToInt(attrVal);
		}

		message = pkInt.multiply(attrValInt).multiply(attrInt).multiply(new BigInteger(Long.toString(serialNum)));
		message = message.mod(new BigInteger(modulus));

		return message;
	}

	public static boolean isIntegerRegex (String str) {
		return str.matches("^[0-9]+$");
	}

	public static BigInteger convertToInt (String input) {
		StringBuilder sBuilder = new StringBuilder();
		if (!input.equals(""))
		{
			for (char c : input.toCharArray())
			{
				sBuilder.append((int) c);
			}
			return new BigInteger(sBuilder.toString());
		}
		return null;
	}

	public static BigInteger findPrimaryKeyProduct (String primaryKeys) {
		double pkInt = 0;
		BigInteger product = null;

		String[] primaryKeyTokens = primaryKeys.split(Symbol.SLASH_DELIMITER);

		for (int i = 0; i < primaryKeyTokens.length; i++)
		{
			if (isIntegerRegex(primaryKeyTokens[i]))
			{
				if (pkInt == 0)
				{
					pkInt = Long.parseLong(primaryKeyTokens[i]);
					product = new BigInteger(primaryKeyTokens[i]);
				}
				else
				{
					product = product.multiply(new BigInteger(primaryKeyTokens[i]));
				}
			}
			else
			{
				if (pkInt == 0)
				{
					pkInt = convertToInt(primaryKeyTokens[i]).intValue();
					product = convertToInt(primaryKeyTokens[i]);
				}
				else
				{
					product = product.multiply(convertToInt(primaryKeyTokens[i]));
				}
			}
		}
		return product;
	}

	public static void readRSAKeyFile (File schemaFile)
	{
		File rsaFile = new File(schemaFile.toString().replace(".sql", "_rsa.txt"));

		try
		{
			if (rsaFile.exists())
			{
				FileInputStream fStream = new FileInputStream(rsaFile);
				DataInputStream in = new DataInputStream(fStream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine = "";

				while ((strLine = br.readLine()) != null)
				{
					String[] tkns = strLine.trim().split(":");
					if (tkns[0].equals("modulus"))
					{
						modulus = tkns[1];
					} 
					else if(tkns[0].equals("privatekey")) 
					{
						privateKey = tkns[1];
					}
					else if(tkns[0].equals("publickey")) 
					{
						publicKey = tkns[1];
					}
				}
				fStream.close();
				in.close();
				br.close();
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static BigInteger encrypt (BigInteger message)
	{
		return message.modPow(new BigInteger(privateKey), new BigInteger(modulus));
	}

}

