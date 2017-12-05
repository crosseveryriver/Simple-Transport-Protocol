package sender;

import util.Packet;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by Administrator on 2017/12/4.
 * 命令行参数 127.0.0.1 8800 file.txt 200 3000 0.5 50
 */
public class Sender {

    private int isn = 123;
    private DatagramSocket socket;
    InetAddress ip;
    int port;

    byte[] fileData;
    byte[] data = new byte[30];
    byte front = 0;
    byte tail = 0;
    HashSet<Byte> set = new HashSet<Byte>();
    //建立连接
    public void connect(InetAddress ip, int port,int timeout) throws IOException, InterruptedException {
        this.ip = ip;
        this.port = port;

        socket = new DatagramSocket();
        Timer timer = new Timer();
        TimerTask task;

        //第一次握手
        Packet packet = new Packet((byte) 1, (byte) 0, (byte) 0, (byte) isn);
        byte[] data = packet.toByteArray();
        DatagramPacket udpPacket = new DatagramPacket(data,data.length,ip,port);
        socket.send(udpPacket);
        final DatagramPacket finalPacket = udpPacket;
        task = new TimerTask() {
            @Override
            public void run() {
                try {
                    socket.send(finalPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task,timeout,timeout);
        //第二次握手
        data = new byte[data.length];
        udpPacket = new DatagramPacket(data,data.length);
        socket.receive(udpPacket);
        task.cancel();
        timer.cancel();
        //第三次握手
        packet = new Packet(data);
        if(packet.getSYN() == 1 && packet.getACK() == (isn + 1)){
            packet.setSYN((byte) 0);
            packet.setACK((byte) (packet.getSEQ() + 1));
            packet.setSEQ((byte) (isn + 1));
        }else{
            throw new RuntimeException("Sender第二次握手失败");
        }
        data = packet.toByteArray();
        udpPacket = new DatagramPacket(data,data.length,ip,port);
        Thread.sleep(100);
        socket.send(udpPacket);
    }

    public void transferFile(final int mws){
        String str = "从c语言过来的程序员可定知道在写一些窗口程序的时候，如果要让程序暂停一段是将，那么直接引入windows.h头文件，然后在程序的任何地方写上Sleep(N)——N表示要暂停的毫秒数，就OK了，那么在java中如果要让程序暂停一段时间，使用线程中的sleep函数就能实现了。\n" +
                "示例代码：";
        fileData = str.getBytes();
        Thread listen = new Thread(new Runnable() {
            public void run() {
                try {
                    socket.receive(new DatagramPacket(data,data.length));
                    Packet packet = new Packet(data);
                        if(data[0] == 0){
                            synchronized (set){
                                if(set.contains(packet.getACK()))
                                    set.remove(packet.getACK());
                                front = Collections.min(set);
                            }
                        }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        listen.start();

        Thread send = new Thread(new Runnable() {
            public void run() {
                byte[] toSend;
                   while(true){
                       if ((tail - front) < (mws/20)){
                           System.out.println(mws/20);
                           if(tail * 20 < fileData.length){
                               int length = Math.min(20,fileData.length - tail * 20);
                               toSend = new byte[length];
                               System.arraycopy(fileData,tail * 20,toSend,0,length);
                               Packet packet = new Packet(toSend,(byte) tail);
                               packet.setSEQ(tail);
                               byte[] packetData = packet.toByteArray();
                               try {
                                   socket.send(new DatagramPacket(packetData,packetData.length,ip,port));
                                   synchronized (set){
                                       set.add(tail);
                                   }
                               } catch (IOException e) {
                                   e.printStackTrace();
                               }
                               tail ++;
                           }else {
                               break;
                           }
                       }else {
                           try {
                               System.out.println("未收到确认消息,暂停1s后继续");
                               Thread.sleep(1000);
                           } catch (InterruptedException e) {
                               e.printStackTrace();
                           }
                       }
                   }
            }
        });
        send.start();

    }

    public void close(){

        socket.close();
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        String[] addrstr = args[0].split("\\.");
        byte[] addrbyte = new byte[4];
        for (int i = 0; i < addrbyte.length; i++) {
            addrbyte[i] = Byte.parseByte(addrstr[i]);
        }
        Inet4Address ip = (Inet4Address) Inet4Address.getByAddress(addrbyte);
        int port = Integer.parseInt(args[1]);
        String file = args[2];
        int mws = Integer.parseInt(args[3]);
        int timeout = Integer.parseInt(args[4]);
        double pdrop = Double.parseDouble(args[5]);
        Random random = new Random(Integer.parseInt(args[6]));

        Sender sender = new Sender();
        sender.connect(ip,port,timeout);
        sender.transferFile(mws);
//        sender.close();

    }
}
