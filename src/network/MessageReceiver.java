package src.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

import static src.utils.Utils.*;
import src.utils.MessageType;

public class MessageReceiver implements Runnable {

    private String header;
    private byte[] body;
    private String type;
    private ChordNode peer;
    private InetSocketAddress ip_sender;
    private String string_msg;
    private SocketChannel socket_channel;
    private SSLEngine engine;

    private Message msg = null;
    private MessageSender msg_sender = null;
    private InetSocketAddress addr_response;
    private int key_response;
    private ByteArrayOutputStream bos;
    private ByteArrayInputStream bis;
    private ObjectOutputStream out;
    private ObjectInput in;
    private byte[] bytes;

    public MessageReceiver(ByteBuffer msg, ChordNode peer, SocketChannel socket_channel, SSLEngine engine) {

        this.socket_channel = socket_channel;
        this.engine = engine;
        this.peer = peer;

        byte[] message = trim_message(msg.array());
        string_msg = new String(message);
        String[] pieces = string_msg.split(CRLF);
        header = pieces[0];
        type = header.split(" ")[0];

        String ip_sender_str = header.split(" ")[1];
        String ip = ip_sender_str.split(":")[0];
        int port = Integer.parseInt(ip_sender_str.split(":")[1]);
        ip_sender = new InetSocketAddress(ip, port);

        int separation_index = string_msg.indexOf(CRLF);
        body = new byte[message.length - header.length() - 4];
        System.arraycopy(message, separation_index + 4, body, 0, body.length);
        // System.out.println("Message received: " + ip_sender);
        // System.out.println("Node peer: " + peer.get_address());
    }

    @Override
    public void run() {
        peer.set_last_response(string_msg);
        switch (type) {
            case MessageType.JOIN:
                msg = new Message(MessageType.OK, peer.get_address());
                requestMessage(peer, ip_sender, 100, msg);

                break;

            case MessageType.OK:
                System.out.println("Response message received successfully!");
                break;

            // EDU
            /*
             * case MessageType.PREDECESSOR:
             * System.out.println("Predecessor message received successfully!");
             * //peer.set_predecessor(ip_sender); System.out.println(ip_sender); break;
             */

            case MessageType.GET_PREDECESSOR:
                System.out.println("Message received: send your predecessor to " + ip_sender);
                msg = new Message(MessageType.RECEIVED_PREDECESSOR, peer.get_address());
                addr_response = peer.get_predecessor();
                bos = new ByteArrayOutputStream();
                //out = null;
                //bytes = null;
                try {
                    //out = new ObjectOutputStream(bos);
                    //out.writeObject(addr_response);
                    //out.flush();
                    //bytes = bos.toByteArray();
                } catch (Exception e) {
                    e.printStackTrace();
                    // ignore exception
                } finally {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // ignore close exception
                    }
                }
                msg.set_body(bytes);

                Message msg2 = new Message("RESPONSE", "192.2.2.2:8007");
                send_response(peer, msg2, socket_channel, engine);
                break;
            case MessageType.RECEIVED_PREDECESSOR:
                bis = new ByteArrayInputStream(body);
                in = null;
                addr_response = null;
                try {
                in = new ObjectInputStream(bis);
                addr_response = (InetSocketAddress) in.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    // ignore exception
                    } finally {
                        try {
                            if (in != null) {
                            in.close();
                            }
                        } catch (IOException ex) {
                        // ignore close exception
                    }
                }
                System.out.println("Message received: received predecessor " + addr_response + " of " + ip_sender);
                InetSocketAddress successor = peer.get_successor();
                // decides wether p should be n‘s successor instead (this is the case if p recently joined the system).
                Key predeccessor_key;
                Key successor_key;
                if(addr_response != null){
                    predeccessor_key = Key.create_key_from_address(addr_response);
                    successor_key = Key.create_key_from_address(successor);
                    // if predeccessor ∈ (n, successor) then
                    if(peer.betweenKeys(peer.get_local_key().key, predeccessor_key.key, successor_key.key)){
                        peer.update_ith_finger(1, addr_response);
                        successor = addr_response;
                    }
                }
                // successor.notify(n)
                Message msg = new Message(MessageType.NOTIFY_IM_PREDECESSOR, peer.get_address());
                requestMessage(peer,  successor, 100, msg);
                
                break;

            case MessageType.NOTIFY_IM_PREDECESSOR:
                System.out.println("Message received: " + ip_sender + " said it is my predecessor");
                peer.notify(ip_sender);
                break;

            case MessageType.REQUEST_KEY:
                System.out.println("Message received: asking if i am alive");
                msg = new Message(MessageType.SENDING_KEY, peer.get_address());
                requestMessage(peer,  ip_sender, 100, msg);
          
                break;
            
            case MessageType.SENDING_KEY:
                System.out.println("Message received: your predecessor is still alive");
                break;

            case MessageType.FIND_SUCCESSOR_KEY:
                bis = new ByteArrayInputStream(body);
                in = null;
                key_response = -1;
                try {
                in = new ObjectInputStream(bis);
                key_response = (int) in.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    // ignore exception
                } finally {
                    try {
                        if (in != null) {
                        in.close();
                        }
                    } catch (IOException ex) {
                        // ignore close exception
                    }
                }
                System.out.println("Message received: find successor of key " + key_response);
                msg = new Message(MessageType.FOUND_SUCCESSOR_KEY, peer.get_address());
                addr_response = peer.find_successor_addr(key_response);
                bos = new ByteArrayOutputStream();
                out = null;
                bytes = null;
                try {
                    out = new ObjectOutputStream(bos);   
                    out.writeObject(addr_response);
                    out.writeObject(key_response);
                    out.flush();
                    bytes = bos.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                    // ignore exception
                } finally {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        // ignore close exception
                    }
                }
                msg.set_body(bytes);
                System.out.println("Message sent: i found the key " + key_response + " in " + addr_response );
                requestMessage(peer,  ip_sender, 100, msg);
               
                break;
            
            case MessageType.FOUND_SUCCESSOR_KEY:
                bis = new ByteArrayInputStream(body);
                in = null;
                addr_response = null;
                key_response = -1;
                try {
                in = new ObjectInputStream(bis);
                addr_response = (InetSocketAddress) in.readObject();
                key_response = (int) in.readObject();
                } catch (ClassNotFoundException | IOException e) {
                    // ignore exception
                    } finally {
                        try {
                            if (in != null) {
                            in.close();
                            }
                        } catch (IOException ex) {
                        // ignore close exception
                    }
                }
                System.out.println("Message received: key " + key_response + " was found in " + addr_response );
                for (int i = 1; i <= KEY_SIZE; i++) {
                    int key = (int) ((int) (peer.get_local_key().key + Math.pow(2, i - 1)) % Math.pow(2, KEY_SIZE));
                    if(key_response == key){
                        // TODO: ele as vezes chega aqui, mas não atualiza a tabela não sei pq :/
                        peer.update_ith_finger(i, addr_response);
                        return;
                    }
                }
                //join circle recently
                peer.update_ith_finger(-1, addr_response);
                break;
        }
    }
    
}