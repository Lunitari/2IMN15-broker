package org.eclipse.leshan.server.demo;

public class User {

	public String UserID;
	public int GroupNo;
	public String RoomId;
	public String Name;
	public String Email;
	private String Password;
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
        LightUSER1 = "";
        LightUSER2 = "";
        Sensor = "";
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

	public Boolean checkPassword(String password){
		return password.equals(Password);
	}
	
	public String checkUserType(String lightID) {
		if (lightID.equals(getLightUSER1())) {
			
			return "USER1";
		} 
		else if (lightID.equals(getLightUSER2())) {
		
			return "USER2";
		}
		return "USER3";
	}
}
