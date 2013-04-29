//Found DFU: [0483:df11] devnum=0, cfg=1, intf=0, alt=0, name="@Internal Flash  /0x08004000/00*002Ka,120*002Kg"
//Found DFU: [0483:df11] devnum=0, cfg=1, intf=0, alt=1, name="@SPI Flash: Config/0x00002000/030*04Kg"
//Found DFU: [0483:df11] devnum=0, cfg=1, intf=0, alt=2, name="@SPI Flash: Library/0x00020000/030*064Kg"

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class DfuMemory {
    public static class SegmentParser {
        private List<Sector> sectors;

        public SegmentParser(String addressStr, String sectorStr) {
            sectors = new ArrayList<Sector>();
            int address = Integer.parseInt(addressStr, 16); //Note:  because 'int' is signed, you can't address more than 0x7FFFFFFF
            int index = 0;
            for (String sector : sectorStr.split(",")) {
                Matcher m = Pattern.compile("(\\d+)\\*(\\d+)([BKM])([abcdefg])").matcher(sector);
                if (m.matches()) {
                    int count = Integer.parseInt(m.group(1));
                    int size  = Integer.parseInt(m.group(2));
                    if (m.group(3) == "K") {
                        size = size * 1024;
                    } else if (m.group(3) == "M") {
                        size = size * 1024 * 1024;
                    }
                    boolean readable = false;
                    boolean erasable = false;
                    boolean writable = false;
                    if (m.group(4) == "a" || m.group(4) == "c" || m.group(4) == "e" || m.group(4) == "g") {
                        readable = true;
                    }
                    if (m.group(4) == "b" || m.group(4) == "c" || m.group(4) == "f" || m.group(4) == "g") {
                        erasable = true;
                    }
                    if (m.group(4) == "d" || m.group(4) == "e" || m.group(4) == "f" || m.group(4) == "g") {
                        writable = true;
                    }
                    if (size * count > 0) {
                        sectors.add(new Sector(address, address + size * count - 1, size, count, readable, erasable, writable));
                        address += size * count;
                    }
                }
                index++;
            }
        }
        public List<Sector> sectors() { return sectors;}
    };
    List<SegmentParser> segments;
    String name;
    public DfuMemory(String str) {
        segments = new ArrayList<SegmentParser>();

        String[] segmentStrings = str.split("/");
        if (segmentStrings.length < 3) {
            throw new IllegalArgumentException("String not in correct format");
        }
        name = segmentStrings[0];
        for (int i = 1; i < segmentStrings.length; i+=2) {
            segments.add(new SegmentParser(segmentStrings[i], segmentStrings[i+1]));
        }
    }
    public String name() { return name; }
    public Sector find(int address)
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
};
