import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import dataConversion.KeyGenerator;


public class DataConversion {

	public static String databaseName = null;
	
	public static void main ( String[] args )
	{
		convert(args);
	}
	
	public static void convert(String[] args)
	{
		String inputDataFile = null;
		String schemaFileName = null;

		if (args.length != 2)
		{
			Scanner scan = new Scanner( System.in );
			System.out.println( "Enter the schema file name: </folderpath/schemaFileName>" );
			schemaFileName = scan.nextLine();
			System.out.println( "Enter the data file to be converted </folderpath/dataFileName>" );
			inputDataFile = scan.next();
		}
		else
		{
			schemaFileName = args[0];
			inputDataFile = args[1];
		}
		
		if ( inputDataFile.length() < 2 )
		{
			System.out.println( "Enter the schema file name: </folderpath/schemaFileName>" );
			System.out.println( "Enter the data file to be converted </folderpath/dataFileName>" );
			System.exit( 1 );
		}
		else
		{
			Path schemaFile = Paths.get( schemaFileName );
			String[] tmp = schemaFile.getFileName().toString().split( "-" );
			databaseName = tmp[0];

			File dataFile = new File( inputDataFile );

			if ( dataFile.isFile() && dataFile.exists() )
			{
				/* Generate ICRL serial number */
				KeyGenerator.generateSerialNum( databaseName, schemaFile );
				System.out.println( " Serial Number generated.." + KeyGenerator.getSerialNumber() );

				/* Generate RSA keys */
				generateRSASignature( bitSize, databaseName, schemaFile );
				System.out.println( "RSA keys generated.. " );

				// Find the Primary Key
				primaryKeyList = findPrimaryKey( schemaFileName, inputDataFile );
				findPrimaryKeyPosition( primaryKeyList, new File( schemaFileName ) );

				convertDataFile( databaseName, dataFile );
				System.out.println( " Final Serial Number :: " + getSerialNumber() );

				saveLastValidSerialNumber( dataFile.getParent() );
			}
			else
			{
				System.out.println( "DataFile doesn't exist" );
				System.exit( 1 );
				
			}

		}
	}

}
