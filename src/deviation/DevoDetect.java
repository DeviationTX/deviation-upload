package deviation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DevoDetect {
    public enum Firmware {UNKNOWN, DEVIATION, WALKERA};
    private enum Type {UNKNOWN, FIRMWARE, LIBRARY, FIRMWARE_LIBRARY};
    private Firmware firmware;
    private Type type;
    private Transmitter model;
    String version;
    public DevoDetect() {
        init();
    }
    private void init() {
        firmware = Firmware.UNKNOWN;
        type = Type.UNKNOWN;
        model = TransmitterList.UNKNOWN();
        version = null;
    }
    public void update(DevoDetect item)
    {
    	if (firmware == Firmware.UNKNOWN)
    		firmware = item.firmware;
    	if (model.isUnknown())
    		model = item.model;
    	if (version == null)
    		version = item.version;
    	addRemoveType(item.type, true);
    	
    }
    public boolean Analyze(String id) {
        //init();
        Type thisType = Type.UNKNOWN;
        Matcher m;
        int idx;
        if ((idx = id.lastIndexOf("/")) > -1) {
            id = id.substring(idx+1);
        }
        if ((m = Pattern.compile("devo([^-]+)-(\\S+) Firmware").matcher(id)).matches()) {
            firmware = Firmware.DEVIATION;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            thisType = Type.FIRMWARE;
        } else if ((m = Pattern.compile("devo([^-]+)-(\\S+) Library").matcher(id)).matches()) {   
            firmware = Firmware.DEVIATION;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            thisType = Type.LIBRARY;
        } else if ((m = Pattern.compile("deviation-devo([^-]+)-(\\S+)\\.zip").matcher(id)).matches()) {
            firmware = Firmware.DEVIATION;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            thisType = Type.FIRMWARE;
        } else if ((m = Pattern.compile("deviation-fs-devo([^-]+)-(\\S+)\\.zip").matcher(id)).matches()) {
            firmware = Firmware.DEVIATION;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            thisType = Type.LIBRARY;
        } else if ((m = Pattern.compile("DEVO-([^-]+) FW\\S* (\\S+)", Pattern.CASE_INSENSITIVE).matcher(id)).matches()) {
            firmware = Firmware.WALKERA;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            thisType = Type.FIRMWARE;
        } else if ((m = Pattern.compile("DEVO-([^-]+) Lib\\S* (\\S+)", Pattern.CASE_INSENSITIVE).matcher(id)).matches()) {   
            firmware = Firmware.WALKERA;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            thisType = Type.LIBRARY;
        }
        if (thisType != Type.UNKNOWN) {
        	addRemoveType(thisType, true);
    	}
        return (firmware == Firmware.UNKNOWN) ? false : true;
    }
    private void addRemoveType(Type _type, boolean add) {
    	if (add) {
    		if (type == Type.FIRMWARE_LIBRARY)
    			return;
        	if ((_type == Type.FIRMWARE && type == Type.LIBRARY)
        			|| (_type == Type.LIBRARY && type == Type.FIRMWARE))
        	{
        		type = Type.FIRMWARE_LIBRARY;
        	} else {
        		type = _type;
        	}
    	} else {
        	if (type == Type.FIRMWARE_LIBRARY) {
        		if (_type == Type.FIRMWARE) {
        			type = Type.LIBRARY;
        		} else if (_type == Type.LIBRARY) {
        			type = Type.FIRMWARE;
        		}
        	} else if (type == _type) {
        		type = Type.UNKNOWN;
        	}
    	}
    }
    public boolean Found() {
        return (firmware == Firmware.UNKNOWN) ? false : true;
    }
    public String version() { return version; }
    public boolean isFirmware() { return (type == Type.FIRMWARE || type == Type.FIRMWARE_LIBRARY) ? true : false; }
    public boolean isFirmware(boolean add) { addRemoveType(Type.FIRMWARE, add); return add;}
    public boolean isLibrary() { return (type == Type.LIBRARY || type == Type.FIRMWARE_LIBRARY) ? true : false; }
    public boolean isLibrary(boolean add) { addRemoveType(Type.LIBRARY, add); return add;}
    public Firmware firmware() { return firmware; }
    public Transmitter model() { return model; }
}
