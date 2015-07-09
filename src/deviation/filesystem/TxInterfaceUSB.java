package deviation.filesystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.fat.SuperFloppyFormatter;
import deviation.Dfu;
import deviation.DfuDevice;
import deviation.DfuInterface;
import deviation.FileInfo;
import deviation.Progress;
import deviation.Transmitter;
import deviation.TxInfo;
import deviation.filesystem.DevoFS.DevoFSFileSystem;

public class TxInterfaceUSB extends TxInterfaceCommon implements TxInterface  {
    DfuDevice dev;
    FlashIO rootBlockDev;
    FlashIO mediaBlockDev;
    private final int SECTOR_SIZE = 4096;
    private FileSystem rootFs;
    private FileSystem mediaFs;
    private DfuInterface rootIface;
    private DfuInterface mediaIface;
    private Transmitter model;
    //private Progress progress;
    
    public TxInterfaceUSB(DfuDevice dev) {
        mediaBlockDev = null;
        rootBlockDev = null;
        this.dev = dev;
        //this.progress = progress;
		TxInfo txInfo = dev.getTxInfo();
		Transmitter tx = txInfo.type();
        this.model = tx;
    	if (tx == Transmitter.DEVO_UNKNOWN)
    		return;
        if (tx.hasMediaFS()) {
    		mediaIface = dev.SelectInterfaceByAddr(tx.getMediaSectorOffset() * SECTOR_SIZE);
        	mediaBlockDev = new FlashIO(dev, tx.getMediaSectorOffset() * SECTOR_SIZE, tx.isMediaInverted(), SECTOR_SIZE, null);
        	//dev.close();
        }
		rootIface = dev.SelectInterfaceByAddr(tx.getRootSectorOffset() * SECTOR_SIZE);
		if (rootIface == null) {
			System.out.println("Could not identify any memory region for rootFS");
		}
        rootBlockDev = new FlashIO(dev, tx.getRootSectorOffset() * SECTOR_SIZE, tx.isRootInverted(), SECTOR_SIZE, null);
    	
    }
    public void setProgress(Progress progress) {
    	rootBlockDev.setProgress(progress);
    	if (model.hasMediaFS()) {
    		mediaBlockDev.setProgress(progress);
    	}
    }
    public DfuDevice getDevice() { return dev; }
    public boolean hasSeparateMediaDrive() { return mediaBlockDev == null ? false : true; }
    public void Format(FSStatus status) throws IOException {
        if (! status.isRootFormatted()) {
        	dev.SelectInterface(rootIface);
        	//rootBlockDev.markAllCached();
        	if (model.getRootFSType() == FSType.FAT) {
        		rootFs = SuperFloppyFormatter.get(rootBlockDev).format();
        	} else {
        		rootFs = DevoFSFileSystem.format(rootBlockDev);
        	}
            mediaFs = rootFs;
        }
        if (! status.isMediaFormatted()) {
            if (model.hasMediaFS()) {
            	dev.SelectInterface(mediaIface);
            	//mediaBlockDev.markAllCached();
            	if (model.getRootFSType() == FSType.FAT) {
            		mediaFs = SuperFloppyFormatter.get(mediaBlockDev).format();
            	} else {
            		mediaFs = DevoFSFileSystem.format(mediaBlockDev);
            	}
            }
        }
    }
    public void Init(FSStatus status) throws IOException {
        if (status.isRootFormatted()) {
        	dev.SelectInterface(rootIface);
        	if (model.getRootFSType() == FSType.FAT) {
        		rootFs = FatFileSystem.read(rootBlockDev, false);
        	} else {
        		rootFs = DevoFSFileSystem.read(rootBlockDev, false);
        	}
            mediaFs = rootFs;
        }
        if (status.hasMediaFS()) {
            dev.SelectInterface(mediaIface);
            if (model.getRootFSType() == FSType.FAT) {
            	mediaFs = FatFileSystem.read(mediaBlockDev, false);
            } else {
            	mediaFs = DevoFSFileSystem.read(mediaBlockDev, false);
            }
        }
    }
    public List <FileInfo> readAllDirs() {
    	List<FileInfo> files = new ArrayList<FileInfo>();
    	try {
    		FsDirectory dir = rootFs.getRoot();
    		files.addAll(readDirRecur("", dir));
    		if (model.hasMediaFS()) {
    			dir = mediaFs.getRoot();
    			files.addAll(readDirRecur("media/", dir));
    		}
    	} catch (Exception e) { e.printStackTrace(); }
    	return files;
    }
    
