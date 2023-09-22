import java.net.*;
import java.util.Random;

public class GUDP_packet_timer extends Thread{
	
	private GUDPPacket gudppacket;
	private DatagramSocket socket;
	private boolean ACK = false;
	private int send_counter = 5;
	private int x;
	
	public GUDP_packet_timer(DatagramSocket socket, GUDPPacket gudppacket){	//constructor
		this.gudppacket = gudppacket;
		this.socket = socket;
	}
	
	@Override
	public void run(){
		while(ACK == false && send_counter > 0){
			try{
				//Random Generator
				Random rand = new Random(); //initiate the Random class to generate a random integer (0-1000) used as the BSN
				int upperbound = 50;
				//Random Generator
				Thread.sleep(100 + 2*x);
				//given code
				DatagramPacket udppacket = gudppacket.pack();
				socket.send(udppacket);
				//given code
				//System.out.println("Sequence_number: " + gudppacket.getSeqno());
				
				x = rand.nextInt(upperbound);
				
				Thread.sleep(100 + (7 - send_counter) * x);	//wait for 50ms + a random amount of time, up to 1000ms before trying to send the packet again
				}
			catch(Exception e){}
			send_counter = send_counter - 1;
		}
	}
	public void received_ACK(){
		ACK = true;
	}
	public boolean get_ACK(){
		return ACK;
	}
	public int get_counter(){
		return send_counter;
	}
	public GUDPPacket get_packet(){
		return gudppacket;
	}
}