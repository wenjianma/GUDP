import java.net.*;

public class GUDP_peer{
	
	private InetSocketAddress socket_address;
	private GUDP_send_buffer send_buffer;
	private GUDP_receive_buffer receive_buffer;
	
	public GUDP_peer(InetSocketAddress socket_address, GUDP_send_buffer send_buffer, GUDP_receive_buffer receive_buffer){	//constructor
		this.socket_address = socket_address;
		this.send_buffer = send_buffer;
		this.receive_buffer = receive_buffer;
	}
	
	public InetSocketAddress getSocketAddress(){
		return socket_address;
	}
	public GUDP_send_buffer get_send_buffer(){
		return send_buffer;
	}
	public GUDP_receive_buffer get_receive_buffer(){
		return receive_buffer;
	}
	public void transmission_complete(){
		send_buffer.transmission_complete();
	}
}