    public void readDir(String dirStr) {
        if (! dirStr.matches("/.*")) {
            dirStr = "/" + dirStr;
        }
        FileSystem fs = (dirStr.matches("(?i:/media.*)")) ? mediaFs : rootFs;
        if (fs == rootFs) {
        	dev.SelectInterface(rootIface);
        } else {
        	dev.SelectInterface(mediaIface);
        }
        readDir(fs, dirStr);
    }
    public void copyFile(FileInfo file) {
    	FileSystem fs = (file.name().matches("(?i:media/.*)")) ? mediaFs : rootFs;
        if (fs == rootFs) {
        	dev.SelectInterface(rootIface);
        } else {
        	dev.SelectInterface(mediaIface);
        }
    	FSUtils.copyFile(fs,  file);
    }
    public void fillFileData(FileInfo file) {
    	FileSystem fs = (file.name().matches("(?i:media/.*)")) ? mediaFs : rootFs;
        if (fs == rootFs) {
        	dev.SelectInterface(rootIface);
        } else {
        	dev.SelectInterface(mediaIface);
        }
        FSUtils.fillFileData(fs, file);
    }
    public void open() {
		if (dev.open() != 0) {
			System.out.println("Error: Unable to open device");
			return;
		}
		dev.claim_and_set();    	
    }

    public void close() {
    	if (rootFs != null) {
    		try {
    			dev.SelectInterface(rootIface);
    			rootFs.close();
    			rootBlockDev.close();
    		} catch(Exception e) { e.printStackTrace(); }
    	}
    	if (mediaFs != null && mediaFs != rootFs) {
    		try {
    			dev.SelectInterface(mediaIface);
    			mediaFs.close();
    			mediaBlockDev.close();
    		} catch (Exception e) { e.printStackTrace(); }
    	}
    	dev.close();
    }

    private boolean DetectFS(int status) throws IOException {
    	DfuInterface iface;
    	FlashIO blkdev;
    	FSType type;
    	if (status == FSStatus.ROOT_FS) {
    		iface = rootIface;
    		blkdev = rootBlockDev;
    		type = model.getRootFSType();
    	} else if (status == FSStatus.MEDIA_FS) {
    		iface = mediaIface;
    		blkdev = mediaBlockDev;
    		type = model.getMediaFSType();
    	} else {
    		throw new IOException();
    	}
    	dev.SelectInterface(iface);
        if (dev.open() != 0) {
        	throw new IOException();
        }
        dev.claim_and_set();
        Dfu.setIdle(dev);
    	boolean ret = FSUtils.DetectFS(blkdev, type);
    	dev.close();
    	return ret;
    }
    public FSStatus getFSStatus() {
        boolean has_root = false;
        boolean has_media = false;
        if (model == Transmitter.DEVO_UNKNOWN) {
    		return FSStatus.unformatted();
        }
    
        if (model.hasMediaFS()) {
        	try {
        		has_media = DetectFS(FSStatus.MEDIA_FS);
        	} catch (Exception e){
        		System.out.println("Error: Unable to open media device");
        		return FSStatus.unformatted();
        	}
        }
       	try {
       		has_root = DetectFS(FSStatus.ROOT_FS);
       	} catch (Exception e){
       		System.out.println("Error: Unable to open root device");
    		return FSStatus.unformatted();
       	}
        //IOUtil.writeFile("fatroot", fatRootBytes);
		return new FSStatus(model, has_root, has_media);
    }
}
