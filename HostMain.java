import java.net.*;

public class HostMain {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Socket hostsocket = null;
		DatagramSocket drawsocket = null;
		
		try {
			hostsocket = new Socket("localhost", 8000);
			
			new HostHomeFrame(hostsocket);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}
