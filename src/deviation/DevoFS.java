package deviation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import de.waldheinz.fs.BlockDevice;

public class DevoFS implements BlockDevice
{
    //private final RamDisk ram;
    private final byte[] ram;
    private final ByteBuffer rambuf;
    private final DfuDevice dev;
    private final boolean[] cached;
    private final boolean[] changed;
    private final long startOffset;
    private final long memAddress;
    private final int sectorSize;
    private final boolean invert;


    public DevoFS(DfuDevice dev, long start_address, boolean invert)
    {
    	memAddress = dev.Memory().findStartingAddress();
        startOffset = start_address - memAddress;
        
    	Sector sector = dev.Memory().find((int)memAddress);
        sectorSize = sector.size();
        int mem_size = dev.Memory().contiguousSize((int)memAddress);
        
    	ram = new byte[mem_size];
    	rambuf = ByteBuffer.wrap(ram);
        this.dev = dev;
        cached = new boolean[(mem_size + sectorSize - 1) / sectorSize];
        changed = new boolean[(mem_size + sectorSize - 1) / sectorSize];
        this.invert = invert;
    }
    
    public void close() throws IOException {
    	int sector_num;
    	for (sector_num = 0; sector_num < changed.length; sector_num++) {
            if (changed[sector_num]) {
            	byte [] data = Arrays.copyOfRange(ram, sector_num * sectorSize,  sector_num * sectorSize + sectorSize);
                if (invert) {
                    data = DevoFat.invert(data);
                }
                Dfu.sendToDevice(dev,  (int)(memAddress + sector_num * sectorSize), data, null);
                changed[sector_num] = false;
            }
    	}
    }
    public void flush() throws IOException { System.out.println("flush");}
    public int getSectorSize() throws IOException { return sectorSize; }
    public long getSize() throws IOException { return ram.length; }
    public boolean isClosed() { return false; }
    public boolean isReadOnly() { return false; }
    
    private void cache(int sector_num) throws IOException {
        System.out.format(("Cache of 0x%08x : %d%n"), memAddress + sector_num * sectorSize, cached[sector_num] ? 1 : 0);
        if (! cached[sector_num]) {
            byte[] data = Dfu.fetchFromDevice(dev, (int)(memAddress + sector_num * sectorSize), sectorSize);
            if (invert) {
                data = DevoFat.invert(data);
            }
            System.arraycopy(data, 0, ram, sector_num * sectorSize, data.length);
            cached[sector_num] = true;
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
            changed[curSector] = true;
            curSector++;
        }  
    }
}
