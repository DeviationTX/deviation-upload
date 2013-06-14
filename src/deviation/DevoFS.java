package deviation;

import java.io.IOException;
import java.nio.ByteBuffer;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.util.RamDisk;

public class DevoFS implements BlockDevice
{
    public final static int DEFAULT_SECTOR_SIZE = 512;
    private final RamDisk ram;
    private final DfuDevice dev;
    private final boolean[] cached;
    private final boolean[] changed;
    private final long startAddress;
    private final int sectorSize;
    private final boolean invert;


    public DevoFS(DfuDevice dev, long start_address, boolean invert, int size, int sector_size)
    {
        ram = new RamDisk(size, sector_size);
        this.dev = dev;
        cached = new boolean[(size + sector_size - 1) / sector_size];
        changed = new boolean[(size + sector_size - 1) / sector_size];
        startAddress = start_address;
        sectorSize = sector_size;
        this.invert = invert;
    }
    
    public DevoFS(DfuDevice dev, long start_address, boolean invert, int size)
    {
        this(dev, start_address, invert, size, DEFAULT_SECTOR_SIZE);
    }

    public void close() throws IOException { ram.close(); }
    public void flush() throws IOException { ram.flush(); }
    public int getSectorSize() throws IOException { return sectorSize; }
    public long getSize() throws IOException { return ram.getSize(); }
    public boolean isClosed() { return ram.isClosed(); }
    public boolean isReadOnly() { return ram.isReadOnly(); }
    
    private void cache(int sector_num) throws IOException {
        System.out.format(("Cache of 0x%08x : %d%n"), startAddress + sector_num * sectorSize, cached[sector_num] ? 1 : 0);
        if (! cached[sector_num]) {
            byte[] data = Dfu.fetchFromDevice(dev, (int)(startAddress + sector_num * sectorSize), sectorSize);
            if (invert) {
                data = DevoFat.invert(data);
            }
            ByteBuffer byteBuffer = ByteBuffer.allocate(data.length);
            byteBuffer.put(data);
            byteBuffer.flip();
            ram.write(sector_num * sectorSize, byteBuffer);
            cached[sector_num] = true;
        }
    }
    public void read(long devOffset, ByteBuffer dest) throws IOException {
        long start = devOffset;
        long end = start + dest.remaining();
        int curSector = (int)(start / sectorSize);
        long lastSector = (end + sectorSize -1) / sectorSize;
        while (curSector <= lastSector) {
            cache(curSector);
            curSector++;
        }
        ram.read(devOffset, dest);
    }

    public void write(long devOffset, ByteBuffer src) throws IOException {
        long start = devOffset;
        long end = start + src.remaining();
        int curSector = (int)(start / sectorSize);
        long lastSector = (end + sectorSize -1) / sectorSize;
        if (devOffset % sectorSize > 0) {
            //Not at a sector boundary
            cache((int)(devOffset / sectorSize));
        }
        if ((devOffset + src.remaining()) % sectorSize > 0) {
            //End is not on a sector boundary;
            cache((int)((devOffset + src.remaining()) / sectorSize));
        }
        ram.write(devOffset, src);
        while (curSector <= lastSector) {
            changed[curSector] = true;
            curSector++;
        }  
    }
}
