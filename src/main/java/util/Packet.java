package util;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Administrator on 2017/12/4.
 * 包含请求头和数据,放在DatagramPacket中
 */
public class Packet implements Serializable{
    private byte SYN = 0;
    private byte ACK = 0;
    private byte FIN = 0;
    private byte SEQ = 0;
    private byte[] data;

    public Packet(byte[] data,int length,byte seq){
        byte[] tmp = new byte[length];
        System.arraycopy(data,0,tmp,0,length);
        if(tmp.length > 20)
            throw new RuntimeException("STP packet data too long");
        this.data = tmp;
        this.SEQ = seq;
    }
    public Packet(byte[] data,byte seq){
        if(data.length > 20)
            throw new RuntimeException("STP packet data too long");
       this.data = data;
       this.SEQ = seq;
    }

    public Packet(byte syn, byte ack, byte fin, byte seq){
        SYN = syn;
        ACK = ack;
        FIN = fin;
        SEQ = seq;
    }

    public Packet(byte[] packet){
        if(packet[0] > 0){
            data = new byte[20];
            System.arraycopy(packet,10,data,0,packet[0]);
        }
        SYN = packet[1];
        ACK = packet[2];
        FIN = packet[3];
        SEQ = packet[4];
    }

    public byte getSEQ() {
        return SEQ;
    }

    public void setSEQ(byte SEQ) {
        this.SEQ = SEQ;
    }

    public byte getSYN() {
        return SYN;
    }

    public void setSYN(byte SYN) {
        this.SYN = SYN;
    }

    public byte getACK() {
        return ACK;
    }

    public void setACK(byte ACK) {
        this.ACK = ACK;
    }

    public byte getFIN() {
        return FIN;
    }

    public void setFIN(byte FIN) {
        this.FIN = FIN;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
    @Override
    public String toString() {
        return  data.toString();
    }

    public boolean isConnectRequest(){
        //TODO
        return true;
    }

    public byte[] toByteArray(){
        byte[] result;
        if(data == null){
            result = new byte[10];
        }else {
            result = new byte[30];
            System.arraycopy(data,0,result,10,data.length);
        }
        result[0] = (data == null) ? 0 : (byte) data.length;
        result[1] = SYN;
        result[2] = ACK;
        result[3] = FIN;
        result[4] = SEQ;
        return result;
    }

    public int size(){
        return (data == null ? 0 : data.length) + 10;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Packet packet = (Packet) o;

        if (SYN != packet.SYN) return false;
        if (ACK != packet.ACK) return false;
        if (FIN != packet.FIN) return false;
        if (SEQ != packet.SEQ) return false;
        return Arrays.equals(data, packet.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
