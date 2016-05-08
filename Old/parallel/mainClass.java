package parallel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class mainClass {
	
	public static void main ( String[] args )
	{
		if(args.length == 0) {
			System.out.println("No argument was entered: </directory/schema-file.sql/>");
			System.exit(1);
		} else if(args[0].length() < 4) {
			System.out.println("No argument was entered: </directory/schema-file.sql/>");
			System.exit(1);
		} 

		Path schemaFilePath = Paths.get(args[0]);
		ArrayList<Path> unlFileList = unlList(schemaFilePath);
		ArrayList<Thread> threadList = new ArrayList<Thread>();

		for(int i = 0; i < unlFileList.size(); i++) {
			DataObj dataObject = new DataObj(schemaFilePath, unlFileList.get(i));
			ThreadObj thrdObj = new ThreadObj(dataObject);
			Thread thrd = new Thread(thrdObj);
			threadList.add(thrd);
			thrd.start();
		}
		for(int i = 0; i < threadList.size(); i++) {
			try {
				threadList.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static ArrayList<Path> unlList(Path schemaFilePath) {
		ArrayList<Path> unlFiles = new ArrayList<Path>();
		try {
			Files.walk(schemaFilePath.getParent()).forEach(dataFilePath -> {
				if (Files.isRegularFile(dataFilePath)) {
					if(dataFilePath.toString().endsWith(".unl") && !dataFilePath.toString().endsWith("_ICDB.unl")) {
						unlFiles.add(dataFilePath);
					}
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return unlFiles;
	}
}
