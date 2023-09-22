import java.net.*;
import java.util.LinkedList;
import java.util.Random;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GUDP_send_buffer extends Thread{
	
	private LinkedList<GUDPPacket> send_buffer;
	private LinkedList<GUDP_packet_timer> window;
	
	private int sequence_number;
	private int window_size = 3;
	private DatagramSocket socket;
	private GUDPPacket ack_gudppacket;
	private InetSocketAddress remote_socket_address;
	private boolean transmission_complete = false;
	private boolean sending_completed = false;
	
	public GUDP_send_buffer(DatagramSocket socket, InetSocketAddress remote_socket_address){	//constructor
		send_buffer = new LinkedList<GUDPPacket>();
		window = new LinkedList<GUDP_packet_timer>();
		this.socket = socket;
		this.remote_socket_address = remote_socket_address;
	}
	
	@Override
	public void run(){							//start and send BSN packet first
		
		send_buffer.addFirst(generate_BSN());	//send BSN_packet to the first place of the send_buffer
		
		int counter = 0;
		boolean done = false;
		
		while((send_buffer.size() != 0 || window.size() != 0) || !transmission_complete){		//is at least of size (1), because of the BSN packet, so if we send at least 1 packet, then size is 2
			if(window.size() < window_size){	//add a packet to the window and remove it from the send_buffer
				try{
					if(send_buffer.getFirst().getSocketAddress() != null){	//if there is a packet in the send_buffer, try to send it
						send_buffer.getFirst().setSeqno(sequence_number);
						GUDP_packet_timer new_packet_timer = new GUDP_packet_timer(socket, send_buffer.getFirst());
						send_buffer.removeFirst();
						window.add(new_packet_timer);
						new_packet_timer.start();
						sequence_number = sequence_number + 1;
					}
				}
				catch(Exception e){}
			}
			for(int x = 0; x < window.size(); x++){	//check if a packet has been transmitted for the maximum amount of times without receiving an ACK
				try{
					if(window.get(x).get_counter() == 0){
						//packet has timed out, network problem
						System.out.println("Packet with sequence_number: " + window.get(x).get_packet().getSeqno() + " has timed out, network problem.");
						System.exit(1);
					}
				}
				catch(Exception e){}
			}
			
			if(counter == 1){
				//receive the ACKs//
				new receive_and_prepare(window).start();
				//receive the ACKs//
			}
			counter++;
			
		}
		
		sending_completed = true;
		
	}
	
	public void to_send_buffer(GUDPPacket gudppacket){
		send_buffer.add(gudppacket);
	}
	public void transmission_complete(){
		transmission_complete = true;
	}
	public boolean get_sending_completed(){
		return sending_completed;
	}
	
	private GUDPPacket generate_BSN(){
		//Random Generator
		Random rand = new Random(); //initiate the Random class to generate a random integer (0-1000) used as the BSN
		int upperbound = 1000;
		sequence_number = rand.nextInt(upperbound);
		//Random Generator
		//buffer crap
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.BIG_ENDIAN);
		//buffer crap
		GUDPPacket BSN_packet = new GUDPPacket(buffer);
		BSN_packet.setPayloadLength(0);
		BSN_packet.setVersion((short) 1);
		BSN_packet.setType((short) 2);	//type 2 == BSN
		BSN_packet.setSeqno(sequence_number);
		//System.out.println("BSN Socket_Address: " + remote_socket_address);
		BSN_packet.setSocketAddress(remote_socket_address);	//we cast the remote socket address to InetSocketAddress
		
		//System.out.println("BSN number: " + BSN_packet.getSeqno());
		
		return BSN_packet;
		
		//This is how to send the packet//
		//sequence_number = sequence_number + 1;
		//try{
			//given code from send//
			//DatagramPacket udppacket = BSN_packet.pack();
			//socket.send(udppacket);
			//given code from send//
		//}
		//catch(Exception e){
			//error handling code
		//}
		//This is how to send the packet//
	}
	
	private class receive_and_prepare extends Thread{
		
		public receive_and_prepare(LinkedList<GUDP_packet_timer> window){	//constructor
			
		}
		@Override
		public void run(){
			while(!sending_completed){
				
				ack_gudppacket = null;
			
				try{
					byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
					DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
					socket.receive(udppacket);
					ack_gudppacket = GUDPPacket.unpack(udppacket);
				}
				catch(Exception e){
					//error handling code
				}
			
				
				//process the ACKs//
				if(ack_gudppacket != null && window != null){
					try{
						//System.out.println(ack_gudppacket.getSeqno() + " == " + window.getFirst().get_packet().getSeqno());
						//if the sequence_number of the packet received is equal to the sequence_number of any sent packet, remove that packet from the window
						for(int x = 0; x < window_size; x++){
							if(ack_gudppacket.getSeqno() == (window.get(x).get_packet().getSeqno()) + 1){
								System.out.println("ACK received for sequence_number: " + ack_gudppacket.getSeqno());
								window.get(x).received_ACK();
								window.remove(x);
							}
						}
					}
					catch(Exception e) {}
				}
				else{
					break;
				}
				//process the ACKs//
			}
		}
	}
	
}