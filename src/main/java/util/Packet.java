package util;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/12/4.
 * 包含请求头和数据,放在DatagramPacket中
 */
public class Packet implements Serializable{
    int SYN = 0;
    int ACK = 0;
    int FIN = 0;
    String data = "一个数据包";

    @Override
    public String toString() {
        return  data;
    }
}
