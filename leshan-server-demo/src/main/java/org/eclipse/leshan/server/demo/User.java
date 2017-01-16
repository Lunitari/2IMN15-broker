package org.eclipse.leshan.server.demo;

public class User {
//	public String UserID;
//	public int GroupNr;
//	public String Username;
//	public String Email;
//	private String Password;
//	private boolean AtDesk;
	
	public String UserID;
	public int GroupNo;
	public String RoomId;
	public String Name;
//	private String Email;
	public String Email;
	private String Password;
	private boolean AtDesk;
	
	public User(String userID, int groupNo, String roomId, String name, String email, String password) {
        UserID = userID;
        GroupNo = groupNo;
        RoomId=roomId;
        Name = name;
        Email = email;
        Password = password;
//      AtDesk = atDesk;      
    }
	
	public void updatePresenceUser(boolean atDesk){
		AtDesk = atDesk;
	}
	
	public int checkLogin(String password){
		return password.equals(Password) ? AtDesk ? 1 : 2 : 0;
	}
}
