package receiver;

import util.Helper;
import util.Packet;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;

/**
 * Created by Administrator on 2017/12/4.
 * 命令行参数 8800 file.txt
 */
public class Receiver {

    private int isn = 32;
    private DatagramSocket socket;
    HashSet<Packet> buffer = new HashSet<Packet>();
    InetAddress ip;
    int port;

    //日志文件相关变量
    FileWriter writer = new FileWriter("data/Receiver_log.txt");
    Date startDate = new Date();
    int totalSements;
    long totalBytes;

    public Receiver() throws IOException {
        writer.write("<snd/rcv/drop>\t<time>\t<type of packet>\t<seq>\t<number of bytes>\t<ack-number>\t\n");
    }

    public void waitAndConnect(int port) throws IOException, InterruptedException {
        socket = new DatagramSocket(port);
        byte[] data = new byte[10];
        DatagramPacket udpPacket = new DatagramPacket(data, data.length);
        //第一次握手
        System.out.println("Receiver 已经准备就绪");
        socket.receive(udpPacket);
        System.out.println(Arrays.toString(data));
        //第二次握手
        Packet packet = new Packet(data);
        writer.write(Helper.getLogInfo("rcv",packet,startDate));
        int senderIsn = packet.getSEQ();
        if (packet.getSYN() == 1) {
            packet.setACK((byte) (packet.getSEQ() + 1));
            packet.setSEQ((byte) isn);
        }
        data = packet.toByteArray();
        ip = udpPacket.getAddress();
        this.port = udpPacket.getPort();
        udpPacket = new DatagramPacket(data, data.length, ip, this.port);
        Thread.sleep(1000);
        socket.send(udpPacket);
        writer.write(Helper.getLogInfo("snd",packet,startDate));
        //第三次握手
        socket.receive(udpPacket);
        System.out.println(Arrays.toString(data));
        packet = new Packet(data);
        writer.write(Helper.getLogInfo("rcv",packet,startDate));
        if (packet.getACK() != (isn + 1) || packet.getSEQ() != (senderIsn + 1)) {
            throw new RuntimeException("Reveiver第三次握手失败");
        }
    }

    public void saveData() throws IOException, InterruptedException {


        do {
            try {
                byte[] data = new byte[30];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                System.out.println(Arrays.toString(data));
                Packet stpPacket = new Packet(data);
                writer.write(Helper.getLogInfo("rcv",stpPacket,startDate));
                //第四次和第五次握手
                if (stpPacket.getFIN() != 0){
                    Packet finReply = new Packet((byte) 0,(byte) (stpPacket.getFIN() + 1),(byte) isn,(byte) 0);
                    System.out.println("第五次握手:" + Arrays.toString(finReply.toByteArray()));
                    socket.send(new DatagramPacket(finReply.toByteArray(),finReply.size(),ip,port));
                    writer.write(Helper.getLogInfo("snd",finReply,startDate));
                    break;
                }
                totalSements ++;
                buffer.add(stpPacket);
                stpPacket = new Packet((byte) 0, stpPacket.getSEQ(), (byte) 0, (byte) 0);
                data = stpPacket.toByteArray();
                packet = new DatagramPacket(data, data.length, ip, port);
                System.out.println("receiver sending :" + Arrays.toString(stpPacket.toByteArray()));
                socket.send(packet);
                writer.write(Helper.getLogInfo("snd",stpPacket,startDate));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (true);
        Packet[] stpPackets = buffer.toArray(new Packet[1]);
        Arrays.sort(stpPackets, new Comparator<Packet>() {
            public int compare(Packet o1, Packet o2) {
                return o1.getSEQ() - o2.getSEQ();
            }
        });
        RandomAccessFile out = new RandomAccessFile("data/output.txt", "rw");
        out.seek(out.length());
        for (Packet p : stpPackets) {
            totalBytes += p.getData().length;
            out.write(p.getData());
        }
        out.close();
    }

    public void handleClose() throws IOException {
        //第六次握手
        Packet finPacket = new Packet((byte) 0,(byte) 0,(byte) isn,(byte) 0);
        System.out.println("第六次握手:" + Arrays.toString(finPacket.toByteArray()));
        socket.send(new DatagramPacket(finPacket.toByteArray(),finPacket.size(),ip,port));
        writer.write(Helper.getLogInfo("snd",finPacket,startDate));
        byte[] reply = new byte[10];
        socket.receive(new DatagramPacket(reply,10));
        Packet finReply = new Packet(reply);
        if(finReply.getACK() == (byte) (isn + 1) && finReply.getFIN() != 0){
            System.out.println("第七次握手成功:" + Arrays.toString(reply));
            writer.write(Helper.getLogInfo("rcv",finReply,startDate));
        }else{
            System.out.println("第七次握手失败:" + Arrays.toString(reply));
        }
        socket.close();

        writer.write("Amount of ( original ) Data Received ( in bytes ) do not include retransmitted data : " + totalBytes + "\n"
                + "Number of ( original ) Data Segments Received : " + totalSements + "\n"
        );
        writer.flush();
        writer.close();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        int port = Integer.parseInt(args[0]);
        String file = args[1];
        Receiver receiver = new Receiver();
        receiver.waitAndConnect(port);
        receiver.saveData();
        receiver.handleClose();

    }
}
