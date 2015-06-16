import java.io.File;

public class ConversionModule {

	public static void main(String[] args) {
		if (args.length == 1)
		{
			SchemaConversionModule.execute(new String[] { args[0] });
			
			File f = new File(args[0]);
			
			File folder = new File(f.getParent());
			File[] listOfFiles = folder.listFiles();

			for (File file : listOfFiles) {
			    if (file.isFile() && file.exists()) {
			        DataConversionModule.execute(new String[] {
			        		args[0],
			        		file.getAbsolutePath()
			        });
			    }
			}
		}
	}

}
