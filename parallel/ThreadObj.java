package parallel;

public class ThreadObj implements Runnable{

	private DataObj dataObject;
	
	public ThreadObj(DataObj dataObject) {
		this.dataObject = dataObject;
	}
	
	public void run() {
		DataConversion temp = new DataConversion();
		temp.execute(dataObject);
	}
}
