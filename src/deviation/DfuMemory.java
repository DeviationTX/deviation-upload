package deviation;

import java.util.*;
import java.util.regex.*;

//Found DFU: [0483:df11] devnum=0, cfg=1, intf=0, alt=0, name="@Internal Flash  /0x08004000/00*002Ka,120*002Kg"
//Found DFU: [0483:df11] devnum=0, cfg=1, intf=0, alt=1, name="@SPI Flash: Config/0x00002000/030*04Kg"
//Found DFU: [0483:df11] devnum=0, cfg=1, intf=0, alt=2, name="@SPI Flash: Library/0x00020000/030*064Kg"

public class DfuMemory {
    public static class SegmentParser {
        private List<Sector> sectors;

        public SegmentParser(String addressStr, String sectorStr) {
            sectors = new ArrayList<Sector>();
            
            long address = Long.decode(addressStr).longValue();
            for (String sector : sectorStr.split(",")) {
                Matcher m = Pattern.compile("(\\d+)\\*(\\d+)([BKM])([abcdefg])").matcher(sector);
                if (m.matches()) {
                    int count = Integer.parseInt(m.group(1));
                    int size  = Integer.parseInt(m.group(2));
                    if (m.group(3).equals("K")) {
                        size = size * 1024;
                    } else if (m.group(3).equals("M")) {
                        size = size * 1024 * 1024;
                    }
                    boolean readable = false;
                    boolean erasable = false;
                    boolean writable = false;
                    if (m.group(4).matches("[aceg]")) {
                        readable = true;
                    }
                    if (m.group(4).matches("[bcfg]")) {
                        erasable = true;
                    }
                    if (m.group(4).matches("[defg]")) {
                        writable = true;
                    }
                    if (size * count > 0) {
                        //System.out.format("Sector start: 0x%x end: 0x%x size:%d count:%d %s%s%s%n",
                        //                  address, address + size * count - 1, size, count,
                        //                  readable ? "r" : "", erasable ? "e" : "", writable ? "w" : "");
                        sectors.add(new Sector(address, address + size * count - 1, size, count, readable, erasable, writable));
                        address += size * count;
                    }
                }
            }
        }
        public List<Sector> sectors() { return sectors;}
    };
    private List<SegmentParser> segments;
    String name;
    public DfuMemory(String str) {
        segments = new ArrayList<SegmentParser>();
        if (str == null || str.length() == 0) {
            name = "Unknown";
            return;
        }
        String[] segmentStrings = str.split("/");
        if (segmentStrings.length < 3) {
            System.out.format("Unexpected string: %s%n", str);
            throw new IllegalArgumentException("String not in correct format");
        }
        name = segmentStrings[0];
        for (int i = 1; i < segmentStrings.length; i+=2) {
            segments.add(new SegmentParser(segmentStrings[i], segmentStrings[i+1]));
        }
    }
    public List<SegmentParser> segments() { return segments; }
    public String name() { return name; }
    public Sector find(long address)
    {
        for(SegmentParser segment : segments) {
            for (Sector sector: segment.sectors()) {
                if (address >= sector.start() && address <= sector.end()) {
                    return sector;
                }
            }
        }
        return null;
    }
    
    public long findStartingAddress() {
        long address = -1;
        for(SegmentParser segment : segments) {
            for (Sector sector: segment.sectors()) {
                if (address == -1 || sector.start() < address) {
                    address = sector.start();
                }
            }
        }
        return address;
    }
    
    public long contiguousSize(int address) {
        for(SegmentParser segment : segments) {
            long end = 0;
            boolean ok = false;
            for (Sector sector: segment.sectors()) {
                end = sector.end();
                if (address >= sector.start() && address <= sector.end()) {
                    ok = true;
                }
            }
            if (ok) {
                return end + 1 - address;
            }
        }
        return -1;
    }
};
