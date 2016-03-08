package application;

import java.util.Date;

public class Request implements ChatConfig, java.io.Serializable {

	private int requestType = NEW_MESSAGE;
	private Date time;
	private String fromUser;
	private String toUser;
	private String message;
	
	public Request( int requestType ){
		time = new Date();
		this.requestType = requestType;
	}
	
	public Request(String fromUser, int requestType, String Message){
		this(requestType);
		this.fromUser = fromUser;
		this.message = Message;
	}
	
	public Request(String fromUser, String toUser, int requestType, String Message){
		this(fromUser, requestType, Message);
		this.toUser = toUser;
	}
	
	public void  resetRequestType(int newType){
		requestType = newType;
	}
	
	public int getRequestType(){
		return requestType;
	}
	
	public Date getCreationTime(){
		return time;
	}
	
	public String getFromName(){
		return fromUser;
	}
	
	public String getToUser(){
		return toUser;
	}
	
	public String getMessage(){
		return message;
	}
	
}
