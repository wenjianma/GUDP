import java.net.*;
import java.io.IOException;
import java.util.LinkedList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;


public class GUDPSocket implements GUDPSocketAPI {
    DatagramSocket datagramSocket;
	LinkedList<GUDP_peer> peer_list;
	boolean transmission_complete = false;
	
	GUDP_peer receive_peer;
	boolean first_time_receive = true;

    public GUDPSocket(DatagramSocket socket) {
        datagramSocket = socket;
		peer_list = new LinkedList<GUDP_peer>();
    }

    public void send(DatagramPacket packet) throws IOException {
		//given code//
        GUDPPacket gudppacket = GUDPPacket.encapsulate(packet);
		//given code//
		
		boolean found = false;
		
		for(int x = 0; x < peer_list.size(); x++){
			if(peer_list.get(x).getSocketAddress().equals(gudppacket.getSocketAddress())){	//if peer is found, push the packet to its buffer
				found = true;
				peer_list.get(x).get_send_buffer().to_send_buffer(gudppacket);
			}
		}
		if(found == false){	//if the peer is not found, create one with its send_buffer, add it to the peer_list and push the packet to its send_buffer
			
			GUDP_send_buffer send_buffer = new GUDP_send_buffer(datagramSocket, gudppacket.getSocketAddress());
			GUDP_peer new_peer = new GUDP_peer(gudppacket.getSocketAddress(), send_buffer, null);
			
			peer_list.add(new_peer);
			
			new_peer.get_send_buffer().to_send_buffer(gudppacket);
			new_peer.get_send_buffer().start();
		}
		
    }

    public void receive(DatagramPacket packet) throws IOException {
		//we are creating only one peer, since we are only receiving from one source per port
        //System.out.println("Try to check for peers...");
		try{
		//given code//
		byte[] buf = new byte[GUDPPacket.MAX_DATAGRAM_LEN];
		DatagramPacket udppacket = new DatagramPacket(buf, buf.length);
		datagramSocket.receive(udppacket);
		GUDPPacket gudppacket = GUDPPacket.unpack(udppacket);
		gudppacket.decapsulate(packet);
		//given code//
		
		boolean found = false;
		
		//check if this peer already exists
		for(int x = 0; x < peer_list.size(); x++){
			if(peer_list.get(x).getSocketAddress().equals(gudppacket.getSocketAddress())){
				found = true;
				peer_list.get(x).get_receive_buffer().to_receive_buffer(gudppacket);
				peer_list.get(x).get_receive_buffer().start();
				peer_list.get(x).get_receive_buffer().receive_buffer_finalize(packet);
			}
		}
		if(found == false){	//if the peer is not found, create one and a receive_buffer and add the peer to the list
			GUDP_receive_buffer receive_buffer = new GUDP_receive_buffer(datagramSocket, gudppacket.getSocketAddress());
			GUDP_peer new_peer = new GUDP_peer(gudppacket.getSocketAddress(), null, receive_buffer);
			
			peer_list.add(new_peer);
			
			new_peer.get_receive_buffer().to_receive_buffer(gudppacket);
			new_peer.get_receive_buffer().start();
			new_peer.get_receive_buffer().receive_buffer_finalize(packet);	//after finding the correct packet, finalize it, to send it to the application
		}
		}
		catch(Exception e){System.out.println("Hehe boi...");}
    }

    public void finish() throws IOException {	//watch out for these methods, they will terminate the threads
		for(int x = 0; x < peer_list.size(); x++){
			peer_list.get(x).transmission_complete();
		}
    }
    public void close() throws IOException {
        
		boolean none_sending;
		boolean none_receiving;
		
		//check senders//
		try{	//if try can do it, then we are sending, if not, catch will assume that we are receiving
			while(true){
				none_sending = true;
				for(int x = 0; x < peer_list.size(); x++){
					if(peer_list.get(x).get_send_buffer().get_sending_completed() == false){
						none_sending = false;
					}
				}
				if(none_sending == true){
					break;
				}
			}
		}
		catch(Exception e){}
		//check senders//
		
		System.exit(0);
	}
}

