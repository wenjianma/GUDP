import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

public class GUDP_receive_buffer{
	
	private LinkedList<GUDPPacket> receive_buffer;
	private int sequence_number = -50;
	private DatagramSocket socket;
	private GUDPPacket final_gudppacket;
	private GUDPPacket gudppacket;
	private InetSocketAddress remote_socket_address;
	private boolean found;
	private boolean BSN_received = false;
	private boolean ready = false;
	
	public GUDP_receive_buffer(DatagramSocket socket, InetSocketAddress remote_socket_address){	//constructor
		receive_buffer = new LinkedList<GUDPPacket>();
		//receive_buffer_2 = new LinkedList<GUDPPacket>();
		//buffer crap
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.BIG_ENDIAN);
		//buffer crap
		//buffer crap
		ByteBuffer buffer1 = ByteBuffer.allocate(8);
		buffer1.order(ByteOrder.BIG_ENDIAN);
		//buffer crap
		gudppacket = new GUDPPacket(buffer);
		final_gudppacket = new GUDPPacket(buffer1);
		this.socket = socket;
		this.remote_socket_address = remote_socket_address;
	}
	
	//@Override
	public void start(){	//start
		//find the package with the correct sequence_number, which is == current sequence_number + 1	
		found = false;
		try{
			receive_and_prepare r_n_p = new receive_and_prepare();
			r_n_p.start();
		}
		catch(Exception e){}
		
		while(!found){
			try{Thread.sleep(1);}
			catch(Exception e){}
		}
		
		//gudp_check_packet = r_n_p.get_packet();
		
	}
	
	public void to_receive_buffer(GUDPPacket gudppacket){
		
		boolean found_packet = false;
		ready = false;
		
		send_ACK(gudppacket);
		
		if(receive_buffer.size() == 0){
			//System.out.println("Adding Type: " + gudppacket.getType());
			receive_buffer.add(gudppacket);
		}
		else{
			for(int x = 0; x < receive_buffer.size(); x++){
				if(receive_buffer.get(x).getSeqno() == gudppacket.getSeqno()){
					found_packet = true;
				}
			}
			//if(!found_packet && gudppacket.getType() == 2){
			//	if(gudppacket.getSeqno() != -1){
			//		receive_buffer.addFirst(gudppacket);
			//	}
			//}
			if(!found_packet){
				System.out.println("Sending ACK: " + gudppacket.getSeqno() + " ");
				//send_ACK(gudppacket.getSeqno());
				if(gudppacket.getSeqno() >= sequence_number){
					//System.out.println("sequence_number added: " + gudppacket.getSeqno());
					receive_buffer.add(gudppacket);
				}
				
				//sort the receive_buffer based on sequence_number
				for(int x = 0; x < receive_buffer.size(); x++){
					for(int y = 0; y < receive_buffer.size(); y++){
						GUDPPacket temp;
						if(receive_buffer.get(x).getSeqno() < receive_buffer.get(y).getSeqno()){
							temp = receive_buffer.get(x);
							receive_buffer.set(x, receive_buffer.get(y));
							receive_buffer.set(y, temp);
						}
					}
				}
				//sort the receive_buffer based on sequence_number
				//purge the receive_buffer
				for(int x = 0; x < receive_buffer.size(); x++){
					if(receive_buffer.getFirst().getSeqno() < sequence_number){
						receive_buffer.removeFirst();
					}
				}
				//purge the receive_buffer
			}
		//System.out.println("//--Table--//");
		//for(int x=0;x<receive_buffer.size();x++){System.out.println(receive_buffer.get(x).getSeqno());}
		//System.out.println("//--Table--//");
		ready = true;
		}
		
	}
	public void receive_buffer_finalize(DatagramPacket packet){
		//needed to convey the packet to the higher level. added at the end of the GUDPSocket.send(packet) method.
		//System.out.println("This returns to the application: " + final_gudppacket.getSeqno());
		//if(found){
			//System.out.println("This returns to the application: " );
			try{
				System.out.println("This returns to the application: " + final_gudppacket.getSeqno());
				final_gudppacket.decapsulate(packet);	//makes no sense at first sight, but is needed.
			}
			catch(Exception e){
				System.out.println("Exception when returning to application: " + final_gudppacket.getSeqno());
			}
		//}
	}
	private class receive_and_prepare extends Thread{
		
		public receive_and_prepare(){}//empty constructor
		
		@Override
		public void run(){
			look_for_packet();
			while(!found){
				try{
					//given code//
					byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
					DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
					socket.receive(udppacket);
					GUDPPacket gudppacket = GUDPPacket.unpack(udppacket);
					//gudppacket.decapsulate(packet);
					//given code//
					to_receive_buffer(gudppacket);
				}
				catch(Exception e){
					//error handling code
				}
				look_for_packet();
			}
		}
		
		public void look_for_packet(){
			
			if(ready){
				//----scan the whole buffer instead of only one packet-----//
				try{
					
					if(receive_buffer.getFirst().getType() == 1 && receive_buffer.getFirst().getSeqno() == sequence_number){
						System.out.println("FOUND!" + receive_buffer.getFirst().getSeqno());
						//send_ACK(receive_buffer.getFirst().getSeqno());
						final_gudppacket = receive_buffer.getFirst();
						receive_buffer.removeFirst();
						found = true;
						sequence_number = sequence_number + 1;
						//break;
					}
					
					if(receive_buffer.getFirst().getType() == 2){	//if it is the BSN packet, set sequence_number and reply with an ACK					
						sequence_number = receive_buffer.getFirst().getSeqno() + 1;
						//send_ACK(receive_buffer.getFirst().getSeqno());
						receive_buffer.removeFirst();
						//break;
					}
					
				}
				catch(Exception e){}
				//if not found, receive a new packet and try again
			}
		}
	}
	private void send_ACK(GUDPPacket gudppacket){
		//buffer things
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.BIG_ENDIAN);
		//buffer things
		GUDPPacket ACK_packet = new GUDPPacket(buffer);
		ACK_packet.setPayloadLength(0);
		ACK_packet.setVersion((short) 1);
		ACK_packet.setType((short) 3);	//type 3 == ACK
		ACK_packet.setSeqno((gudppacket.getSeqno()) + 1);
		ACK_packet.setSocketAddress(remote_socket_address);	//we cast the remote socket address to InetSocketAddress
		
		try{
			//given code from send//
			//GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
			DatagramPacket udppacket = ACK_packet.pack();
			socket.send(udppacket);
			//given code from send//
		}
		catch(Exception e){}
	}
}