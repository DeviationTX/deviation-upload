package deviation;

import java.util.zip.ZipEntry;

public class FileInfo {
    private int size;
    private String name;
    private byte[]data;
    private long time;
    private long Crc;

    public int size() { return size; }
    public String name() { return name; }
    public long Crc() {return Crc; }
    public long time() { return time; }
    public byte[] data() { return data; }

    public FileInfo(ZipEntry ze, byte[] data) {
        size = (int)ze.getSize();
        name = ze.getName();
        time= ze.getTime();
        Crc = ze.getCrc();
        this.data = data;        
    }
}
