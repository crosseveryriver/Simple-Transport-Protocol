package util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/12/5.
 */
public class Test {
    public static void main(String[] args) throws IOException {
        DatagramSocket socket = new DatagramSocket(8800);
        byte[] data = new byte[10];
        DatagramPacket packet = new DatagramPacket(data,data.length);
        while(true){
            socket.receive(packet);
            System.out.println(Arrays.toString(data));
        }
    }
}
