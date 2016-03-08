package application;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Session {

	private String username;
	private Socket socket;
	private ObjectInputStream inBox;
	private ObjectOutputStream outBox;
	
	public Session(Socket socket){
		this.socket = socket;
	}
	
	public Session(String username, Socket socket, ObjectInputStream input, ObjectOutputStream output){
		this.username = username;
		this.socket = socket;
		this.inBox = input;
		this.outBox = output;
	}
	
	public String getUsername(){
		return username;
	}
	
	public void SetObjectInputStream(ObjectInputStream input){
		inBox = input;
	}
	
	public void setObjectOutputStream(ObjectOutputStream output){
		outBox = output;
	}
	
	public ObjectInputStream getInBox(){
		return inBox;
	}
	
	public ObjectOutputStream getOutBox(){
		return outBox;
	}
}
