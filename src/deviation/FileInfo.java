package deviation;

import java.util.Arrays;
import java.util.zip.ZipEntry;

public class FileInfo {
    private int size;
    private String name;
    private String owner;
    private byte[]data;
    private long time;
    private long Crc;
    private boolean deleted;

    public int size() { return size; }
    public String name() { return name; }
    public String baseName() {
    	String[]filepath = name.split("/");
    	return filepath[filepath.length-1];
    }
	public String parentDir() {
		String[]filepath = name.split("/");
		String[]filedir;
		if (filepath.length > 1) {
			filedir = Arrays.copyOfRange(filepath, 0, filepath.length-1);
		} else {
			filedir = new String[0];
		}

		return String.join("/", filedir);
	}
    public long Crc() {return Crc; }
    public long time() { return time; }
    public byte[] data() { return data; }
    public void setData(byte []data) { this.data = data; size = data.length; }
    public void setOwner(String owner) { this.owner = owner; }
    public String owner() { return owner; }
    public boolean deleted() { return deleted; }
    public void setDeleted() { deleted = true; }

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
    public FileInfo(String name, byte[] data) {
    	this.size = data.length;
    	this.name = name;
    	this.data = data;
    }
    public FileInfo(FileInfo file) {
    	this.size = file.size;
    	this.name = file.name;
    	this.owner = file.owner;
    	this.data = file.data;
    	this.time = file.time;
    	this.Crc = file.Crc;
    	this.deleted = file.deleted;
    }
}
