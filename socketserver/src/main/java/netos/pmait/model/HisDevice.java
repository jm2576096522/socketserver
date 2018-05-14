package netos.pmait.model;

import java.sql.Timestamp;

 

public class HisDevice {
	
	public  int state;
	
	public String mac;
	
	
	public Timestamp onlineTime;
	
	
	public Timestamp getOnlineTime() {
		return onlineTime;
	}

	public void setOnlineTime(Timestamp onlineTime) {
		this.onlineTime = onlineTime;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}
	
	

}
