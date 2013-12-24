import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Node  implements PeerSearchSimplified {
	private int Port;							// Nodes port Value
	private Long id;							// Nodes Id value
	Map<Integer, String> routing_table; // int nodeId and String Ip_address
	Map<String, HashMap<String, Integer>> indexer; // String Word & Map of
													// Strin links and frequency
	JSONObject obj = new JSONObject();				// JSON object used for Messages
	private DatagramSocket socket = null;			// Datagram Socket for transmission
	static byte[] buffer = new byte[1024];			// Byte[] for sending and recieving JSON messages from
	static byte[] recieve = new byte[1024];			// differnet nodes

	Node() {
		super();
	}

	public DatagramSocket init(DatagramSocket UdpSocket)throws SocketException, UnknownHostException {
		this.Port = UdpSocket.getLocalPort();					//Int funtion
		UdpSocket.close();
		routing_table = new HashMap<Integer, String>();
		this.socket = new DatagramSocket(this.Port);
		return UdpSocket;
	}
	
		
	public void send() throws JSONException, IOException {
		//Send onto the network
		//Simple send. Creates a JSON message with, node_id, ip_address and port.
		//sends it on the network.
		InetSocketAddress IPAddress = new InetSocketAddress("localhost", 8767);
		obj.put("node_id", this.id);
		obj.put("ip_address", "190.67.253.1");
		obj.put("port", 1008);
		buffer = obj.toString().getBytes();
		DatagramPacket msg_to_send = new DatagramPacket(buffer, buffer.length,
				IPAddress.getAddress(), IPAddress.getPort());
		this.socket.send(msg_to_send);

	}

	public SearchResult[] search(String[] words) throws JSONException,InterruptedException {
		int[] hash = new int[words.length]; // get the hash of each word being searched
		hash = hashCode(words);             // inorder to find them in network
		ArrayList<String> temparray = new ArrayList<String>();    // create a tmp array list.
		for (int i = 0; i < words.length; i++) {
			obj.put("type", "SEARCH");						// Loop through all the words
			obj.put("word", words);							// Create a JSON message for each
			obj.put("node_id", String.valueOf(hash[i]));	
			obj.put("sender_id", String.valueOf(this.id));
			buffer = obj.toString().getBytes();
			try {
				//Set up a sending DP and a recieving DP
				DatagramPacket sending = new DatagramPacket(buffer, buffer.length,InetAddress.getByName("localhost"), 8767);
				DatagramPacket recieving = new DatagramPacket(recieve, recieve.length); // receive
				//send of the JSON message																// packet
				this.socket.send(sending);
				this.socket.setSoTimeout(5000); // wait x amount of time
				this.socket.receive(recieving);

				if (recieving.getData().toString().equals(null))  // check that data != 0 if so node could be dead
					ping(obj); // ping node 
				else {
					JSONArray temparr = new JSONArray();				//create new array
					JSONObject result = new JSONObject(recieving.getData());
					System.out.println(result+" here we go \n");
					temparr = result.getJSONArray("response");			// extracting response from recieved message
					for (int j = 0; j < temparr.length(); j++) {
						temparray.add(temparr.getString(j));  		//	saving it into temp array
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return (SearchResult[]) temparray.toArray(); // return results
	}

	public void run() throws IOException, JSONException {
		DatagramPacket recieved = new DatagramPacket( recieve,  recieve.length);
		this.socket.receive(recieved);
		String Type = new String(recieved.getData());
		JSONObject obj = new JSONObject(Type);
		Type = obj.get("type").toString();
		switch (Type) {
		case "JOINING_NETWORK":
			handle_join(obj);
			break;
		case "JOINING_NETWORK_RELAY":
			 handle_JoinRelayMsg(obj);
			break;
		case "ROUTING_INFO":
			 handle_RoutingMsg(obj);
			break;
		case "LEAVING_NETWORK":
			handle_LeavingMsg(obj);
			break;
		case "INDEX":
			 handle_IndexMsg(obj);
			break;
		case "SEARCH":
			 handle_SearchMsg(obj);
			break;
		case "SEARCH_RESPONSE":
			//handle_SearchResponseMsg(obj);
			break;
		case "PING":
			 handle_ping(obj);
			break;
		case "ACK":

			break;
		case "ACK_INDEX":

			break;
		default:

			break;
		}
	}
	
	private void handle_RoutingMsg(JSONObject target) throws JSONException, UnknownHostException {
		// For the gate way node 
		//Not needed because of simplification, no leaf sets !!
		
	}

	private void handle_SearchMsg(JSONObject SearchResponse) throws JSONException, IOException {
		/**
		 * Create JSON Message to send back  
		 **/		
		obj.put("type", "SEARCH_RESPONSE");
		obj.put("word", SearchResponse.get("word").toString());
		obj.put("node_id", String.valueOf(this.id));
		obj.put("target_id", SearchResponse.get("sender_id").toString());
		
		// JSON Array to store result of search.
		//TODO
		JSONArray result = new JSONArray();
		
		Iterator<Entry<String, HashMap<String, Integer>>> iter = indexer.entrySet().iterator();
		 HashMap<String, Integer> temp;
		while(iter.hasNext()){
			Entry<String, HashMap<String, Integer>> table = iter.next();
            temp = table.getValue();
            String url = temp.toString();
            JSONObject Jurl = new JSONObject(url);
            JSONObject Jrank = new JSONObject(table.getKey());
            
            result.put(Jurl);            
            result.put(Jrank);			
		}
		obj.put("response", result);
		DatagramPacket SearchRes = new DatagramPacket(buffer, buffer.length,InetAddress.getByName(obj.get("ip_address").toString()), (Integer)obj.get("port"));
        socket.send(SearchRes);
	}

	private void handle_JoinRelayMsg(JSONObject JoinRelayReq) throws JSONException, IOException {
		//Not needed because of simplification, no leaf sets !!
		
		String sender_id = JoinRelayReq.get("node_id").toString();	// Get who is sending the Join req
		obj.put("type", "JOINING_NETWORK_SIMPLIFIED");				// Create a Json message
		obj.put("node_id", sender_id);								// Pass on the sender as node_id
		obj.put("target_id", JoinRelayReq.get("target_id").toString() );	// THe target_id
		obj.put("ip_address", JoinRelayReq.get("ip_address").toString());	// Use the senders ip, only relaying no need to use our ip
		
		buffer = obj.toString().getBytes();
		// TODO Check if node is in table ir not add (Just in case it happens for some reason).
		DatagramPacket RelayMsg = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(JoinRelayReq.get("target_id").toString()), 8767);
        socket.send(RelayMsg);			
		
		// send the routing information on
        // did this in handle join method.
        // think i messed up 
		JSONObject obj2 = new JSONObject();
		obj2.put("type", "ROUTING_INFO");
		obj2.put("gateway_id", String.valueOf(this.id)); // gateway node id
		obj2.put("node_id", JoinRelayReq.get("node_id")); // indicates target node (which is the joining node)
		obj2.put("ip_address", InetAddress.getByName("localhost").toString());
		
		routing_table.put((Integer) JoinRelayReq.get("id"),JoinRelayReq.get("ip_address").toString());
		Iterator<Entry<Integer, String>> iter = routing_table.entrySet().iterator();
		JSONArray table = new JSONArray(); // building json array for routing table
		while(iter.hasNext()){
				Entry<Integer, String> iterPoint = iter.next();
	            JSONObject tmp = new JSONObject();
	            tmp.put("node_id", iterPoint.getKey());
	            tmp.put("ip_address", iterPoint.getValue());
	            table.put(tmp);
	           //table:
	           //		"node_id"
	           //		"ip_address"
	    }
	    obj2.put("routing_table", table);
	    buffer = obj.toString().getBytes();
	    InetSocketAddress IPAddress = new InetSocketAddress("localhost",(Integer)JoinRelayReq.get("port"));
	    DatagramPacket sendRoutingInfo = new DatagramPacket(buffer, buffer.length, IPAddress.getAddress(), IPAddress.getPort());
	    this.socket.send(sendRoutingInfo);
	}

	public void indexPage(String url, String[] words) {
		int[] hash = new int[words.length];
		hash = hashCode(words); // Hash the words
		for (int i = 0; i < words.length; i++) {
			try {
				// Create JSON message
				obj.put("type", "INDEX");
				obj.put("target_id", String.valueOf(this.id));
				obj.put("keyword", hash[i]); // hashed word to send
				JSONArray wordarr = new JSONArray(); // Create an array for links
				wordarr.put(url);	// add the url to links
				//"link": [
			    //           "http://www.newindex.com", // the url the word is found in
			    //           "http://www.xyz.com"
			    //          ]
				obj.put("link", wordarr);				
				buffer = obj.toString().getBytes(); // place into  buffer for sending.
				
				// Sending to the network
				//
				InetSocketAddress IPAddress = new InetSocketAddress("localhost", 8767);				
				DatagramPacket indexMsg = new DatagramPacket(buffer, buffer.length,IPAddress.getAddress(), IPAddress.getPort());
				this.socket.send(indexMsg);  // send

			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	
	
	private void handle_IndexMsg(JSONObject IndexReq) throws JSONException {
		// Recieve an index_message, update my own index.
		Integer rank;
		String link = IndexReq.get("link").toString();
		String word = IndexReq.get("keyword").toString();
			if (indexer.containsKey(word)) {
				if (!indexer.get(word).containsValue(link)) {
                    indexer.get(word).put(link, 0);
				} else {                                                         
                    rank = indexer.get(word).get(link);
                    rank++;
                    indexer.get(word).put(link, rank);                                               
				}
			}else{				
				HashMap<String, Integer> newLink = new HashMap<String,Integer>();
				newLink.put(link,0);
				indexer.put(word, newLink); // add new word and link
			}		
	}
	
	public boolean leaveNetwork(long network_id) throws IOException {
		try {
			/**
			 * Set up the Message
			 * */
			obj.put("type", "LEAVING_NETWORK");
			obj.put("node_id", network_id);
			buffer = obj.toString().getBytes();

			/**
			 * Iterate through the nodes routing table, informing each of its
			 * intention to leave*
			 */
			Iterator<Entry<Integer, String>> iter = routing_table.entrySet()
					.iterator();
			@SuppressWarnings("resource")
			DatagramSocket socket = new DatagramSocket(8767);

			while (iter.hasNext()) {
				Entry<Integer, String> next_node = iter.next();
				DatagramPacket disconnect_msg = new DatagramPacket(buffer,buffer.length, InetAddress.getByName(next_node.getValue()),8767);
				socket.send(disconnect_msg);
			}
			return true;
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return false;
	}

	private void handle_LeavingMsg(JSONObject LeaveReq) throws JSONException {
			routing_table.remove(LeaveReq.get("node_id").toString());
	}
	
	@Override
	public synchronized long joinNetwork(InetSocketAddress inetAddr, String ident, String target_id) throws JSONException, IOException,
			InterruptedException {
		this.id = (long) Integer.parseInt(ident);
		obj.put("type", "JOINING_NETWORK");
		obj.put("node_id", ident);
		obj.put("ip_address", inetAddr.getHostName());
		// obj.put("port",port); //

		buffer = obj.toString().getBytes();
		InetSocketAddress IPAddress = new InetSocketAddress("localhost", 8767); // Change
		DatagramPacket msg_to_send = new DatagramPacket(buffer, buffer.length,
				IPAddress.getAddress(), IPAddress.getPort());
		this.socket.send(msg_to_send);

		return 1;
	}

	private synchronized void handle_join(JSONObject JoinReq) throws JSONException, IOException{
		
		/**
		 * Method handles when a Node recieves a Join Network req. Ie it is the boortstrap
		 * Firstly it adds the new node to its routing table. Then iterates thorugh all known nodes saving there ids and address
		 * and sends on the routing information to the New node.
		 * **/
		obj.put("type", "ROUTING_INFO");
		obj.put("gateway_id", String.valueOf(this.id)); // gateway node id
		obj.put("node_id", JoinReq.get("node_id")); // indicates target node (which is the joining node)
		obj.put("ip_address", InetAddress.getByName("localhost").toString());
		
		routing_table.put((Integer) JoinReq.get("id"),JoinReq.get("ip_address").toString());
		Iterator<Entry<Integer, String>> iter = routing_table.entrySet().iterator();
		JSONArray table = new JSONArray(); // building json array for routing table
		while(iter.hasNext()){
				Entry<Integer, String> iterPoint = iter.next();
	            JSONObject tmp = new JSONObject();
	            tmp.put("node_id", iterPoint.getKey());
	            tmp.put("ip_address", iterPoint.getValue());
	            table.put(tmp);
	           //table:
	           //		"node_id"
	           //		"ip_address"
	    }
	    obj.put("routing_table", table);
	    buffer = obj.toString().getBytes();
	    InetSocketAddress IPAddress = new InetSocketAddress("localhost",(Integer)JoinReq.get("port"));
	    DatagramPacket sendRoutingInfo = new DatagramPacket(buffer, buffer.length, IPAddress.getAddress(), IPAddress.getPort());
	    this.socket.send(sendRoutingInfo);	   
}
	
	private void ping(JSONObject blah) throws JSONException, IOException {
		/*
		 * Pinging a node in the network to see if it is still aliveMust wait
		 * for an ACK, if one is recieved then the node is alive otherwise it is
		 * dead. Ping is called when attempting to TODO
		 */
		JSONObject obj = new JSONObject(); // create the JSON Message
		obj.put("type", "PING"); // Type of Message
		obj.put("sender_id", this.id); // Sender iD
		obj.put("target_id", blah.get("node_id").toString()); // Target id
		obj.put("ip_address", blah.get("")); // Ip address
		/*
		 * Create the Datapackets to hold the Ping and Ack messages Then Send
		 * the ping and wait X amount of time, while waiting to recieve ack
		 */
		buffer = obj.toString().getBytes();
		DatagramPacket SendPing = new DatagramPacket(buffer, buffer.length,
				InetAddress.getByName("localhost"), 8767);
		DatagramPacket RecAck = new DatagramPacket(recieve, recieve.length); // receive
																				// packet
		this.socket.send(SendPing);
		this.socket.setSoTimeout(3000); //
		this.socket.receive(RecAck); // Once the ack is recieved we must check
										// if it is empty

		if (RecAck.getData().toString().length() < 1) {   // if nothing is sent back after Timeout assume Dead
			routing_table.remove(blah.get("node_id").toString());
		}

	}
	
	private void handle_ping(JSONObject PingMsg) throws JSONException, IOException{
		// Create JSON message
		 obj.put("type", "ACK");
		 obj.put("node_id", String.valueOf(this.id));
		 obj.put("ip_address", PingMsg.get("ip_address"));
		 buffer = obj.toString().getBytes(); 				//Pass message to buffer for sending
		 InetSocketAddress IPAddress = new InetSocketAddress("localhost",(Integer)PingMsg.get("port"));
         DatagramPacket ACKMsg = new DatagramPacket(buffer, buffer.length, IPAddress.getAddress(), IPAddress.getPort());
         this.socket.send(ACKMsg); // Send ACk  
	}	
		
	private int[] hashCode(String[] str) {
		  int[] hash = new int[str.length];
		  for (int i = 0; i < hash.length; i++) {
			  for (int j = 0; j < str[i].length(); j++) {
				  hash[i] = hash[i] * 31 + str[i].charAt(j);
			  }
			  hash[i] = Math.abs(hash[i]);
		  }		  
		  return hash;
		}	
	
}
 
