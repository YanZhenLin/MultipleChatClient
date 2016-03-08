package application;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ChatServer extends Application implements ChatConfig{
	
	private int sessionNo = 0;
	private List<Session> sessions;
	
	private TextArea taLog;
	public static void main(String[] args){
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage){
		taLog = new TextArea();
		Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
		primaryStage.setTitle("Chat Server"); // Set the stage title 
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show();
		sessions = Collections.synchronizedList(new ArrayList<Session>()); //initialize the synchronized hashMap after the stage setup
		startServer();
	}
	
	private void startServer(){
		new Thread(() -> { //lambda expression
	 		try{
	 			//create a server socket listening on port 
	 			ServerSocket serverSocket = new ServerSocket(PORT);
	 			Platform.runLater(()->taLog.appendText(new Date()+ ": Server started at socket "+PORT+"\n"));
	 			//initialize the linkedList 
	 			//the server sockets are now created, we will now need to open it to create client sockets
	 			while(true){
	 				Socket clientSocket = serverSocket.accept(); //accept a connection request here from the client 
	 				Platform.runLater(()->taLog.appendText(new Date()+ ": new client connected from address: "+clientSocket.getInetAddress().getHostAddress()+"\n"));
	 				new Thread(new ClientHandle(clientSocket)).start(); //start a new ClientHandle task
	 			}
			}catch(IOException ex){
				ex.printStackTrace();
			}
		}).start();
	}

	//once we we a request from a user connection, we need to start a new thread for that user, a lock will need to be put on the users.txt file
	class ClientHandle implements Runnable, ChatConfig{
		Socket clientSocket; //we can get the InetAddress from this socket if we wanted
		User user;
		ObjectInputStream inputStream;
		ObjectOutputStream outBox;
		
		public ClientHandle(Socket clientSocket){
			this.clientSocket = clientSocket;
			/*we will define the Object inputStream here, but it won't take any inputStream until 
			 * after the start method call in the Thread initialization*/
			try { //now we are stuck here, because the client side has not initialize their outputStream, seems like we have a deadlock
				inputStream = new ObjectInputStream(clientSocket.getInputStream()); //create an inputstream first, which expects that the client side has created an outputStream
				outBox = new ObjectOutputStream(clientSocket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			boolean successfulEntry = false; //initially not registered or logged in
			String username = "";
			while(!successfulEntry){ //while we are in the login/register user state
				//while the task is running, and we do not have successful entry access yet by the user
				//this object will be an authentication login/registration request
				InitialRequest requestType = null;
				try {
					requestType = (InitialRequest)inputStream.readObject();
					
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
				int temp = requestType.getRequest();
				Platform.runLater(()->taLog.appendText(new Date()+ "... request type : "+temp+" \n"));
				
				//non of this will run if an exception occurs
				String message = "";
				int statusCode = FAILED;
				
				//we need to update invalidate the statusCode and the successfulEntry variable on successful entry
				ChatUsers.setTextArea(taLog);
				if(  requestType.getRequest() == LOGIN ){
					//check the authentication
					username = requestType.getUserName();
					String pass = requestType.getPass();
					String profile = ChatUsers.getUserFromFile(username);
					String[] profileTuple= profile.split(",");
					if(profile == ""){ //if the profile is empty, then the login attempt failed
						successfulEntry = false;
						message = "could not find the user in system";
						Platform.runLater(()->taLog.appendText(new Date()+ " .... login attempt failed, no such user \n"));
					}else{ //if the profile is not empty,
						
						String tpass = requestType.getPass();
						Platform.runLater(()->taLog.appendText(new Date()+ " .... password attempt:"+tpass+" \n"));
						
						if( profileTuple[1].equals(requestType.getPass()) ){ //password match
							successfulEntry = true;
							message = "logged in successfully";
							statusCode = OK;
							Platform.runLater(()->taLog.appendText(new Date()+ " .... login success \n"));
						}else{ //incorrect password
							message = "incorrect password";
						}
					}
				}else if( requestType.getRequest() == REGISTER ){ //request type is 
					username = requestType.getUserName();
					//need to check if the username trying to add is available
					
					String getUserResponse = ChatUsers.getUserFromFile(username);
					if( getUserResponse != "" ){ //user already exists
						message = "The Username is already taken";
					}else{
						String pass = requestType.getPass();
						if( pass.length() >= 6 ){
							successfulEntry = true;
							message = "registration success";
							statusCode = OK; 
							//create the user in our system
							user = new User(requestType.getUserName(), requestType.getPass());
							ChatUsers.setUser(user); //writes into the uses.txt file, this has to be synchronized
							Platform.runLater(()->taLog.appendText(new Date()+ " .... registration success \n"));
						}else{ //password fail
							message = "password needs to be 6 characters long";
						}
					}
				}
				//notify the client side that the server is ready for them to chat
				//instantiate a new initial response object here with message and status code, send via a ObjectOutputStream 
				Platform.runLater(()->taLog.appendText(new Date()+ " .... generating initial response object \n"));
				InitialResponse newResponse = new InitialResponse(requestType.getRequest(), statusCode, message);
				
				try {
					outBox.writeObject(newResponse);
				} catch (IOException e) {
					e.printStackTrace();
				}
				//we need to add it to the hashMap
				 //we only create a new entry in the hashMap when we have a successful login or registration
				boolean tempEntry = successfulEntry;
				Platform.runLater(()->taLog.appendText(new Date()+ " .... session added , successfulEntry value = "+tempEntry+"\n"));
			}
			//end of while loop, if we get out of the while loop that means the client either exits or is now able to start chatting
			sessions.add(new Session(username, clientSocket, inputStream, outBox)); //only add the session after successful entry
			
			while(successfulEntry){ //while the user is in successful entry mode, listen for the next request, either a message request or a exit request
				
				Request request = null;
				try {
					request = (Request)inputStream.readObject();
					Platform.runLater(()->taLog.appendText(new Date()+ " .... new input request recieved \n"));
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
				
				if(request != null ){ //if the request is properly set
					if( request.getRequestType() == LOGOUT ){
						//we will handle this later
					}else if( request.getRequestType() == NEW_MESSAGE){
						// for now let's just worry about out the new message request
						
						String fromUser = request.getFromName(); //this is the only one we technically need
						String Message = request.getMessage();
						
						//broadcast to all the sessions that are online, only users inside the sessions 
						Platform.runLater(()->taLog.appendText(new Date()+ " .... number of sessions:"+sessions.size()+"\n"));
						synchronized(sessions) {
							for(Session session: sessions){ //this is an iterator traversal, hence we need to synchronize it before hand
								if(session.getUsername() != username){
									try {
										Platform.runLater(()->taLog.appendText(new Date()+ " .... trying to broadcast to "+session.getUsername()+" \n"));
										session.getOutBox().writeObject(request);
										Platform.runLater(()->taLog.appendText(new Date()+ " .... finish broadcasting to "+session.getUsername()+" \n"));
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
				}//we ignore if the request is null, guess it will suffice
			} //end of while loop
		}//end of run method
	}//end of inner class definition
}
