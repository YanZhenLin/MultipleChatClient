package application;

import java.io.*;
import java.net.*;
import java.util.Date;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage; 
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Label;


public class ChatClient extends Application implements ChatConfig{

	private String currentUsername = "";
	
	private Socket socket; //this is the the client's request socket
	private TextField usernameField;
	private TextField passwordField;
	private ChatRegisterButton registerButton;
	private ChatLoginButton loginButton;
	private StackPane masterContainer;
	private BorderPane registrationContainer;
	private BorderPane chatContainer;
	private TextArea newMessageEditor;
	private TextArea pastMessageViewer;
	private Label ChatHeaderLabel;
	
	private ObjectInputStream fromServer;
	private ObjectOutputStream toServer;
	
	//logged in or not status
	private boolean NotLoggedIn = true; //initially not logged in
	private boolean Online = false; //initially not online, this is the opposite of the NotloggedIn status
	@Override
	public void start(Stage primaryStage){
		
		//the initial FX initializers
		usernameField = new TextField("Username");
		passwordField = new TextField("Password");
		usernameField.setFont(Font.font("Times", 12));
		usernameField.setPrefWidth(30);
		passwordField.setFont(Font.font("Times", 12));
		passwordField.setPrefWidth(30);
		
		registerButton = new ChatRegisterButton();
		loginButton = new ChatLoginButton();
		//not working
		//loginButton.setOnAction(new Login()); // set the action handler for the login button
		
		masterContainer = new StackPane(); 
		
		registrationContainer = new BorderPane();
		registrationContainer.setPadding(new Insets(20, 20, 20, 20));
		
		VBox textfieldContainer = new VBox();
		HBox buttonContainer = new HBox();
		textfieldContainer.getChildren().addAll(usernameField, passwordField);
		buttonContainer.getChildren().addAll(registerButton, loginButton);
		
		registrationContainer.setCenter(textfieldContainer);
		registrationContainer.setBottom(buttonContainer);
		
		//the master container will have the registrationContainer display first
		masterContainer.getChildren().add(registrationContainer); 
		/*we will need to have the master container remove the registrationContainer 
		 * and add this one when we are either logged in or successfully registered*/
		
		chatContainer = new BorderPane(); 
		//chatContainer.setPadding(new Insets(20, 20, 20, 20));
		
		ChatHeaderLabel = new Label();
		ChatHeaderLabel.setPadding(new Insets(10, 10, 10, 10));
		
		//we need to give this a key action event
		newMessageEditor = new TextArea();
		newMessageEditor.setPrefColumnCount(200); 
		newMessageEditor.setPrefRowCount(15);
		newMessageEditor.setWrapText(true);
		newMessageEditor.setFont(Font.font("Times", 20));
		newMessageEditor.setPadding(new Insets(10, 10, 10, 10));
		newMessageEditor.setOnKeyPressed(new MessageEditorKeyPressHandler()); //this is the only listener we need at the moment
		/* we need to define the action here, when the enter key is press, we need to update the pastMessageViewer */
		
		pastMessageViewer = new TextArea();
		pastMessageViewer.setEditable(false);
		pastMessageViewer.setPrefColumnCount(200); 
		pastMessageViewer.setPrefRowCount(200);
		pastMessageViewer.setWrapText(true);
		pastMessageViewer.setPadding(new Insets(10, 10, 10, 10));
		pastMessageViewer.setFont(Font.font("Times", 20));
		//
		
		//this does not need to be a member pane
		//ScrollPane scrollPane = new ScrollPane(pastMessageViewer);
		
		//build the layout containers
		chatContainer.setTop(ChatHeaderLabel); //we will need to update the greeting label upon successful entry
		chatContainer.setCenter(pastMessageViewer);
		chatContainer.setBottom(newMessageEditor);
		//once the registration or the login is successful, we need to remove the entry layout and put 
		//masterContainer.getChildren().remove(registrationContainer);
		
		Scene scene = new Scene(masterContainer, 300, 300);
		primaryStage.setTitle("Chat Client");
		primaryStage.setScene(scene);
		primaryStage.show();
		
		connectToServer();
	}
	
