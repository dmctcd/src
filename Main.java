import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.JSONException;

public class Main {
	
	static Node node1 = new Node();
	public static void main(String[] args) throws JSONException, IOException, InterruptedException {
		DatagramSocket socket = new DatagramSocket(8767);
		String[] temp = {"foo","nag","Waa"};
		//node1.run();
		node1.init(socket);
		node1.send();
		//node1.index();
		node1.search(temp);

	}

}
