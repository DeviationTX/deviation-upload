package deviation;

import java.io.IOException;
import java.util.Iterator;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.fat.SuperFloppyFormatter;
import deviation.TxInfo.TxModel;

public class DevoFat {
    
    public enum FatStatus {NO_FAT, ROOT_FAT, MEDIA_FAT, ROOT_AND_MEDIA_FAT};

    BlockDevice rootBlockDev;
    BlockDevice mediaBlockDev;
    private final int SECTOR_SIZE = 4096;
    private FileSystem rootFs;
    private FileSystem mediaFs;
    private TxModel model;
    
    public DevoFat(DfuDevice dev, TxModel model) {
        int sector_offset;
        int num_sectors;
        mediaBlockDev = null;
        rootBlockDev = null;
        this.model = model;
        switch(model) {
        case DEVO7e:
            sector_offset = 0;
            num_sectors = 512;
            break;
        case DEVO6:
        case DEVO8:
        case DEVO10:
            sector_offset = 54;
            num_sectors = 1024;
            break;
        case DEVO12:
            sector_offset = 0;
            num_sectors = 512;
            mediaBlockDev = new DevoFS(dev, 0x64080000, false, 16 * 1024 * 1024, SECTOR_SIZE);
            break;
        default:
            return;
        }
        rootBlockDev = new DevoFS(dev, sector_offset * SECTOR_SIZE, true, (num_sectors - sector_offset) * SECTOR_SIZE, SECTOR_SIZE);
    }
    public void Format(FatStatus type) throws IOException {
        if (type == FatStatus.ROOT_FAT || type == FatStatus.ROOT_AND_MEDIA_FAT) {
            rootFs = SuperFloppyFormatter.get(rootBlockDev).format();
            mediaFs = rootFs;
        }
        if (type == FatStatus.MEDIA_FAT || type == FatStatus.ROOT_AND_MEDIA_FAT) {
            if (model == TxModel.DEVO12) {
                mediaFs = SuperFloppyFormatter.get(mediaBlockDev).format();
            }
        }
    }
    public void Init(FatStatus type) throws IOException {
        if (type == FatStatus.ROOT_FAT || type == FatStatus.ROOT_AND_MEDIA_FAT) {
            rootFs = FatFileSystem.read(rootBlockDev, false);
            mediaFs = rootFs;
        }
        if (type == FatStatus.MEDIA_FAT || type == FatStatus.ROOT_AND_MEDIA_FAT) {
            if (model == TxModel.DEVO12) {
                mediaFs = FatFileSystem.read(mediaBlockDev, false);
            }
        }
    }
    public void readDir(String dirStr) {
        if (! dirStr.matches("/.*")) {
            dirStr = "/" + dirStr;
        }
        try {        		
        FileSystem fs = (dirStr.matches("/media")) ? mediaFs : rootFs;
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

    public static byte[] invert(byte[] data) {
        int i;
        for(i = 0; i < data.length; i++) {
            int j = ~data[i]; 
            data[i] = (byte)(j&0xff);
        }
        return data;
    }

}
