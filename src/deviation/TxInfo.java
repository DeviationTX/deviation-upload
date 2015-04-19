package deviation;

import java.util.*;


public class TxInfo {
    private String model;
    private long id1;
    private long id2;
    private long id3;
    private Transmitter type;
    public TxInfo() {
        model = null;
        type = Transmitter.DEVO_UNKNOWN;
        id1 = 0;
        id2 = 0;
        id3 = 0;
    }
    public TxInfo(byte [] data)
    {
        model = new String(Arrays.copyOfRange(data, 0, 32));
        type = Transmitter.DEVO_UNKNOWN;
        txloop:
        for (Transmitter tx : Transmitter.values()) {
        	byte [] match = tx.getId().getBytes();
        	if (match.length == 0)
        		continue;
        	for (int i = 0; i < match.length; i++) {
        		if (match[i] != data[8+i]) {
        			continue txloop;
        		}
        	}
        	type = tx;
        }
        id1 = (0xff & data[0x22]) | ((0xff & data[0x24]) << 8) | ((0xff & data[0x26]) << 16) | ((long)(0xff & data[0x28]) << 24);
        id2 = (0xff & data[0x2a]) | ((0xff & data[0x2c]) << 8) | ((0xff & data[0x2e]) << 16) | ((long)(0xff & data[0x30]) << 24);
        id3 = (0xff & data[0x32]) | ((0xff & data[0x34]) << 8) | ((0xff & data[0x36]) << 16) | ((long)(0xff & data[0x38]) << 24);
    }
    public String model() { return model;}
    public long id1() { return id1;}
    public long id2() { return id2;}
    public long id3() { return id3;}
    public Transmitter type() { return type;}
    public byte [] encodeId() { return type.encode(id1,  id2,  id3); }
    public boolean matchModel(Transmitter t)
    {
        if (type == Transmitter.DEVO_UNKNOWN || type != t) {
            return false;
        }
        return true;
    }
    public static Transmitter getModelFromString(String str) {
    	for (Transmitter tx : Transmitter.values()) {
    		if (tx.modelMatch(str)) {
    			return tx;
    		}
    	}
        return Transmitter.DEVO_UNKNOWN;
    }
    public static String typeToString(Transmitter type)
    {
    	return type.getName();
    }
}
