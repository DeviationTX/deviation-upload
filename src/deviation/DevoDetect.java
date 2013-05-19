package deviation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import deviation.DevoDetect.Firmware;
import deviation.TxInfo.TxModel;

public class DevoDetect {
    public enum Firmware {UNKNOWN, DEVIATION, WALKERA};
    public enum Type {UNKNOWN, FIRMWARE, LIBRARY};
    private Firmware firmware;
    private Type type;
    private TxModel model;
    String version;
    public DevoDetect() {
        init();
    }
    private void init() {
        firmware = Firmware.UNKNOWN;
        type = Type.UNKNOWN;
        model = TxModel.DEVO_UNKNOWN;
        version = null;
    }
    public boolean Analyze(String id) {
        init();
        Matcher m;
        int idx;
        if ((idx = id.lastIndexOf("/")) > -1) {
            id = id.substring(idx+1);
        }
        if ((m = Pattern.compile("devo([^-]+)-(\\S+) Firmware").matcher(id)).matches()) {
            firmware = Firmware.DEVIATION;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            type = Type.FIRMWARE;
        } else if ((m = Pattern.compile("devo([^-]+)-(\\S+) Library").matcher(id)).matches()) {   
            firmware = Firmware.DEVIATION;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            type = Type.LIBRARY;
        } else if ((m = Pattern.compile("deviation-devo([^-]+)-(\\S+)\\.zip").matcher(id)).matches()) {
            firmware = Firmware.DEVIATION;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            type = Type.FIRMWARE;
        } else if ((m = Pattern.compile("deviation-fs-devo([^-]+)-(\\S+)\\.zip").matcher(id)).matches()) {
            firmware = Firmware.DEVIATION;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            type = Type.LIBRARY;
        } else if ((m = Pattern.compile("DEVO-([^-]+) FW\\S* (\\S+)", Pattern.CASE_INSENSITIVE).matcher(id)).matches()) {
            firmware = Firmware.WALKERA;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            type = Type.FIRMWARE;
        } else if ((m = Pattern.compile("DEVO-([^-]+) Lib\\S* (\\S+)", Pattern.CASE_INSENSITIVE).matcher(id)).matches()) {   
            firmware = Firmware.WALKERA;
            version = m.group(2);
            model = TxInfo.getModelFromString(m.group(1));
            type = Type.LIBRARY;
        }
        return (firmware == Firmware.UNKNOWN) ? false : true;
    }
    public boolean Found() {
        return (firmware == Firmware.UNKNOWN) ? false : true;
    }
    public String version() { return version; }
    public Type type() { return type; }
    public Firmware firmware() { return firmware; }
    public TxModel model() { return model; }
}
