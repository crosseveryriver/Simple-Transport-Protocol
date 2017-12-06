package util;



import javafx.scene.chart.PieChart;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/12/5.
 */
public class Test {
    int x = 10;
    ConcurrentHashMap<Integer,Boolean> map;

    public static void main(String[] args) throws IOException, InterruptedException {
        Test t = new Test();
        Random random = new Random(50);
        for (int i = 0; i < 20; i++) {
            System.out.println(random.nextDouble());

        }
    }

    public void testTime() throws InterruptedException {
        Date date = new Date();
        Thread.sleep(1500);
        System.out.println((new Date().getTime() - date.getTime())/1000.0);
    }

    public void testFileWriter() throws IOException {
        FileWriter writer = new FileWriter("data/log.txt");
        for (int i = 0; i < 10; i++) {
            writer.write("a\tbsssssssssssssss\tc\t\n");
        }
        for (int i = 0; i < 10; i++) {
            writer.write("a\tbss\tc\t\n");
        }
        writer.close();
    }

    public void fileByte() throws IOException {
        FileInputStream in = new FileInputStream("data/input.txt");
        byte[] bytes = new byte[20];
        for(int i =0;i < 25;i ++){
            in.read(bytes,0,20);
            System.out.println(i + ":" + Arrays.toString(bytes));
        }

    }

    public void testSocket() throws IOException, InterruptedException {
        DatagramSocket socket1 = new DatagramSocket();
        DatagramSocket socket2 = new DatagramSocket(8900);
        InetAddress ip = InetAddress.getByName("localhost");
        int port = 8900;
        byte[] x = new byte[]{1};
        socket1.send(new DatagramPacket(x,1,ip,port));
        x[0] ++;
        socket1.send(new DatagramPacket(x,1,ip,port));
        socket1.close();
        Thread.sleep(3000);
        DatagramPacket packet = new DatagramPacket(new byte[10],10);

        socket2.receive(packet);
        System.out.println(Arrays.toString(packet.getData()));
        socket2.receive(packet);
        System.out.println(Arrays.toString(packet.getData()));

    }

    public void testHashSet(){
        HashSet<Packet> packets = new HashSet<Packet>();
        Packet p = new Packet((byte) 1,(byte) 1,(byte) 1,(byte) 1);
        Packet p2 = new Packet((byte) 1,(byte) 1,(byte) 1,(byte) 0);
        for (int i = 0; i < 10; i++) {
            packets.add(p);
        }
        for (int i = 0; i < 10; i++) {
            packets.add(p2);
        }
        System.out.println(packets.size());
    }

    public void fileTest() throws IOException {
        FileInputStream in = new FileInputStream(new File("data/input.txt"));
        int tmpByte;
        ArrayList<Byte> bytes = new ArrayList<Byte>();

        RandomAccessFile out = new RandomAccessFile("data/output.txt","rw");
        long length = out.length();
        out.seek(length);

        while((tmpByte = in.read()) != -1){
           out.write(tmpByte);
        }

        in.close();
        out.close();

    }

    public void listenPort() throws IOException {
        DatagramSocket socket = new DatagramSocket(8800);
        byte[] data = new byte[10];
        DatagramPacket packet = new DatagramPacket(data,data.length);
        while(true){
            socket.receive(packet);
            System.out.println(Arrays.toString(data));
        }
    }


    public void testMutiThreadShare(){
        Timer timer  = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                x += 100;
            }
        };
        TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                System.out.println(x);
            }
        };

        timer.schedule(task,100,3000);
        timer.schedule(task1,200,1000);
    }

    public void testTransfer() throws IOException, InterruptedException {
        String str = "从c语言过来的程序员可定知道在写一些窗口程序的时候，如果要让程序暂停一段是将，那么直接引入windows.h头文件，然后在程序的任何地方写上Sleep(N)——N表示要暂停的毫秒数，就OK了，那么在java中如果要让程序暂停一段时间，使用线程中的sleep函数就能实现了";
        byte[] fileData = str.getBytes();
        System.out.println(fileData.length);
        System.out.println(Arrays.toString(fileData));
    }

    private byte[] toPrimitives(Byte[] oBytes)
    {
        byte[] bytes = new byte[oBytes.length];

        for(int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }

        return bytes;
    }
}
