package util;

import java.net.DatagramPacket;
import java.util.Date;

/**
 * Created by Administrator on 2017/12/4.
 */
public class Helper {
    public static String getLogInfo(String sndOrRcvOrDrop,Packet stpPacket, Date startDate){
        double time = (new Date().getTime() - startDate.getTime()) / 1000.0;
        String packetType = getPacketType(stpPacket);
        byte seq = stpPacket.getSEQ();
        int numOfBytes = stpPacket.getData() == null ? 0 : stpPacket.getData().length;
        byte ack = stpPacket.getACK();
        return sndOrRcvOrDrop+ "\t"+time+"\t"+packetType+"\t"+seq+"\t"+numOfBytes+"\t" + ack + "\t\n";
    }

    public  static String getPacketType(Packet packet){
        if(packet.getData() == null){
            if(packet.getSYN() == 0 && packet.getSEQ() != 0 && packet.getACK() != 0)
                return "SA";
            if(packet.getSYN() != 0){
                if(packet.getACK() != 0){
                    return "SA";
                }else {
                    return "S";
                }
            }else if(packet.getFIN() != 0){
                if(packet.getACK() != 0){
                    return "FA";
                }else {
                    return "F";
                }
            }else if(packet.getACK() >= 0){
                return "A";
            }else {
                return "unrecognized";
            }
        }else {
            return "D";
        }
    }
}
