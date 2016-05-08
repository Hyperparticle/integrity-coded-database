
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VerifyQueryModule
{
	private static double startTime, stopTime, numTests = 100;
	private static double avg = 0;


	private final static Logger log = Logger.getLogger(VerifyQueryModule.class.getName());

	private static String databaseName;
	private static String folderPath;
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://onyx.boisestate.edu:3333/";

	private static boolean DEBUG = false;

	private static int uniqueAttrs;

	private static ArrayList<String> primaryKeyList = new ArrayList<String>();

	public static String getFolderPath ()
	{
		return folderPath;
	}
	public static void setFolderPath (final String folderPath)
	{
		VerifyQueryModule.folderPath = folderPath;
	}
	public static String getDatabaseName ()
	{
		return databaseName;
	}
	public static void setDatabaseName (String databaseName)
	{
		VerifyQueryModule.databaseName = databaseName;
	}

	public static void main (String[] args)
	{
		String qFile = null;

		Scanner scan = new Scanner(System.in);
		System.out.println("Enter the folderPath where schema file exists");
		folderPath = "providedfiles/world/";//scan.nextLine();

		if (folderPath.length() > 0)
		{
			if(folderPath.endsWith("/")) {
				folderPath = folderPath.substring(0, folderPath.length() -1);
			}

			setFolderPath(folderPath);
			File fPath = new File(folderPath);
			if (fPath.exists())
			{
				System.out.println("Enter the sql query file to be converted");
				qFile = "providedfiles/ICDB_QueryExample.sql";//scan.nextLine();
				scan.close();

				if (qFile.length() > 0)
				{
					File queryFile = new File(qFile);
					if (queryFile.exists() && queryFile.isFile())
					{
						
						for(int i = 0; i < numTests; i++)
						{
							startTime = System.nanoTime();
							if (verifyICDBQuery(folderPath, queryFile))
							{
								System.out.println("ICDB signature verified");
							}
							else
							{
								System.out.println("ICDB signature not verified");
							}
							stopTime = System.nanoTime();
							avg += (stopTime-startTime)/1000000;
							System.out.println(i);
						}
						
					}
					else
					{
						System.out.println("Query file doesnt exist");
						System.exit(1);
					}
				}
			}
			else
			{
				System.out.println("Folder Path doesnt exist");
				System.exit(1);
			}
		}
		System.out.println("Average Time (ms): " + avg/numTests);
	}

	private static boolean verifyICDBQuery (String folderPath, File queryFile)
	{
		boolean verified = false;

		StringBuffer convertedQuery = readQueryFile(queryFile);
		Connection connect = null;
		PreparedStatement statement = null;

		primaryKeyList = populatePrimaryKeyList(folderPath, getDatabaseName());

		String query = convertedQuery.toString();
		String[] queryTkns = query.split(";");

		try
		{
			Class.forName(JDBC_DRIVER);
			connect = DriverManager.getConnection(DB_URL + getDatabaseName() + "?allowMultiQueries=true" + "&user=root&password=");

			if (connect != null)
			{
				System.out.println("Connected to database");

				for(int i = 1; i < queryTkns.length; i++) {
					if (queryTkns[i].toUpperCase().startsWith("SELECT"))
					{
						queryTkns[i] += ";";
						statement = connect.prepareStatement(queryTkns[i]);
						verified = queryVerification(statement, queryTkns[i]);
						if(!verified) {
							break;
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (statement != null)
			{
				try
				{
					statement.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}

			if (connect != null)
			{
				try
				{
					connect.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
		}
		return verified;
	}

	private static boolean queryVerification(PreparedStatement statement, String queryString) {

		boolean verified = false;
		ArrayList<String> tableNames = new ArrayList<String>();
		ArrayList<String> attrNames = new ArrayList<String>();
		ArrayList<String> attrValues = new ArrayList<String>();
		ArrayList<String> attr_SVC_Values = new ArrayList<String>();
		ArrayList<String> primaryKeyValue = new ArrayList<String>();
		ArrayList<String> attrSerialNumValues = new ArrayList<String>();
		ArrayList<String> reCalculatedValues = new ArrayList<String>();
		ArrayList<String> primaryKeyAttr = new ArrayList<String>();

		Map<String, String> primaryKeys = new HashMap<String, String>();
		ResultSet result = null;
		uniqueAttrs = 0;

		try {
			result = statement.executeQuery();
			while (result.next())
			{
				String[] attributes = queryString.split(Symbol.NEWLINE_DELIMITER);

				for (String lineAttr : attributes)
				{
					if (lineAttr.startsWith("SELECT"))
					{
						String[] indAttr = lineAttr.trim().split(Symbol.COMMA_DELIMITER_WITHOUT_SPACE);
						for(int i = 0; i < indAttr.length; i++) {

							String selectAttr = indAttr[i];
							boolean isKey = false;
							selectAttr = selectAttr.trim();

							if (selectAttr.startsWith("SELECT"))
							{
								String[] tmpAttr = selectAttr.split("\\s+");
								selectAttr = tmpAttr[1];
							}
							if (!selectAttr.endsWith(Symbol.SVC_SUFFIX))
							{
								isKey = isPrimaryKey(selectAttr);
								if (!isKey)
								{
									if(!attrNames.contains(selectAttr)) 
									{
										uniqueAttrs++;
									}
									attrNames.add(selectAttr);
									attrValues.add(result.getString(selectAttr));
								}
								else
								{
									if(i + 1 < indAttr.length) {
										if(indAttr[i + 1].trim().equalsIgnoreCase(selectAttr + Symbol.SVC_SUFFIX)) {
											if(!attrNames.contains(selectAttr)) 
											{
												uniqueAttrs++;
											}
											attrNames.add(selectAttr);
											attrValues.add(result.getString(selectAttr));
										}
									}
									if(!primaryKeyAttr.contains(selectAttr)) {
										primaryKeyAttr.add(selectAttr);
									}
									primaryKeyValue.add(selectAttr + Symbol.SLASH_DELIMITER + result.getString(selectAttr));
								}
							}
							else
							{
								String tmp = result.getString(selectAttr);
								if (tmp.contains(Symbol.SLASH_DELIMITER))
								{
									String[] tkns = tmp.split(Symbol.SLASH_DELIMITER);

									attr_SVC_Values.add(tkns[0]);
									attrSerialNumValues.add(tkns[1]);
								}
							}
						}
					}
					if (lineAttr.startsWith("FROM"))
					{
						String[] indAttr = lineAttr.trim().split("\\s+");

						for (String fromAttr : indAttr)
						{
							fromAttr = fromAttr.trim();

							if (fromAttr.equals("FROM") || fromAttr.equals("JOIN"))
							{
								continue;
							}
							else
							{
								if (fromAttr.endsWith(Symbol.COMMA_DELIMITER_WITHOUT_SPACE))
								{
									fromAttr = fromAttr.replace(",", "");
								}

								if(fromAttr.endsWith(";")) {
									fromAttr = fromAttr.substring(0, fromAttr.length()-1);
								}

								if (!tableNames.contains(fromAttr))
								{
									tableNames.add(fromAttr);
								}
							}
						}
					}
				}
			}

			primaryKeys = populateKeyValue(attrNames, primaryKeyValue, getDatabaseName(), getFolderPath(), tableNames);
			reCalculatedValues = verifyQuery(attrNames, attrValues, primaryKeys, attrSerialNumValues, getDatabaseName(), getFolderPath(), primaryKeyAttr.size());

			if (reCalculatedValues != null && !reCalculatedValues.isEmpty() && attr_SVC_Values != null && !attr_SVC_Values.isEmpty())
			{
				for (int i = 0, j = 0; j < attr_SVC_Values.size() && i < reCalculatedValues.size(); j++, i++)
				{
					if (reCalculatedValues.get(i).equals(attr_SVC_Values.get(j)))
					{
						verified = true;
					} 
					else {
						verified = false;
						break;
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return verified;
	}

	private static StringBuffer readQueryFile (File queryFile)
	{
		StringBuffer sb = new StringBuffer();
		FileInputStream fstream;

		try
		{
			fstream = new FileInputStream(queryFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			while ((strLine = br.readLine()) != null)
			{
				if(!strLine.startsWith("--") && !strLine.equals("")) {
					if (strLine.toUpperCase().startsWith("USE"))
					{
						getDatabaseNameFromQuery(strLine);
					}
					else if (strLine.equals(""))
					{
						sb.append(Symbol.NEWLINE_DELIMITER);
					}
					else if (strLine.toUpperCase().startsWith("FROM") || strLine.toUpperCase().startsWith("WHERE"))
					{
						sb.append(Symbol.NEWLINE_DELIMITER);
					}
					sb.append(strLine);
				}
			}
			br.close();
			in.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return sb;
	}

	private static void getDatabaseNameFromQuery (String line)
	{
		String databaseName = null;
		String[] lineTkns = line.split("\\s+");
		if (lineTkns[1].contains("`"))
		{
			lineTkns[1] = lineTkns[1].replace("`", "");
		}
		databaseName = lineTkns[1].trim().substring(0, lineTkns[1].length() - 1);
		setDatabaseName(databaseName);
	}

	private static ArrayList<String> populatePrimaryKeyList (String folderPath, String databaseName)
	{
		File primaryKeyFile = new File(folderPath + Symbol.SLASH_DELIMITER + databaseName.toLowerCase() + Symbol.SCHEMA_FILE_EXTENSION + Symbol.PRIMARY_KEY_FILE_EXTENSION);

		try
		{
			if (primaryKeyFile.exists())
			{
				FileInputStream fStream = new FileInputStream(primaryKeyFile);
				DataInputStream in = new DataInputStream(fStream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine = "";

				while ((strLine = br.readLine()) != null)
				{
					String[] tkns = strLine.split(":");
					if (tkns[2].contains(Symbol.SLASH_DELIMITER))
					{
						String[] tmpTkns = tkns[2].split(Symbol.SLASH_DELIMITER);
						for (String key : tmpTkns)
						{
							if (!primaryKeyList.contains(key))
							{
								primaryKeyList.add(key);
							}
						}
					}
					else
					{
						if (!primaryKeyList.contains(tkns[2]))
						{
							primaryKeyList.add(tkns[2]);
						}
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
		return primaryKeyList;
	}

	public static boolean isPrimaryKey (String selectAttr)
	{

		boolean isPrimaryKey = false;
		for (String key : primaryKeyList)
		{
			if (key.equalsIgnoreCase(selectAttr))
			{
				isPrimaryKey = true;
				break;
			}
		}
		return isPrimaryKey;
	}

	private static Map<String, String> populateKeyValue (ArrayList<String> attrNames, ArrayList<String> primaryKeyValue, String databaseName,
			String folderPath, ArrayList<String> tableNames)
			{
		Map<String, String> pKeys = new HashMap<String, String>();
		File pKFile = null;


		if (attrNames != null && attrNames.size() > 0 && primaryKeyValue != null && primaryKeyValue.size() > 0)
		{
			FileInputStream fstream;

			if (folderPath != null)
			{
				File fPath = new File(folderPath);

				if (fPath != null)
				{
					pKFile = new File(fPath + Symbol.SLASH_DELIMITER + databaseName.toLowerCase() + Symbol.SCHEMA_FILE_EXTENSION + Symbol.PRIMARY_KEY_FILE_EXTENSION);
					try
					{
						if (pKFile.exists())
						{
							for (int i = 0; i < attrNames.size(); i++)
							{
								String[] pTkns = null;

								String[] attrTkns = primaryKeyValue.get(i/uniqueAttrs).split(Symbol.SLASH_DELIMITER);
								String strLine = "";

								fstream = new FileInputStream(pKFile);
								DataInputStream in = new DataInputStream(fstream);
								BufferedReader br = new BufferedReader(new InputStreamReader(in));

								while ((strLine = br.readLine()) != null)
								{
									boolean flip = false;
									for(int m = 0; m < tableNames.size(); m++) {
										if(strLine.startsWith(tableNames.get(m))) {
											flip = true;
											break;
										}
									}
									if(!flip) {
										continue;
									}

									boolean singleKey = false;
									boolean multipleKey = false;

									String[] tkns = strLine.trim().split(":");
									String[] tmpTkns = tkns[1].split(Symbol.COMMA_DELIMITER_WITHOUT_SPACE);

									for (int j = 0; j < tmpTkns.length; j++)
									{
										if (attrNames.get(i).equalsIgnoreCase(tmpTkns[j]))
										{
											for (int j2 = 0; j2 < primaryKeyValue.size(); j2++)
											{
												pTkns = primaryKeyValue.get(j2).split(Symbol.SLASH_DELIMITER);
												if (!tkns[2].contains(Symbol.SLASH_DELIMITER) && tkns[2].equalsIgnoreCase(pTkns[0]))
												{
													singleKey = true;
													multipleKey = false;
													break;
												}
												else if (tkns[2].contains(pTkns[0]))
												{
													singleKey = false;
													multipleKey = true;
													break;
												}
											}
											break;
										}
									}

									if (singleKey)
									{
										if(pKeys.get(attrNames.get(i)) != null) {
											pKeys.put(attrNames.get(i), pKeys.get(attrNames.get(i)) + "/" + attrTkns[1]);
										} else {
											pKeys.put(attrNames.get(i), attrTkns[1]);
										}
										break;
									}
									else if(multipleKey)
									{
										String pKey = "";
										for (String key : primaryKeyValue)
										{
											String[] tmp = key.split(Symbol.SLASH_DELIMITER);
											if (pKey.length() == 0)
											{
												pKey = tmp[1];
											}
											else
											{
												pKey += Symbol.SLASH_DELIMITER + tmp[1];
											}
										}
										pKeys.put(attrNames.get(i), pKey);
									}
								}
								br.close();
								in.close();
								fstream.close();
							}
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return pKeys;
			}

	public static ArrayList<String> verifyQuery (ArrayList<String> attrNames, ArrayList<String> attrValues, Map<String, String> primaryKeysMap, ArrayList<String> attrSerialNumValues, String dbName, String folderPath, int numPrimaryKeys)
	{
		BigInteger message = null, encrypt = null;
		BigInteger privateKey = findRSAPrivateKey(folderPath, dbName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.RSA_KEY_FILE_EXTENSION);
		BigInteger modulus = findRSAModulus(folderPath, dbName + Symbol.SCHEMA_FILE_EXTENSION + Symbol.RSA_KEY_FILE_EXTENSION);
		ArrayList<String> reCalculatedValues = new ArrayList<String>();
		String[] tempList = null;
		String tempString = "";

		if (attrNames != null && attrNames.size() > 0 && attrValues != null && attrValues.size() > 0 && attrSerialNumValues != null
				&& attrSerialNumValues.size() > 0)
		{
			for (int k = 0, i = 0, j = 0, z = 0; k < attrSerialNumValues.size() && i < attrNames.size() && j < attrValues.size(); k++, i++, j++, z += numPrimaryKeys)
			{
				tempList = primaryKeysMap.get(attrNames.get(i)).split(Symbol.SLASH_DELIMITER);
				tempString = "";

				for(int x = 0; x < numPrimaryKeys; x++) {
					tempString += tempList[(z + x)/uniqueAttrs];
					if(numPrimaryKeys > 1 && (x+1) < numPrimaryKeys) {
						tempString += "/";
					}
				}

				message = null;

				if(tempList[i/uniqueAttrs] != "") {
					message = generateRSASignature(attrNames.get(i), tempString, attrValues.get(j), attrSerialNumValues.get(k), modulus);	
				}
				if (message != null)
				{
					if(DEBUG) {
						System.out.println(" verify query :: message :: " + message.toString());
					}
					encrypt = encrypt(message, privateKey, modulus);
					if(DEBUG) {
						System.out.println(" verify query :: encrypt : " + encrypt.toString());
						System.out.println(" verify query :: Encrypted message in hex:: " + encrypt.toString(16));
					}
					reCalculatedValues.add(encrypt.toString(16));
				}

			}
		}
		return reCalculatedValues;
	}

	public static BigInteger findRSAPrivateKey (final String folderPath, final String rsaKeyFile)
	{
		BigInteger privateKey = null;
		File rsaFile = new File(folderPath + Symbol.SLASH_DELIMITER + rsaKeyFile.toLowerCase());

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
					strLine = strLine.trim();
					String[] tkns = strLine.split(":");

					if (tkns[0].equals("privatekey"))
					{
						privateKey = new BigInteger(tkns[1]);
					}
				}

				fStream.close();
				in.close();
				br.close();
			}
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.INFO, "FileNotFoundException in findRSAPrivateKey() ");
		}
		catch (IOException e)
		{
			log.log(Level.INFO, "IOException in findRSAPrivateKey() ");
		}
		return privateKey;
	}

	public static BigInteger findRSAModulus (final String folderPath, final String rsaKeyFile)
	{
		// Read p and q key from rsaKeyFile

		BigInteger modulus = null;
		File rsaFile = new File(folderPath + Symbol.SLASH_DELIMITER + rsaKeyFile.toLowerCase());

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
					strLine = strLine.trim();
					String[] tkns = strLine.split(":");

					if (tkns[0].equals("modulus"))
					{
						modulus = new BigInteger(tkns[1]);
					}
				}

				fStream.close();
				in.close();
				br.close();
			}
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.INFO, "FileNotFoundException in findRSAModulus() ");
		}
		catch (IOException e)
		{
			log.log(Level.INFO, "IOException in findRSAModulus() ");
		}
		return modulus;
	}

	private static BigInteger generateRSASignature (String individualAttr, String primaryKey, String individualAttrVal, String serialNum,
			BigInteger modulus)
	{

		BigInteger message = null;
		BigInteger pkBigInt, attrValBigInt, attrBigInt;
		if(DEBUG) {
			System.out.println(" ******************************************* ");
			System.out.println(" verify query :: attr name :: " + individualAttr);
			System.out.println(" verify query :: pk name :: " + primaryKey);
		}
		// Check if its a single primary key or combination of keys

		if (!primaryKey.contains(Symbol.SLASH_DELIMITER))
		{
			// Check if primary key is a number or not
			if (!isIntegerRegex(primaryKey))
			{
				pkBigInt = convertToInt(primaryKey);
			}
			else
			{
				pkBigInt = new BigInteger(primaryKey);
			}
		}
		else
		{
			pkBigInt = findPrimaryKeyProduct(primaryKey);
		}
		if(DEBUG) {
			System.out.println("verify query :: pkBigInt :: " + pkBigInt.toString());
		}
		// Convert attribute name token to integer
		attrBigInt = convertToInt(individualAttr);
		if(DEBUG) {
			System.out.println("verify query :: attrBigInt :: " + attrBigInt.toString());
		}
		// Check if data file token is a number or not
		if (!isIntegerRegex(individualAttrVal))
		{
			attrValBigInt = convertToInt(individualAttrVal);
		}
		else
		{
			attrValBigInt = new BigInteger(individualAttrVal);
		}
		if(DEBUG) {
			System.out.println("verify query :: tknBigInt :: " + attrValBigInt.toString());
			System.out.println(" verify query :: serial Number : " + serialNum);
		}

		message = pkBigInt.multiply(attrValBigInt).multiply(attrBigInt).multiply(new BigInteger(serialNum));
		message = message.mod(modulus);

		return message;

	}

	public static BigInteger convertToInt (String input)
	{
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

	public static boolean isIntegerRegex (String str)
	{
		return str.matches("^[0-9]+$");
	}

	public static BigInteger encrypt (BigInteger message, BigInteger privateKey, BigInteger modulus)
	{
		return message.modPow(privateKey, modulus);
	}

	public static BigInteger findPrimaryKeyProduct (String primaryKeys)
	{
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

}
