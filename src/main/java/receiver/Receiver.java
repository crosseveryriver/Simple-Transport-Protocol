package receiver;

import util.Packet;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Administrator on 2017/12/4.
 * 命令行参数 8800 file.txt
 */
public class Receiver {

    private int isn = 32;
    private DatagramSocket socket;
    ArrayList<byte[]> buffer = new ArrayList<byte[]>();
    InetAddress ip;
    int port;

    public void waitAndConnect(int port) throws IOException, InterruptedException {
      socket = new DatagramSocket(port);
      byte[] data = new byte[10];
      DatagramPacket udpPacket = new DatagramPacket(data,data.length);
      //第一次握手
      socket.receive(udpPacket);
      //第二次握手
      Packet packet = new Packet(data);
        int senderIsn = packet.getSEQ();
      if(packet.getSYN() == 1){
          packet.setACK((byte) (packet.getSEQ() + 1));
          packet.setSEQ((byte) isn);
      }
      data = packet.toByteArray();
      ip = udpPacket.getAddress();
      this.port = udpPacket.getPort();
      udpPacket = new DatagramPacket(data,data.length,ip,this.port);
      Thread.sleep(100);
      socket.send(udpPacket);
      //第三次握手
        socket.receive(udpPacket);
        packet = new Packet(data);
        if(packet.getACK() != (isn + 1) || packet.getSEQ() != (senderIsn + 1)){
            throw new RuntimeException("Reveiver第三次握手失败");
        }
    }

    public void processPacket(){}

    public void saveData() throws IOException {

        byte[] data = new byte[30];
        DatagramPacket packet = new DatagramPacket(data,data.length,ip,port);
        do{
            try {
                socket.receive(packet);
                Packet stpPacket = new Packet(data);
                if(stpPacket.getFIN() != 0)
                    break;
                buffer.add(stpPacket.getData());
                stpPacket = new Packet((byte) 0,stpPacket.getSEQ(),(byte) 0 ,(byte) 0);
                data = stpPacket.toByteArray();
                packet = new DatagramPacket(data, data.length,ip,port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }while(true);
    }

    public void handleClose(){
        socket.close();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        int port = Integer.parseInt(args[0]);
        String file = args[1];
        Receiver receiver = new Receiver();
        receiver.waitAndConnect(port);
        receiver.saveData();
//        receiver.handleClose();

    }
}
