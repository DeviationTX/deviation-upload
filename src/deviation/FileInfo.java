package deviation;

import java.util.zip.ZipEntry;

public class FileInfo {
    private int size;
    private String name;
    private String owner;
    private byte[]data;
    private long time;
    private long Crc;

    public int size() { return size; }
    public String name() { return name; }
    public long Crc() {return Crc; }
    public long time() { return time; }
    public byte[] data() { return data; }
    public void setOwner(String owner) { this.owner = owner; }
    public String owner() { return owner; }

    public FileInfo(ZipEntry ze, byte[] data) {
        size = (int)ze.getSize();
        name = ze.getName().toUpperCase();
        time= ze.getTime();
        Crc = ze.getCrc();
        owner = null;
        this.data = data;        
    }
    public FileInfo(String name, int size) {
    	this.size = size;
    	this.name = name;
    }
}
