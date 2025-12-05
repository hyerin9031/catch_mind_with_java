package termproject;

import java.net.*;
import java.io.*;

public class HostMain {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Socket hostsocket = null;
		try {
			hostsocket = new Socket("localhost", 5000);
			
			new HostHomeFrame(hostsocket);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}
