package deviation;

import java.util.List;
import java.util.logging.Logger;

import deviation.filesystem.FSType;
import deviation.misc.Crc;

public class Transmitter {

	private static final Logger LOG = Logger.getLogger(Transmitter.class.getName());

	public static enum ProtoFiles {NONE, KEEP, ALL};
	public String name;
	public String id;
	public int encryption;
	public FlashInfo root;
	public FlashInfo media;
	public List<String> matchRules;
	public List<SectorOverride> overrideSectors;
	public ProtoFiles fsProtocols;
	public Transmitter() {
		root = FlashInfo.empty;
		media = FlashInfo.empty;
		fsProtocols = ProtoFiles.NONE;
	}
	public Transmitter(String name,
			String id,
			int encryption,
			FlashInfo root,
			FlashInfo media,
			List<String> matchRules,
			List<SectorOverride> overrideSectors) {
		this.name = name;
		this.id = id;
		this.encryption = encryption;
		this.root = root;
		this.media = media;
		this.matchRules = matchRules;
		this.overrideSectors = overrideSectors;
	}
	
	public String getName()           { return name; }
	public String getId()             { return id; }
	public long getRootSectorOffset()  { return root.sectorOffset; }
    public int getRootSectorCount()   { return root.sectorCount; }
    public FSType getRootFSType()     { return root.fsType; }
    public boolean isRootInverted()   { return root.inverted; }
    public boolean hasMediaFS()       { return media.sectorOffset < 0 ? false : true; }
    public long getMediaSectorOffset() { return media.sectorOffset; }
    public int getMediaSectorCount()  { return media.sectorCount; }
    public FSType getMediaFSType()    { return media.fsType; }
    public boolean isMediaInverted()  { return media.inverted; }
    public ProtoFiles needsFsProtocols() { return fsProtocols; }

    public void overrideSector(List<DfuInterface> interfaces) {
    	if (overrideSectors == null) {
    		return;
    	}
    	for (SectorOverride override: overrideSectors) {
    		if (override.iface == null) {
    			continue;
    		}
			List <Sector> sectors = interfaces.get(override.iface).Memory().segments().get(0).sectors();
    		if (override.remove != null && override.remove == true) {
    			sectors.clear();
    		}
    		if (override.add  != null) {
    			for (SectorPrivate sector: override.add) {
    				sectors.add(new Sector(sector.start, sector.end, sector.size, sector.count, true, true, true));
    			}
    		}
    	}
    }
    
    public boolean isUnknown() { return name.equals("Unknown"); }
    
    public boolean modelMatch(String str) {
    	if (matchRules == null) {
    		return false;
    	}
    	for (String match : matchRules) {
    		if (str.matches(match)) {
    			return true;
    		}
    	}
    	return false;
    }
    
	public byte[] encode(long id1, long id2, long id3) {
		switch(encryption) {
		case 12: return encode_12(id1, id2, id3);
		case 10: return encode_10(id1, id2, id3);
		case 8:  return encode_6_8(id1, id2, id3);
		case 7:  return encode_7(id1, id2, id3);
		case 6:  return encode_6_8(id1, id2, id3);
		case 4:  return encode_4(id1, id2, id3);
		}
		return null;
	}

    static private long calcCrc(long[] computed, int count, long id1, long id2, long id3) {
        long [] data = new long[7];
        data[0] = id1;
        data[1] = id2;
        data[2] = id3;
        data[3] = computed[0];
        data[4] = computed[1];
        data[5] = computed[2];
        data[6] = computed[3];
        long crc = 0xFFFFFFFFL;
        for (int i = 0; i < count; i++) {
            crc = Crc.Dfu32(data, crc);
        }
        LOG.finest(String.format("Crc: 0x%x", crc));
        return crc;
    }

    static private byte[] convertToByteArray(long[] data, int offset) {
        byte []buf = new byte[20];
        int i = 0;
        for (long  val : data) {
            buf[i++] = (byte)((val >>  0) & 0xff);
            buf[i++] = (byte)((val >>  8) & 0xff);
            buf[i++] = (byte)((val >> 16) & 0xff);
            buf[i++] = (byte)((val >> 24) & 0xff);
        }
        for (i = 0; i < buf.length; i++) {
            int val = buf[i] & 0xff;
            if(val >= 0x80 && val <= 0xcf - offset) {
                val += offset;
            } else if(val >= 0xd0 - offset && val < 0xd0) {
                val -= (0x50 - offset);
            }
            buf[i] = (byte)val;
        }
        return buf;
    }
	static private byte[] encode_12(long id1, long id2, long id3) {
        long[] a = new long[5];
        a[0] = 0xFFFFFFFFL & (id1 + id2 + id3);
        a[1] = 0xFFFFFFFFL & (id1 ^ id2 ^ id3);
        a[2] = 0xFFFFFFFFL & (id2 + id3);
        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
        a[4] = calcCrc(a, 12, id1, id2, id3);
        return convertToByteArray(a, 12);
	}
	static private byte[] encode_10(long id1, long id2, long id3) {
        long[] a = new long[5];
        a[0] = 0xFFFFFFFFL & (id1 + id2);
        a[1] = 0xFFFFFFFFL & (id1 ^ id2);
        a[2] = 0xFFFFFFFFL & (id2 + id3);
        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
        a[4] = calcCrc(a, 10, id1, id2, id3);
        return convertToByteArray(a, 10);
    }
	static private byte[] encode_6_8(long id1, long id2, long id3) {
        long[] a = new long[5];
        a[0] = 0xFFFFFFFFL & (id1 + id2);
        a[1] = 0xFFFFFFFFL & (id1 ^ id2);
        a[2] = 0xFFFFFFFFL & (id1 + id3);
        a[3] = 0xFFFFFFFFL & (id1 ^ id3);
        a[4] = calcCrc(a, 8, id1, id2, id3);
        return convertToByteArray(a, 8);
	}
	static private byte[] encode_7(long id1, long id2, long id3) {
        long[] a = new long[5];
        a[0] = 0xFFFFFFFFL & (id1 + id2);
        a[1] = 0xFFFFFFFFL & (id2 ^ id2);
        a[2] = 0xFFFFFFFFL & (id1 + id3);
        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
        a[4] = calcCrc(a, 7, id1, id2, id3);
        return convertToByteArray(a, 7);
	}
	static private byte[] encode_4(long id1, long id2, long id3) {
        long[] a = new long[5];
        a[0] = 0xFFFFFFFFL & (id1 + id2);
        a[1] = 0xFFFFFFFFL & (id2 ^ id3);
        a[2] = 0xFFFFFFFFL & (id1 + id2);
        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
        a[4] = calcCrc(a, 7, id1, id2, id3);
        return convertToByteArray(a, 4);
	}
	static public class FlashInfo {
		public int sectorOffset;
		public int sectorCount;
		public boolean inverted;
		public FSType fsType;
		final static FlashInfo empty = new FlashInfo(-1, -1, false, FSType.NONE);
		private FlashInfo (int offset, int count, boolean inverted, FSType type) {
			this.sectorOffset = offset;
			this.sectorCount = count;
			this.inverted = inverted;
			this.fsType = type;
		}
		FlashInfo () {}
	}
	static public class SectorOverride {
		public Integer iface;
		public Boolean remove;
		public List<SectorPrivate> add;
	}
	static public class SectorPrivate {
        public long start;
        public long end;
        public long size;
        public int count;
	}
}
