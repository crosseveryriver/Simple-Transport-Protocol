package util;

import com.sun.org.apache.xpath.internal.operations.Bool;
import receiver.Receiver;
import sender.Sender;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/12/5.
 */
public class Test {
    int x = 10;
    ConcurrentHashMap<Integer,Boolean> map;

    public static void main(String[] args) throws IOException, InterruptedException {
        Test t = new Test();
        t.testTransfer();

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
        String str = "从c语言过来的程序员可定知道在写一些窗口程序的时候，如果要让程序暂停一段是将，那么直接引入windows.h头文件，然后在程序的任何地方写上Sleep(N)——N表示要暂停的毫秒数，就OK了，那么在java中如果要让程序暂停一段时间，使用线程中的sleep函数就能实现了。\n" +
                "示例代码：";
        byte[] fileData = str.getBytes();
        System.out.println(fileData.length);
        System.out.println(Arrays.toString(fileData));
    }
}
