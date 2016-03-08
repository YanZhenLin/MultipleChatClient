package application;

public class InitialResponse implements ChatConfig, java.io.Serializable {

	private int EntryType = LOGIN;
	private int statusCode = OK;
	private String message = "";
	
	public InitialResponse(int type){
		EntryType = type;
	}
	
	public InitialResponse(int type, int statusCode, String msg){
		this.EntryType = type;
		this.statusCode = statusCode;
		this.message = message;
	}
	
	protected void setResponse(int statusCode, String msg){
		this.statusCode = statusCode;
		this.message = msg;
	}
	
	public String getMessage(){
		return message;
	}
	
	public int getStatusCode(){
		return statusCode;
	}
	
	public int getType(){
		return EntryType;
	}
	
}
