package application;

public class InitialRequest implements ChatConfig, java.io.Serializable{

	private int requestType = LOGIN;
	private String Username;
	private String Password;
	
	public InitialRequest(int type){
		requestType = type;
	}
	
	protected int getRequest(){
		return requestType;
	}
	
	protected void setUserPass(String user, String pass){
		this.Username = user;
		this.Password = pass;
	}
	
	protected String getUserName(){
		return Username;
	}
	
	protected String getPass(){
		return Password;
	}
	
}
