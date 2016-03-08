package application;

public class User {
	
	private String username = "";
	private String password = "";
	
	public User(String username, String password){
		this.username = username;
		this.password = password;
	}
	
	protected void resetUsername(String newUserName){
		this.username = newUserName;
	}
	
	protected void resetPassword(String newPass){
		this.password = newPass;
	}
	
	protected String getUsername(){
		return username;
	}
	
	protected String getPassword(){
		return password;
	}
}
