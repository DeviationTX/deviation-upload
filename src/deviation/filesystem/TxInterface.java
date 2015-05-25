package deviation.filesystem;

import java.io.IOException;
import java.util.Iterator;

import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.fat.SuperFloppyFormatter;
import deviation.DfuDevice;
import deviation.DfuInterface;
import deviation.FileInfo;
import deviation.Progress;
import deviation.Transmitter;
import deviation.TxInfo;
import deviation.TxUtils;

public class TxInterface {
    
    public enum FatStatus {NO_FAT, ROOT_FAT, MEDIA_FAT, ROOT_AND_MEDIA_FAT};

    DfuDevice dev;
    FlashIO rootBlockDev;
    FlashIO mediaBlockDev;
    private final int SECTOR_SIZE = 4096;
    private FileSystem rootFs;
    private FileSystem mediaFs;
    private DfuInterface rootIface;
    private DfuInterface mediaIface;
    private Transmitter model;
    private FSUtils fsutils;
    private Progress progress;
    
    public TxInterface(DfuDevice dev, Progress progress) {
        mediaBlockDev = null;
        rootBlockDev = null;
        this.dev = dev;
        this.progress = progress;
		TxInfo txInfo = TxUtils.getTxInfo(dev);
		Transmitter tx = txInfo.type();
        this.model = tx;
        fsutils = new FSUtils();
    	if (tx == Transmitter.DEVO_UNKNOWN)
    		return;
        if (tx.hasMediaFS()) {
    		mediaIface = dev.SelectInterfaceByAddr(tx.getMediaSectorOffset() * SECTOR_SIZE);
        	mediaBlockDev = new FlashIO(dev, tx.getMediaSectorOffset() * SECTOR_SIZE, tx.isMediaInverted(), SECTOR_SIZE, progress);
        	dev.close();
        }
		rootIface = dev.SelectInterfaceByAddr(tx.getRootSectorOffset() * SECTOR_SIZE);
        rootBlockDev = new FlashIO(dev, tx.getRootSectorOffset() * SECTOR_SIZE, tx.isRootInverted(), SECTOR_SIZE, progress);
    }
    public boolean hasSeparateMediaDrive() { return mediaBlockDev == null ? false : true; }
    public void Format(FatStatus type) throws IOException {
        if (type == FatStatus.ROOT_FAT || type == FatStatus.ROOT_AND_MEDIA_FAT) {
        	dev.SelectInterface(rootIface);
        	rootBlockDev.markAllCached();
            rootFs = SuperFloppyFormatter.get(rootBlockDev).format();
            mediaFs = rootFs;
        }
        if (type == FatStatus.MEDIA_FAT || type == FatStatus.ROOT_AND_MEDIA_FAT) {
            if (model.hasMediaFS()) {
            	dev.SelectInterface(mediaIface);
            	mediaBlockDev.markAllCached();
                mediaFs = SuperFloppyFormatter.get(mediaBlockDev).format();
            }
        }
    }
    public void Init(FatStatus type) throws IOException {
        if (type == FatStatus.ROOT_FAT || type == FatStatus.ROOT_AND_MEDIA_FAT) {
        	dev.SelectInterface(rootIface);
            rootFs = FatFileSystem.read(rootBlockDev, false);
            mediaFs = rootFs;
        }
        if (type == FatStatus.MEDIA_FAT || type == FatStatus.ROOT_AND_MEDIA_FAT) {
            if (model.hasMediaFS()) {
            	dev.SelectInterface(mediaIface);
                mediaFs = FatFileSystem.read(mediaBlockDev, false);
            }
        }
    }
    public void readDir(String dirStr) {
        if (! dirStr.matches("/.*")) {
            dirStr = "/" + dirStr;
        }
        try {
        FileSystem fs = (dirStr.matches("(?i:/media.*)")) ? mediaFs : rootFs;
        if (fs == rootFs) {
        	dev.SelectInterface(rootIface);
        } else {
        	dev.SelectInterface(mediaIface);
        }
        String[] dirs = dirStr.split("/");
        FsDirectory dir = fs.getRoot();
        for (String subdir : dirs) {
            if (subdir.equals("")) {
               continue;
            }
            dir = dir.getEntry(subdir).getDirectory();
        }
        Iterator<FsDirectoryEntry> itr = dir.iterator();
        while(itr.hasNext()) {
            FsDirectoryEntry entry = itr.next();
            System.out.println(entry.getName());
        }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    public void copyFile(FileInfo file) {
    	FileSystem fs = (file.name().matches("(?i:media/.*)")) ? mediaFs : rootFs;
        if (fs == rootFs) {
        	dev.SelectInterface(rootIface);
        } else {
        	dev.SelectInterface(mediaIface);
        }
    	fsutils.copyFile(fs,  file);
    }
    public void close() {
    	if (rootFs != null) {
    		try {
    			dev.SelectInterface(rootIface);
    			rootFs.close();
    			rootBlockDev.close();
    		} catch(Exception e) {}
    	}
    	if (mediaFs != null && mediaFs != rootFs) {
    		try {
    			dev.SelectInterface(mediaIface);
    			mediaFs.close();
    			mediaBlockDev.close();
    		} catch (Exception e) {}
    	}
    }

    public static byte[] invert(byte[] data) {
        int i;
        for(i = 0; i < data.length; i++) {
            int j = ~data[i]; 
            data[i] = (byte)(j&0xff);
        }
        return data;
    }

}
