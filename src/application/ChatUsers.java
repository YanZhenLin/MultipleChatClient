package application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class ChatUsers {
	
	private static final String extension = "users"+File.separator+"user.txt";
	private static final String work_directory = System.getProperty("user.dir")+File.separator;
	private static Semaphore semaphore = new Semaphore(1); //mutually exclusive lock
	private static TextArea taLog;
	
	public static void setTextArea( TextArea log){
		taLog = log;
	}
	
	private static File getUserFile() throws FileNotFoundException{
		
		File file = new File(work_directory+extension);
		if(!file.exists()){
			throw new FileNotFoundException(work_directory+extension+"File now found");
		}else
			return file;
	}
	
	//inside 
	public static String getUserFromFile(String userName){
		
		String ret = "";
		//open the /users/users.txt file, each line in that file represent an user
		File file = null;
		try {
			Platform.runLater(()->taLog.appendText(new Date()+ " .... trying to acquire semaphore \n"));
			semaphore.acquire();
			Platform.runLater(()->taLog.appendText(new Date()+ " .... semaphore acquired \n"));
			try {
				Platform.runLater(()->taLog.appendText(new Date()+ " .... trying to get file \n"));
				file = getUserFile();
				Platform.runLater(()->taLog.appendText(new Date()+ " .... file retrieved \n"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			if(file.exists()){
				Platform.runLater(()->taLog.appendText(new Date()+ " .... user file found \n"));
				try{
					Scanner input = new Scanner(file);		
					String nextLine = "";
					while(input.hasNextLine()){
						nextLine = input.nextLine();
						if(nextLine.contains(userName)){
							ret = nextLine;
							break; //break out of the loop, we've already set the return
						}
					} //if we don't find the correct match, we return an empty string
					input.close();
				}catch(FileNotFoundException ex){
					ex.printStackTrace();
				}
			}else{
				Platform.runLater(()->taLog.appendText(new Date()+ " .... file does not exist \n"));
			}
			
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}finally{
			semaphore.release();
		}
		return ret;
	}

	
	//create a new record inside the /users/users.text file
	public static boolean setUser( User user ){
		boolean ret = false;
		try {
			semaphore.acquire(); //acquire the lock on the resource
			File file = null;
			
			try { //get the file
				file = getUserFile();
			} catch (IOException e) {
					e.printStackTrace();
			}
		
			if(file.exists()){
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
				out.write(user.getUsername()+","+user.getPassword()+"\n");
				out.close();
				ret = true;
			}
		}catch (IOException e) {
			e.printStackTrace();
		}catch(InterruptedException e1){
			e1.printStackTrace();
		}finally{
			semaphore.release(); //release the resource no matter what
		}
		return ret;
	}
	
}
