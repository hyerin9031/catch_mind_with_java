import java.net.*;

public class PlayerMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Socket playersocket = null;
		try {
			playersocket = new Socket("localhost", 7400);
			new PlayerHomeFrame(playersocket);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}