package deviation;

import java.util.*;


public class TxInfo {
    private String model;
    private long id1;
    private long id2;
    private long id3;
    private Transmitter type;
    private final byte identifier[];
    public TxInfo() {
        model = null;
        type = Transmitter.DEVO_UNKNOWN;
        id1 = 0;
        id2 = 0;
        id3 = 0;
        identifier = null;
    }
    public TxInfo(byte [] data)
    {
    	identifier = data;
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
    public byte [] getIdentifier() { return identifier; }
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
    public static TxInfo getTxInfo(DfuDevice dev)
    {
        dev.SelectInterface(dev.Interfaces().get(0));
        if (dev.open() != 0) {
            System.out.println("Error: Unable to open device");
            return new TxInfo();
        }
        dev.claim_and_set();
        Dfu.setIdle(dev);
        byte [] txInfoBytes = Dfu.fetchFromDevice(dev, 0x08000400, 0x40);
        TxInfo txInfo = new TxInfo(txInfoBytes);
        dev.close();
        return txInfo;

    }
}
