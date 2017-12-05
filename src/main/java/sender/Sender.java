package sender;

import util.Packet;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;

/**
 * Created by Administrator on 2017/12/4.
 * 命令行参数 127.0.0.1 8800 file.txt 200 3000 0.5 50
 */
public class Sender {

    //Sender_isn以及命令行参数
    private int isn = 123;
    private DatagramSocket socket;
    private InetAddress receiverIp;
    private int receiverPort;
    private String fileName;
    private int mws;
    private int timeout;
    private double pdrop;
    private int seed;

    private HashSet<DatagramPacket> toSendPackets = new HashSet<DatagramPacket>();
    private Thread sendModule = new Thread(new Runnable() {
        PLDModule module = new PLDModule(seed,pdrop);
        public void run() {
            while (true) {
                if(toSendPackets.size() == 0){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
                    DatagramPacket packet;
                    synchronized (toSendPackets){
                        packet = toSendPackets.iterator().next();
                        toSendPackets.remove(packet);
                    }
                    System.out.println("sending:" + Arrays.toString(packet.getData()));
                    try {
                        module.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        }
    });

    private ArrayList<DatagramPacket> reveivedPackets = new ArrayList<DatagramPacket>();
    private Thread receiveModule = new Thread(new Runnable() {
        public void run() {
            while (true) {
                DatagramPacket packet = new DatagramPacket(new byte[30], 30);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Packet stpPacket = new Packet(packet.getData());
                System.out.println("received packet:" + Arrays.toString(stpPacket.toByteArray()));
                if(stpPacket.getSYN() == 0 && stpPacket.getFIN() == 0 && stpPacket.getData() == null){
                    byte seq = stpPacket.getACK();
                    synchronized (transferingPackets){
                        if(transferingPackets.containsKey(seq)){
                            transferingPackets.get(seq).cancel();
                            transferingPackets.remove(seq);
                        }
                    }
                }else{
                    synchronized (reveivedPackets) {
                        reveivedPackets.add(packet);
                        reveivedPackets.notify();
                    }
                }

            }
        }
    });

    HashMap<Byte, TimerTask> transferingPackets = new HashMap<Byte, TimerTask>();

    byte[] fileData;
    byte[] data = new byte[30];
    byte front = 0;
    byte tail = 0;
    HashSet<Byte> set = new HashSet<Byte>();

    public Sender(InetAddress receiverIp, int receiverPort, String fileName, int mws, int timeout, double pdrop, int seed) {
        this.receiverIp = receiverIp;
        this.receiverPort = receiverPort;
        this.fileName = fileName;
        this.mws = mws;
        this.timeout = timeout;
        this.pdrop = pdrop;
        this.seed = seed;
    }

    public void initialize() throws SocketException {
        socket = new DatagramSocket();
        receiveModule.start();
        sendModule.start();
    }

    public void threeHandshake() throws InterruptedException {
        firstHandShake();
        byte receiverIsn = secondHandShake();
        if (receiverIsn != 0) {
            thirdHandshake(receiverIsn);
        }

    }

    public void firstHandShake() {
        Packet stpPacket = new Packet((byte) 1, (byte) 0, (byte) 0, (byte) isn);
        byte[] data = stpPacket.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, receiverIp, receiverPort);
        synchronized (toSendPackets) {
            toSendPackets.add(packet);
            toSendPackets.notify();
        }
        System.out.println("第一次握手" + Arrays.toString(stpPacket.toByteArray()));
    }

    public byte secondHandShake() throws InterruptedException {
        byte receiverIsn = 0;
        synchronized (reveivedPackets) {
            reveivedPackets.wait();
            if (reveivedPackets.size() == 0)
                throw new RuntimeException("第二次握手失败");
            for (DatagramPacket p : reveivedPackets) {
                Packet stpp = new Packet(p.getData());
                if (stpp.getSYN() == 1 && stpp.getACK() == (isn + 1)) {
                    receiverIsn = stpp.getSEQ();
                    System.out.println("第二次握手" + Arrays.toString(stpp.toByteArray()));
                    break;
                }
            }
        }
        return receiverIsn;
    }

    public void thirdHandshake(byte receiverIsn) {
        Packet stpPacket = new Packet((byte) 0, (byte) (receiverIsn + 1), (byte) 0, (byte) (isn + 1));
        byte[] data = stpPacket.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, receiverIp, receiverPort);
        synchronized (toSendPackets) {
            toSendPackets.add(packet);
            toSendPackets.notify();
        }
        System.out.println("第三次握手" + Arrays.toString(stpPacket.toByteArray()));

    }


    public void transferFile() throws IOException, InterruptedException {
        FileInputStream in = new FileInputStream(new File("data/input.txt"));
        byte[] data = new byte[20];
        int length = 0;
        byte seq = 0;
        Timer timer = new Timer();
        while ((length = in.read(data)) != -1) {
            Packet stpPacket = new Packet(data, length, seq);
            seq++;
            byte[] tmp = stpPacket.toByteArray();
            final DatagramPacket packet = new DatagramPacket(tmp, tmp.length, receiverIp, receiverPort);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    synchronized (toSendPackets) {
                        toSendPackets.add(packet);
                        toSendPackets.notify();
                    }
                }
            };
            synchronized (transferingPackets){
                transferingPackets.put(stpPacket.getSEQ(),task);
                timer.schedule(task,0,timeout);
            }
        }
        while (true){
            synchronized (transferingPackets){
                if(transferingPackets.size() > 0){
                    Thread.sleep(2000);
                }else {
                    break;
                }
            }
        }
    }

    public void close() {
        fourHandshake();
//        socket.close();
    }

    public void fourHandshake() {
        fourthHandshake();
        fifthHandshake();
        sixthHandshake();
        seventhHandshake();
    }

    public void fourthHandshake() {
        Packet packet = new Packet((byte) 0, (byte) 0, (byte) 1, (byte) isn);
        byte[] data = packet.toByteArray();
        DatagramPacket udpPacket = new DatagramPacket(data, data.length, receiverIp, receiverPort);
        synchronized (toSendPackets) {
            toSendPackets.add(udpPacket);
            toSendPackets.notify();
        }
    }

    public void fifthHandshake() {
    }

    public void sixthHandshake() {
    }

    public void seventhHandshake() {
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
        int seed = Integer.parseInt(args[6]);

        Sender sender = new Sender(ip, port, file, mws, timeout, pdrop, seed);
        sender.initialize();
        sender.threeHandshake();
        sender.transferFile();
        sender.close();

    }

    private boolean packetContainsData(DatagramPacket packet) {
        return packet.getData().length > 10;
    }

    class PLDModule{
        private Random random;
        private double pdrop;
        PLDModule(int seed,double pdrop){
            this.random = new Random(seed);
            this.pdrop = pdrop;
        }
        public void send(DatagramPacket packet) throws IOException {
            if(new Packet(packet.getData()).getData() != null && random.nextDouble() < pdrop){
                //丢失的数据包
            }else{
                socket.send(packet);
            }
        }
    }

}
