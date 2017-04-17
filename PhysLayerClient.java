import java.net.*;
import java.io.*;
import java.util.*;

/*
 * This program simulated the physical layer of communication by interpreting "high" and "low" signals
 */
public class PhysLayerClient {
	
	/*
	 * the main method doesn't do much work, just calls the functions and handles a few outputs.
	 */
	public static void main(String[] args) throws Exception{
		try(Socket socket = new Socket("codebank.xyz", 38002)){
			System.out.println("Connected to Server");
			double baseline = recievePreamble(socket);
			System.out.println("Baseline established from preamble: " + baseline);
			byte[] toSend = recieveInfo(socket, baseline);
			sendBytes(socket, toSend);
			socket.close();
			System.out.println("Disconnected from Server");
		}
	}
	
	/*
	 * this method receives the preamble in order to establish a baseline by which that actual
	 * message can be interpreted
	 */
	private static double recievePreamble(Socket socket) throws Exception{
		InputStream in = socket.getInputStream();
		double base = 0.0;
		for(int i = 0; i < 64; i++){
			base += in.read();
		}
		
		return (base / 64.0);
	}
	
	/*
	 * this method receives the actual message sent from the server. It uses the preamble to determine
	 * if a signal is "high" or "low". Then once we take in 5 signals, and use NRZI to interpret what they actually should be,
	 * we use a 4/5bit lookup table to determine the actual message.
	 */
	private static byte[] recieveInfo(Socket socket, double base) throws Exception{
		InputStream in = socket.getInputStream();
		Hashtable<String, String> fiveBit= gen5Bit();
		String[] message = new String[64];
		int prevSignal = 0;
		int curSignal = 0;
		String five = "";
		for(int i = 0; i < 64; i++){
			five = "";
			for( int j = 0; j < 5; j++ ){
				//figure out if we are getting a 0 or a 1 signal
				if( in.read() > base ){
					curSignal = 1;
				} else{
					curSignal = 0;
				}
				
				//NRZI decoding 
				if(curSignal == prevSignal){
					five += "0";
				} else{
					five += "1";
				}
				prevSignal = curSignal;
			}
			
			// 4/5bit decoding
			message[i] = fiveBit.get(five);
		}
		
		
		String recieved = "";
		byte[] recievedBytes = new byte[32];
		//setting up the message to be printed out as a HEX value and then turning that into actual byte values.
		for(int i = 0; i < 32; i++){
			String lowHalf = Integer.toHexString(Integer.parseInt(message[i*2], 2)).toUpperCase();
			String upHalf = Integer.toHexString(Integer.parseInt(message[(i*2) + 1], 2)).toUpperCase();
			String whole = lowHalf + upHalf;
			recieved += whole;
			recievedBytes[i] = (byte)Integer.parseInt(whole, 16); 
		}
		
		System.out.println("Recieved 32 Bytes: " + recieved);
		return recievedBytes;
	}
	
	/*
	 * simply a lookup table for 4/5bit decoding.
	 */
	private static Hashtable<String, String> gen5Bit(){
		Hashtable<String, String> fiveBit = new Hashtable<String, String>();
		fiveBit.put("11110","0000");
		fiveBit.put("01001","0001");
		fiveBit.put("10100","0010");
		fiveBit.put("10101","0011");
		fiveBit.put("01010","0100");
		fiveBit.put("01011","0101");
		fiveBit.put("01110","0110");
		fiveBit.put("01111","0111");
		fiveBit.put("10010","1000");
		fiveBit.put("10011","1001");
		fiveBit.put("10110","1010");
		fiveBit.put("10111","1011");
		fiveBit.put("11010","1100");
		fiveBit.put("11011","1101");
		fiveBit.put("11100","1110");
		fiveBit.put("11101","1111");
		return fiveBit;
	}
	
	/*
	 * a function used to send the message back to the server, and check if its good
	 */
	private static void sendBytes(Socket socket, byte[] toSend) throws Exception{
		OutputStream out = socket.getOutputStream();
		InputStream in = socket.getInputStream();
		out.write(toSend);
		int status = in.read();
		if(status == 1){
			System.out.println("Response Good");
		} else{
			System.out.println("Response bad");
		}
	}
	
	
}
