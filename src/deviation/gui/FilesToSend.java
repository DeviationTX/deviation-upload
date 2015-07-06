package deviation.gui;

import java.util.ArrayList;
import java.util.List;

import deviation.DfuFile;
import deviation.FileInfo;

public class FilesToSend {
	private DfuFile firmwareDfu;
	private List<DfuFile> libraryDfus;
	private List<FileInfo> files;
	private boolean format;
	private long totalBytes;

	
	public FilesToSend() {
        libraryDfus = new ArrayList<DfuFile>();
        firmwareDfu = null;
        files = new ArrayList<FileInfo>();
        format = false;
        totalBytes = 0;
	}
	
	public void setFirmwareDfu(DfuFile dfu) { firmwareDfu = dfu; }
	public DfuFile getFirmwareDfu() { return firmwareDfu; }
	public void setLibraryDfus(List<DfuFile> dfus) { libraryDfus = dfus; }
	public List<DfuFile> getLibraryDfus() { return libraryDfus; }
	public void clearFiles() { files.clear(); }
	public void addFile(FileInfo file) { files.add(file); }
	public void removeFile(FileInfo file) { files.remove(file); }
	public List<FileInfo> getFiles() { return files; }

	public void    format(boolean fmt) { format = fmt; }
	public boolean format() { return format; }
	public void setTotalBytes(long bytes) { totalBytes = bytes; }
	public long getTotalBytes() { return totalBytes; }

}
