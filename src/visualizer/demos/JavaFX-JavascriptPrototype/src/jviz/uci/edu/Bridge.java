package jviz.uci.edu;

public class Bridge {

	public Bridge() {
		System.out.println("Creating bridge...");
		// TODO Auto-generated constructor stub
	}
	
	public void print(String str){
		//new Thread(new Runnable() {
			//public void run() {
				System.out.println(str);
		//	}
		//	}).start();
	}
	
	public void initLayout() {
		Main.jsLayout.initLayout();
	}

}
