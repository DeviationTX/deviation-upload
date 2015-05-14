package deviation;

public enum Transmitter {
	DEVO_UNKNOWN("Unknown", "", 0, -1, -1, false, -1, -1, false) {
		public boolean modelMatch(String str) { return false; }
	},
	DEVO12("Devo 12", "DEVO-12", 12, 0, 512, true, 0x64080, 4*1024, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-12.*") || str.matches(".*devo12.*") || str.equals("12"); }	
	},
	DEVO12E("Devo 12E", "DEVO-12E", 12, 54, 1024, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-12E.*") || str.matches(".*devo12e.*") || str.equals("12e"); }	
	},
	DEVOF12E("Devo F12E", "FPV-12E", 12, 0, 64, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-F12E.*") || str.matches(".*devof12e.*") || str.equals("f12e"); }	
	},
	DEVO10("Devo 10", "DEVO-10", 10, 54, 1024, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-10.*") || str.matches(".*devo10.*") || str.equals("10"); }	
	},
	DEVO8("Devo 8", "DEVO-08", 8, 54, 1024, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-8.*") || str.matches(".*devo8.*") || str.equals("8"); }	
	},
	DEVO7e("Devo 7e", "DEVO-7e", 7, 0, 512, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-7E.*") || str.matches(".*devo7e.*") || str.equals("7e"); }	
	},
	DEVO6("Devo 6", "DEVO-06", 6, 54, 1024, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-6.*") || str.matches(".*devo6.*") || str.equals("6"); }	
	};
/*
  	DEVOF7("Devo F7", "DEVO-F7", 7, -1, -1, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-F7.*") || str.matches(".*devof7.*") || str.equals("f7"); }	
	},
	DEVOF4("Devo F4", "DEVO-F4", 4, -1, -1, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-F4.*") || str.matches(".*devof4.*") || str.equals("f4"); }	
	},
  	DEVOF12("Devo F12", "DEVO-F12", 12, -1, -1, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-F12.*") || str.matches(".*devof12.*") || str.equals("f12"); }	
	},
	DEVOF7S("Devo F7S", "DFPV-7S", 7, -1, -1, true, -1, -1, false) {
		public boolean modelMatch(String str) { return str.matches("DEVO-F7S.*") || str.matches(".*devof7s.*") || str.equals("f7s"); }	
	},
*/
	
	public abstract boolean modelMatch(String str);
	
	private String id;
	private String name;
	private int numChannels;
	private int rootSectorOffset;
	private int rootSectorCount;
	private boolean rootInverted;
	private int mediaSectorOffset;
	private int mediaSectorCount;
	private boolean mediaInverted;
	private Transmitter(
			String name,
			String id,
			int numChannels,
			int rootSectorOffset,
			int rootSectorCount,
			boolean rootInverted,
			int mediaSectorOffset,
			int mediaSectorCount,
			boolean mediaInverted)
	{
		this.name = name;
		this.id = id;
		this.numChannels = numChannels;
		this.rootSectorOffset = rootSectorOffset;
		this.rootSectorCount = rootSectorCount;
		this.rootInverted = rootInverted;
		this.mediaSectorOffset = mediaSectorOffset;
		this.mediaSectorCount = mediaSectorCount;
		this.mediaInverted = mediaInverted;
	}
	public String getName()           { return name; }
	public String getId()             { return id; }
	public int getRootSectorOffset()  { return rootSectorOffset; }
    public int getRootSectorCount()   { return rootSectorCount; }
    public boolean isRootInverted()   { return rootInverted; }
    public boolean hasMediaFS()       { return mediaSectorOffset < 0 ? false : true; }
    public int getMediaSectorOffset() { return mediaSectorOffset; }
    public int getMediaSectorCount()  { return mediaSectorCount; }
    public boolean isMediaInverted()  { return mediaInverted; }	

	public byte[] encode(long id1, long id2, long id3) {
		switch(numChannels) {
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
        //System.out.format("Crc: 0x%x%n", crc);
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

}
