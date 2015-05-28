package deviation.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import de.waldheinz.fs.BlockDevice;
import deviation.Dfu;
import deviation.DfuDevice;
import deviation.Progress;
import deviation.Sector;
import deviation.Sha;

public class FlashIO implements BlockDevice
{
    //private final RamDisk ram;
    private final byte[] ram;
    private final ByteBuffer rambuf;
    private final DfuDevice dev;
    private final boolean[] cached;
    private final String[] chksum;
    private final long startOffset;
    private final long memAddress;
    private final int sectorSize;
    private final boolean invert;
    private final int fsSectorSize;
    private Progress progress;


    public FlashIO(DfuDevice dev, long start_address, boolean invert, int fs_sector_size, Progress progress)
    {
    	memAddress = dev.Memory().findStartingAddress();
        startOffset = start_address - memAddress;
        fsSectorSize = fs_sector_size;
        this.progress = progress;
        
    	Sector sector = dev.Memory().find((int)memAddress);
        sectorSize = sector.size();
        int mem_size = dev.Memory().contiguousSize((int)memAddress);
        
    	ram = new byte[mem_size];
    	rambuf = ByteBuffer.wrap(ram);
        this.dev = dev;
        cached = new boolean[(mem_size + sectorSize - 1) / sectorSize];
        chksum = new String[(mem_size + sectorSize - 1) / sectorSize];
        this.invert = invert;
    }
    
    public void setProgress(Progress progress) {
    	this.progress = progress;
    }
    public void close() throws IOException {
    	int sector_num;
    	for (sector_num = 0; sector_num < cached.length; sector_num++) {
            if (cached[sector_num]) {
            	byte [] data = Arrays.copyOfRange(ram, sector_num * sectorSize,  sector_num * sectorSize + sectorSize);
            	if (chksum[sector_num] != null && chksum[sector_num].equals(Sha.md5(data))) {
            		//Data hasn't changed, no need to write it out
            		continue;
            	}
                if (invert) {
                    data = TxInterface.invert(data);
                }
                Dfu.sendToDevice(dev,  (int)(memAddress + sector_num * sectorSize), data, progress);
            }
    	}
    }
    public void flush() throws IOException { System.out.println("flush");}
    public int getSectorSize() throws IOException { return fsSectorSize; }
    public long getSize() throws IOException { return ram.length - startOffset; }
    public boolean isClosed() { return false; }
    public boolean isReadOnly() { return false; }
    public void markAllCached() {
    	// This is used for the format operation to prevent disk reads
    	for (int i = 0; i < cached.length; i++) {
    		cached[i] = true;
    	}
    }
    
    private void cache(int sector_num) throws IOException {
        System.out.format(("Cache of 0x%08x : %d%n"), memAddress + sector_num * sectorSize, cached[sector_num] ? 1 : 0);
        if (! cached[sector_num]) {
            byte[] data = Dfu.fetchFromDevice(dev, (int)(memAddress + sector_num * sectorSize), sectorSize);
            if (invert) {
                data = TxInterface.invert(data);
            }
            System.arraycopy(data, 0, ram, sector_num * sectorSize, data.length);
            cached[sector_num] = true;
            chksum[sector_num] = Sha.md5(data);
        }
    }
    public void read(long devOffset, ByteBuffer dest) throws IOException {
        long start = startOffset + devOffset;
        long end = start + dest.remaining();
        int curSector = (int)(start / sectorSize);
        long lastSector = (end + sectorSize -1) / sectorSize;
        while (curSector <= lastSector) {
            cache(curSector);
            curSector++;
        }
        rambuf.limit((int)end);
        rambuf.position((int)start);
        dest.put(rambuf);
    }

    public void write(long devOffset, ByteBuffer src) throws IOException {
        long start = startOffset + devOffset;
        long end = start + src.remaining();
        int curSector = (int)(start / sectorSize);
        long lastSector = (end + sectorSize -1) / sectorSize;
        if (start % sectorSize > 0) {
            //Not at a sector boundary
            cache((int)(start / sectorSize));
        }
        if (end % sectorSize > 0) {
            //End is not on a sector boundary;
            cache((int)(end / sectorSize));
        }
        rambuf.limit((int)end);
        rambuf.position((int)start);
        rambuf.put(src);
        while (curSector <= lastSector) {
        	cached[curSector] = true;
        	curSector++;
        }  
    }
}
