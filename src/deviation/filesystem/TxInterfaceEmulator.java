package deviation.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.fat.SuperFloppyFormatter;
import deviation.DfuDevice;
import deviation.FileInfo;
import deviation.Progress;
import deviation.Transmitter;
import deviation.TransmitterList;
import deviation.TxInfo;

public class TxInterfaceEmulator extends TxInterfaceCommon implements TxInterface {
	//private Progress progress;
	private FileSystem fs;
	private FileDisk2 blockDev;
	private Transmitter tx;
	private static final String emulatedTx = "Devo 7e";
	public TxInterfaceEmulator() {
		try {
			blockDev = new FileDisk2(new File("test.fat"), false, 4096, 5000);
			tx = TransmitterList.get(emulatedTx);
		} catch (Exception e) { e.printStackTrace(); }
	}
    public void setProgress(Progress progress) { /*this.progress = progress; */}
    public DfuDevice getDevice() { return null; }
    public boolean hasSeparateMediaDrive() { return false; }
    public void Format(FSStatus status) throws IOException {
		fs = SuperFloppyFormatter.get(blockDev).format();
    }
    public void Init(FSStatus status) throws IOException {
    	fs = FatFileSystem.read(blockDev, false);
    }

    public List <FileInfo> readAllDirs() {
    	List<FileInfo> files = new ArrayList<FileInfo>();
    	try {
    		FsDirectory dir = fs.getRoot();
    		files.addAll(readDirRecur("", dir));
    	} catch (Exception e) { e.printStackTrace(); }
    	return files;
    }
    public void readDir(String dirStr) { readDir(fs, dirStr); }
    public void copyFile(FileInfo file) { FSUtils.copyFile(fs,  file); }
    public void fillFileData(FileInfo file) { FSUtils.fillFileData(fs, file); }
    public void open() {}
    public void close() {
    	try {
    		fs.close();
    	} catch (Exception e) { e.printStackTrace(); }
    }
    public FSStatus getFSStatus() {
    	return new FSStatus(tx, true, false);
    }
    static public TxInfo getTxInfo() {
    	return new TxInfo(TransmitterList.get(emulatedTx));
    }

}
