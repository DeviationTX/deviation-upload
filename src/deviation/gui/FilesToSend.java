package deviation.gui;

import java.util.ArrayList;
import java.util.List;

import deviation.DfuFile;
import deviation.FileInfo;

public class FilesToSend {
	private DfuFile firmwareDfu;
	private List<DfuFile> libraryDfus;
	private List<FileInfo> files;
	private boolean formatRoot;
	private boolean formatMedia;
	private long totalBytes;

	
	public FilesToSend() {
        libraryDfus = new ArrayList<DfuFile>();
        firmwareDfu = null;
        files = new ArrayList<FileInfo>();
        formatRoot = false;
        formatMedia = false;
        totalBytes = 0;
	}
	
	public void setFirmwareDfu(DfuFile dfu) { firmwareDfu = dfu; }
	public DfuFile getFirmwareDfu() { return firmwareDfu; }
	public void setLibraryDfus(List<DfuFile> dfus) { libraryDfus = dfus; }
	public List<DfuFile> getLibraryDfus() { return libraryDfus; }
	public void clearFiles() { files.clear(); }
	public void addFile(FileInfo file) { files.add(file); }
	public List<FileInfo> getFiles() { return files; }

	public void formatRoot(boolean fmt) { formatRoot = fmt; }
	public boolean formatRoot() { return formatRoot; }
	public void formatMedia(boolean fmt) { formatMedia = fmt; }
	public boolean formatMedia() { return formatMedia; }
	public void setTotalBytes(long bytes) { totalBytes = bytes; }
	public long getTotalBytes() { return totalBytes; }

}
