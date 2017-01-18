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
	private double locationX;
	private double locationY;

	private String LightUSER1;
	private String LightUSER2;
	private String Sensor;
	
	public User(String userID, int groupNo, String roomId, String name, String email, String password) {
        UserID = userID;
        GroupNo = groupNo;
        RoomId=roomId;
        Name = name;
        Email = email;
        Password = password;
//      AtDesk = atDesk;      
    }
	
	public void setLocation(double X, double Y) {
		locationX = X;
		locationY = Y;
	}

	public void setSensor(String sensor) {
		this.Sensor = sensor;
	}
	public void setLightUSER1(String light) {
		this.LightUSER1 = light;
	}
	public void setLightUSER2(String light) {
		this.LightUSER2 = light;
	}
	public String getSensor() {
		return Sensor;
	}
	
	public String getLightUSER1() {
		return LightUSER1;
	}
	public String getLightUSER2() {
		return LightUSER2;
	}
	
	
	public double getDistanceFromLocation(int X, int Y) {
		//sqrt((x2-x1)^2 + (y2-y1)^2)
		double distance = Math.sqrt(Math.pow(X - locationX, 2) + Math.pow(Y - locationY, 2));		
		return distance;
	}
	
	public void updatePresenceUser(boolean atDesk){
		AtDesk = atDesk;
	}
	
	public int checkLogin(String password){
		return password.equals(Password) ? AtDesk ? 1 : 2 : 0;
	}
}
