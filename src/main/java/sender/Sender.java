package sender;

import util.Constants;
import util.Helper;
import util.Packet;

import java.io.*;
import java.net.*;
import java.util.*;
import static util.Constants.LENGTH;

/**
 * Created by Administrator on 2017/12/4.
 * 命令行参数 127.0.0.1 8800 data/input.txt 103400 10340 30 0.2 50
 */
public class Sender {

    //定义常量
//    private final int LENGTH = 1034;
    private final int DATA_LENGTH=LENGTH-10;

    //计算timeout
    volatile int packetCount = 0;
    volatile long myTimeout = 0;


    //记录日志文件相关信息
    FileWriter writer;
    Date startDate = new Date();
    volatile long totalBytes;
    volatile int dataSementsSent;
    volatile int packetsDroped;
    volatile int retranSements;
    volatile int dupAcks;


    //Sender_isn以及命令行参数
    private int isn = 123;
    private DatagramSocket socket;
    private InetAddress receiverIp;
    private int receiverPort;
    private String fileName;
    private int mws;
    volatile private int timeout;
    private double pdrop;
    private int seed;

    //是否停止接受模块,发送模块
    private boolean stopSendModule = false;
    private boolean stopReceiveModule = false;

    //待发送的Packet,当该集合不为空的时候,sendModule选择其中的一个Packet发送给Receiver
    private HashSet<Packet> toSendPackets = new HashSet<Packet>();
    //专门用来发送数据的线程
    private Thread sendModule = new Thread(new Runnable() {
        public void run() {
            PLDModule module = new PLDModule(seed,pdrop);
            while (!stopSendModule) {
                if(toSendPackets.size() == 0){
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else{
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    DatagramPacket packet;
                    Packet stpPacket;
                    synchronized (toSendPackets){
                        stpPacket = toSendPackets.iterator().next();
                        toSendPackets.remove(stpPacket);
                    }
                    packet = new DatagramPacket(stpPacket.toByteArray(),stpPacket.size(),receiverIp,receiverPort);
                    try {
                        //使用PLD模块模拟发送数据报,有可能会drop,也有可能发送成功
                        module.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            packetsDroped = module.getTotalDataSementsDroped();
            retranSements = module.getTotalDataSementsSent() - dataSementsSent;
            System.out.println("dropPackets : " + packetsDroped + "sent" + module.getTotalDataSementsSent());
        }
    });

    //接收到的Packet,这个链表中只保存和握手相关的Packet,保存之后,由主线程处理相关数据.和握手无关的Packet在receiveModule中已经处理过了,因此不保存在这个链表中
    private ArrayList<DatagramPacket> receivedPackets = new ArrayList<DatagramPacket>();
    //接受receiver发送数据的模块
    private Thread receiveModule = new Thread(new Runnable() {
        int totalAcks = 0;
        public void run() {
            while (!stopReceiveModule) {
                DatagramPacket packet = new DatagramPacket(new byte[LENGTH], LENGTH);
                try {
                    socket.receive(packet);
                    Packet stpPacket = new Packet(packet.getData());
                    System.out.println("received packet:" + Arrays.toString(stpPacket.toByteArray()));
                    try {
                        writer.write(Helper.getLogInfo("rcv",stpPacket,startDate));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(stpPacket.getSYN() == 0 && stpPacket.getFIN() == 0 && stpPacket.getData() == null){
                        totalAcks ++;
                        MyTask task;
                        byte min;
                        byte seq = stpPacket.getACK();
                        //接收到数据报文确认消息后,将该报文从transferingPackets中删除,并取消与保温对应的计时器
                        synchronized (transferingPackets){
                            task = transferingPackets.remove(seq);
                            try{
                                min = Collections.min(transferingPackets.keySet());
                            }catch (NoSuchElementException e){
                                min = tail;
                            }
                        }
                        front = min;
                        if(task != null){
                            long tmp = task.getTimeout();
                            myTimeout = ((myTimeout * packetCount) + tmp)/ (packetCount + 1);
                            packetCount ++;
                            //更改timeout策略,应用下面一行代码即可根据数据报平均往返时间自动更改并应用timeout值
//                            timeout = (int) myTimeout;
                            System.out.println("curent timeout :" + myTimeout );
                            task.cancel();
                        }
                    }else{
                        synchronized (receivedPackets) {
                            receivedPackets.add(packet);
                            receivedPackets.notify();
                        }
                    }
                    System.out.println("ACK已确认");
                } catch (IOException e) {
                }
            }
            dupAcks = totalAcks - dataSementsSent;
        }
    });

    HashMap<Byte, MyTask> transferingPackets = new HashMap<Byte, MyTask>();

    byte front;
    byte tail;
    HashSet<Byte> set = new HashSet<Byte>();

    public Sender(InetAddress receiverIp, int receiverPort, String fileName, int mws, int timeout, double pdrop, int seed) throws IOException {
        this.receiverIp = receiverIp;
        this.receiverPort = receiverPort;
        this.fileName = fileName;
        this.mws = mws;
        this.timeout = timeout;
        this.pdrop = pdrop;
        this.seed = seed;
        writer = new FileWriter("data/Sender_log.txt");
    }

    //初始化连接,启动相关线程,准备文件读写
    public void initialize() throws IOException {
        socket = new DatagramSocket();
        receiveModule.start();
        sendModule.start();
        writer.write("<snd/rcv/drop>\t<time>\t<type of packet>\t<seq>\t<number of bytes>\t<ack-number>\t\n");
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
        synchronized (toSendPackets) {
            toSendPackets.add(stpPacket);
            toSendPackets.notify();
        }
        System.out.println("第一次握手" + Arrays.toString(stpPacket.toByteArray()));
    }

    public byte secondHandShake() throws InterruptedException {
        byte receiverIsn = 0;
        synchronized (receivedPackets) {
            receivedPackets.wait();
            if (receivedPackets.size() == 0)
                throw new RuntimeException("第二次握手失败");
            for (DatagramPacket p : receivedPackets) {
                Packet stpp = new Packet(p.getData());
                if (stpp.getSYN() == 1 && stpp.getACK() == (isn + 1)) {
                    receiverIsn = stpp.getSEQ();
                    System.out.println("第二次握手" + Arrays.toString(stpp.toByteArray()));
                    break;
                }
            }
            receivedPackets.clear();
        }
        return receiverIsn;
    }

    public void thirdHandshake(byte receiverIsn) throws InterruptedException {
        Packet stpPacket = new Packet((byte) 0, (byte) (receiverIsn + 1), (byte) 0, (byte) (isn + 1));
        synchronized (toSendPackets) {
            toSendPackets.add(stpPacket);
            toSendPackets.notify();
        }
        System.out.println("第三次握手" + Arrays.toString(stpPacket.toByteArray()));
        Thread.sleep(40);

    }

    //三次握手后,开始发送数据
    public void transferFile() throws IOException, InterruptedException {
        File inputFile = new File(fileName);
        totalBytes = inputFile.length();
        FileInputStream in = new FileInputStream(inputFile);
        byte[] data = new byte[DATA_LENGTH];
        int length;
        front = 0;
        tail = 0;
        Timer timer = new Timer();
        //循环读取输入文件,如果不超过窗口大小就将数据封装之后,添加到toSendPackets中,等待sendModule发送数据;
        // 封装的同时会为每个数据报启动一个计时器,如果超过timeout时间之后,就在此向toSendPackets中加入数据报;
        //添加到toSendPackets的同时也会在transferingPackets中添加相应的报文和计时器的映射
        while ((length = in.read(data)) != -1) {
            while((tail - front) > mws/DATA_LENGTH){
                System.out.println("超出窗口大小,暂时停止发送");
                Thread.sleep(2);
            }
            final Packet stpPacket = new Packet(data, length, tail);
            tail++;
            MyTask task = new MyTask(stpPacket,new Date().getTime());
            synchronized (transferingPackets){
                transferingPackets.put(stpPacket.getSEQ(),task);
                timer.schedule(task,0,timeout);
            }
            Thread.sleep(1);
        }
        dataSementsSent = tail;
        Thread.sleep(2);
        while (true){
            boolean sleep;
            synchronized (transferingPackets){
                sleep = transferingPackets.size() > 0;
            }
            if(sleep){
                Thread.sleep(4);
            }else {
                break;
            }
        }
        synchronized (toSendPackets){
            toSendPackets.clear();
        }
        timer.cancel();
    }

    public void fourHandshake() throws InterruptedException {
        fourthHandshake();
        fifthHandshake();
        byte receiverIsn = sixthHandshake();
        seventhHandshake(receiverIsn);
    }

    public void fourthHandshake() {
        Packet packet = new Packet((byte) 0, (byte) 0, (byte) isn, (byte) 0);
        synchronized (toSendPackets) {
            toSendPackets.add(packet);
            toSendPackets.notify();
        }
    }

    public void fifthHandshake() throws InterruptedException {
        synchronized (receivedPackets) {
            receivedPackets.wait();
            if (receivedPackets.size() == 0)
                throw new RuntimeException("第五次握手失败");
            for (DatagramPacket p : receivedPackets) {
                Packet stpp = new Packet(p.getData());
                if (stpp.getFIN() != 0 && stpp.getACK() == (isn + 1) ) {
                    System.out.println("第五次握手" + Arrays.toString(stpp.toByteArray()));
                    break;
                }
            }
            receivedPackets.clear();
        }
    }

    public byte sixthHandshake() throws InterruptedException {
        byte receiverIsn = 0;
        synchronized (receivedPackets) {
            receivedPackets.wait();
            if (receivedPackets.size() == 0)
                throw new RuntimeException("第六次握手失败");
            for (DatagramPacket p : receivedPackets) {
                Packet stpp = new Packet(p.getData());
                if (stpp.getFIN() != 0 ) {
                    receiverIsn = stpp.getFIN();
                    System.out.println("第六次握手" + Arrays.toString(stpp.toByteArray()));
                    break;
                }
            }
            receivedPackets.clear();
        }
        return receiverIsn;
    }

    public void seventhHandshake(byte receiverIsn) {
        Packet packet = new Packet((byte) 0, (byte) (receiverIsn + 1), (byte) isn, (byte) 0);
        synchronized (toSendPackets) {
            toSendPackets.add(packet);
            toSendPackets.notify();
        }
    }

    public void close() throws InterruptedException, IOException {
        fourHandshake();
        Thread.sleep(20);
        stopReceiveModule = true;
        stopSendModule = true;
        int sleepTimes = 0;
        while (sendModule.isAlive() && (sleepTimes > 3)){
            sleepTimes ++;
            Thread.sleep(20);
        }
        socket.close();
        while (receiveModule.isAlive()){
            Thread.sleep(20);
        }
        writer.write("Amount(Original) Data Transfered(in bytes) : "+ totalBytes + "\n"
                + "Number of Data segments sent(excluding retransmissions) : " + dataSementsSent + "\n"
                + "Number of (all) Packets Droped (by PLD Module) : " + packetsDroped + "\n"
                + "Number of Retransmitted segments : " + retranSements + "\n"
                + "Number of Duplicate Acknowledgements Received : " + dupAcks + "\n"
        );
        writer.flush();
        writer.close();
    }
    class PLDModule{
        private Random random;
        private double pdrop;
        //四条发送的握手信息,所以初始值为-4
        int totalDataSementsSent = -4;
        int totalDataSementsDroped = 0;
        PLDModule(int seed,double pdrop){
            this.random = new Random(seed);
            this.pdrop = pdrop;
        }

        public  int getTotalDataSementsSent() {
            return totalDataSementsSent;
        }
        public int getTotalDataSementsDroped() {
            return totalDataSementsDroped;
        }
        //以一定的概率drop数据报
        public void send(DatagramPacket packet) throws IOException {
            totalDataSementsSent ++;
            Packet stpPacket = new Packet(packet.getData());
            String sndOrDrop;
            if(stpPacket.getData() != null && random.nextDouble() < pdrop){
                sndOrDrop = "drop";
                System.out.println("drop:" + Arrays.toString(packet.getData()));
                totalDataSementsDroped ++;
            }else{
                sndOrDrop = "snd";
                System.out.println("sending:" + Arrays.toString(packet.getData()));
                socket.send(packet);
            }
            writer.write(Helper.getLogInfo(sndOrDrop,stpPacket,startDate));
        }

    }

    private double getCurrentTimeInSeconds(){
        return (new Date().getTime() - startDate.getTime()) / 1000.0;
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        String[] addrstr = args[0].split("\\.");
        byte[] addrbyte = new byte[4];
        for (int i = 0; i < addrbyte.length; i++) {
            addrbyte[i] = (byte) Integer.parseInt(addrstr[i]);
        }
        Inet4Address ip = (Inet4Address) Inet4Address.getByAddress(addrbyte);
        int port = Integer.parseInt(args[1]);
        String file = args[2];
        int mws = Integer.parseInt(args[3]);
        int mss = Integer.parseInt(args[4]);
        Constants.LENGTH = mss;
        int timeout = Integer.parseInt(args[5]);
        double pdrop = Double.parseDouble(args[6]);
        int seed = Integer.parseInt(args[7]);

        Sender sender = new Sender(ip, port, file, mws, timeout, pdrop, seed);
        sender.initialize();
        sender.threeHandshake();
        sender.transferFile();
        sender.close();

    }


    class MyTask extends TimerTask{
        private Packet stpPacket;
        private long begin;
        public MyTask(Packet stpPacket,long begin){
            this.stpPacket = stpPacket;
            this.begin = begin;
        }

        public void run() {
            synchronized (toSendPackets) {
                toSendPackets.add(stpPacket);
            }
        }

        public long getTimeout(){
            return new Date().getTime() - begin;
        }
    }
}
