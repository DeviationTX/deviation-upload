import java.io.*;
import java.util.*;


public class TxInfo {
    private String model;
    private long id1;
    private long id2;
    private long id3;
    private enum TxType {DEVO_UNKNOWN, DEVO6, DEVO7e, DEVO8, DEVO10, DEVO12};
    private TxType type;
    public TxInfo(byte [] data)
    {
        model = new String(Arrays.copyOfRange(data, 0, 32));
        type = TxType.DEVO_UNKNOWN;
        if (data[8] == 'D' && data[9] == 'E' && data[10] == 'V' && data[11] == 'O' && data[12] == '-') {
            if        (data[13] == '0' && data[14] == '6') {
                type = TxType.DEVO6;
            } else if (data[13] == '7' && data[14] == 'e') {
                type = TxType.DEVO7e;
            } else if (data[13] == '0' && data[14] == '8') {
                type = TxType.DEVO8;
            } else if (data[13] == '1' && data[14] == '0') {
                type = TxType.DEVO10;
            } else if (data[13] == '1' && data[14] == '2') {
                type = TxType.DEVO12;
            }
        }
        id1 = (0xff & data[0x22]) | ((0xff & data[0x24]) << 8) | ((0xff & data[0x26]) << 16) | ((long)(0xff & data[0x28]) << 24);
        id2 = (0xff & data[0x2a]) | ((0xff & data[0x2c]) << 8) | ((0xff & data[0x2e]) << 16) | ((long)(0xff & data[0x30]) << 24);
        id3 = (0xff & data[0x32]) | ((0xff & data[0x34]) << 8) | ((0xff & data[0x36]) << 16) | ((long)(0xff & data[0x38]) << 24);
    }
    public String model() { return model;}
    public long id1() { return id1;}
    public long id2() { return id2;}
    public long id3() { return id3;}
    private byte[] convertToByteArray(long[] data, int offset) {
        byte []buf = new byte[16];
        int i = 0;
        for (long  val : data) {
            buf[i++] = (byte)((val >>  0) & 0xff);
            buf[i++] = (byte)((val >>  8) & 0xff);
            buf[i++] = (byte)((val >> 16) & 0xff);
            buf[i++] = (byte)((val >> 24) & 0xff);
        }
        for (i = 0; i < buf.length; i++) {
            int val = buf[i] & 0xff;
            if(val >= 0x80 && val <= 0xcf - offset) {
                val += offset;
            } else if(val >= 0xd0 - offset && val < 0xd0) {
                val -= (0x50 - offset);
            }
            buf[i] = (byte)val;
        }
        return buf;
    }
    private byte[] encode_7e() {
        long[] a = new long[4];
        a[0] = 0xFFFFFFFFL & (id1 + id2);
        a[1] = 0xFFFFFFFFL & (id2 ^ id2);
        a[2] = 0xFFFFFFFFL & (id1 + id3);
        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
        return convertToByteArray(a, 7);
    }
    private byte[] encode_6_8() {
        long[] a = new long[4];
        a[0] = 0xFFFFFFFFL & (id1 + id2);
        a[1] = 0xFFFFFFFFL & (id1 ^ id2);
        a[2] = 0xFFFFFFFFL & (id1 + id3);
        a[3] = 0xFFFFFFFFL & (id1 ^ id3);
        return convertToByteArray(a, 8);
    }
    private byte[] encode_10() {
        long[] a = new long[4];
        a[0] = 0xFFFFFFFFL & (id1 + id2);
        a[1] = 0xFFFFFFFFL & (id1 ^ id2);
        a[2] = 0xFFFFFFFFL & (id2 + id3);
        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
        return convertToByteArray(a, 10);
    }
    private byte[] encode_12() {
        long[] a = new long[4];
        a[0] = 0xFFFFFFFFL & (id1 + id2 + id3);
        a[1] = 0xFFFFFFFFL & (id1 ^ id2 ^ id3);
        a[2] = 0xFFFFFFFFL & (id2 + id3);
        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
        return convertToByteArray(a, 12);
    }
    public byte [] encodeId() {
        switch (type) {
            case DEVO6:  return encode_6_8();
            case DEVO7e: return encode_7e();
            case DEVO8:  return encode_6_8();
            case DEVO10: return encode_10();
            case DEVO12: return encode_12();
            default:     return null;
        }
    }
}
