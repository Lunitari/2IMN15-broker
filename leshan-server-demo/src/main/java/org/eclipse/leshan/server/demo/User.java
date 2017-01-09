package org.eclipse.leshan.server.demo;

public class User {
	public String UserID;
	public int GroupNr;
	public String Username;
	public String Email;
	private String Password;
	private boolean AtDesk;
	
	public User(String userID, int groupNr, String username, String email, String password, boolean atDesk) {
        UserID = userID;
        groupNr = groupNr;
        Username = username;
        Email = email;
        Password = password;
        AtDesk = atDesk;
        
    }
	
	public void updatePresenceUser(boolean atDesk){
		AtDesk = atDesk;
	}
	
	public int checkLogin(String password){
		return password.equals(Password) ? AtDesk ? 1 : 2 : 0;
	}
}