	public void connectToServer(){
		try{
			//create the socket
			System.out.println("connecting to host...");
			socket = new Socket(HOST, PORT); //the connection here works, but we don't exit this command it appears
			System.out.println("return from host, new socket = "+socket.toString());
		}catch(IOException ex){
			ex.printStackTrace();
		}
		
		System.out.println("starting a new client thread");
		//we need to start the new thread and fire it at the end
		new Thread( ()->{
			try{ //we have a chicken or the egg dilemma here
				//the client needs to create an OutputStream first, since the Server side will create an input stream first before creating an output stream
				toServer = new ObjectOutputStream(socket.getOutputStream());
				fromServer = new ObjectInputStream(socket.getInputStream());
				
				while(NotLoggedIn) {
					//will wait for ever
					waitForEntryRequest(); //wait until the user tries to login or register button is clicked, the button click currently does not interrupt waiting
				}
				
				//if we loggedIn successfully or registered successfully, then we get past the first while loop 
				while( Online ){ //basically always online after log in
					System.out.println("Online \n");
					//wait for new message
					//receive the new message, and print it into the textArea
					try {
						Request newRequest = (Request)fromServer.readObject();
						
						//get the username, the date and the message
						String newMessage = formatMessage(newRequest);
						Platform.runLater(()->pastMessageViewer.appendText(newMessage));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
			}catch(InterruptedException e){
				e.printStackTrace();
			}catch(IOException e){
				e.printStackTrace();
			}
		}).start(); //start the local session 
	}
	
	public void checkSocketStatus() throws InterruptedException{
		if(Online){
			System.out.println("socket is now online");
			Thread.sleep(5000);
		}
	}
	
	public void waitForEntryRequest() throws InterruptedException{
		while(NotLoggedIn){
			Thread.sleep(100);
		}
	}
	
	public class ChatRegisterButton extends Button{
		public ChatRegisterButton(){
			super("Register");
			//register the button listener here
			this.setOnMouseClicked(e->handleRegisterMouseClick());
		}
		
		private void handleRegisterMouseClick(){
			//get the values from the 
			String user = usernameField.getText().trim();
			String pass = passwordField.getText().trim();
			//create a new InitialRequest object and send it to the server
			InitialRequest init = new InitialRequest(REGISTER);
			init.setUserPass( user, pass ); //fill in the registration user and password fields
			
			InitialResponse response = null;
			try {
				toServer.writeObject(init);
				response = (InitialResponse)fromServer.readObject();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch ( ClassNotFoundException e2){
				e2.printStackTrace();
			}
		
			//if successful
			if(response != null ){ 
				if( response.getStatusCode() == OK ){ //registered OK
					currentUsername = user;
					successfulLoginRegister();
				}else{ //if not successful display the reason why we were not successful
					//we need display the message label
					//for now we will add a simple printout
					System.out.println(response.getMessage());
				}
			}
		}
	}
	
	public class ChatLoginButton extends Button{
		public ChatLoginButton(){
			super("Login");
			//register the button listener here
			this.setOnMouseClicked(e->handleLoginMouseClick());
		}
		
		private void handleLoginMouseClick(){
			String user = usernameField.getText();
			String pass = passwordField.getText();
			//create a new InitialRequest object and send it to the server
			InitialRequest init = new InitialRequest(LOGIN);
			init.setUserPass( user, pass ); //fill in the registration user and password fields
			
			InitialResponse response = null;
			try {
				toServer.writeObject(init);
				response = (InitialResponse)fromServer.readObject();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch ( ClassNotFoundException e2){
				e2.printStackTrace();
			}
			
			//if successful
			if(response != null ){ 
				if( response.getStatusCode() == OK ){ //registered OK 
					currentUsername = user;
					successfulLoginRegister();
				}else{ //if not successful display the reason why we were not successful
					//we need display the message label
					//for now we will add a simple printout
					System.out.println(response.getMessage());
				}
			}
		}
	}
	
	private void successfulLoginRegister(){
		//maybe we need to run platform.runLater for all of these actions
		Platform.runLater(() ->{ 
			masterContainer.getChildren().remove(registrationContainer);
			ChatHeaderLabel.setText(currentUsername + "\n");			
			masterContainer.getChildren().add(chatContainer);
			});
		NotLoggedIn = false; //to break the while loop in
		Online = true;
	}
	
	//we will get to this one later
	class MessageEditorKeyPressHandler implements EventHandler<KeyEvent>{
		@Override
		public void handle(KeyEvent keyEvent) {
			if( keyEvent.getCode() == KeyCode.ENTER){
				//if the enter key is pressed, we need to do a few things
				String message = newMessageEditor.getText().trim();
				if(message.length() > 0){ //if not an empty string
					Request newRequest = new Request(currentUsername, NEW_MESSAGE, message);
					//update the pastMessageViewer
					String newMessage = formatMessage(newRequest);
					Platform.runLater(()->pastMessageViewer.appendText(newMessage));
					try {
						toServer.writeObject(newRequest);
						System.out.println("wrote to server");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	
	public String formatMessage(Request NReq){
		String usr = NReq.getFromName();
		String message = NReq.getMessage();
		Date creationDate = NReq.getCreationTime();
		return usr+": "+message+"\n"+creationDate.toString()+"\n";
	}
	
	public static void main(String[] args){
		launch(args);
	}
}
