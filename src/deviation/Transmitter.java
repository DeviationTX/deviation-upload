package deviation;

public enum Transmitter {
	DEVO_UNKNOWN("Unknown", "", -1, -1, false, -1, -1, false) {
		public byte[] encode(long id1, long id2, long id3) { return null;}
		public boolean modelMatch(String str) { return false; }
	},
	DEVO12("Devo 12", "DEVO-12", 0, 512, true, 0x64080, 4*1024, false) {
		public byte[] encode(long id1, long id2, long id3) {
	        long[] a = new long[5];
	        a[0] = 0xFFFFFFFFL & (id1 + id2 + id3);
	        a[1] = 0xFFFFFFFFL & (id1 ^ id2 ^ id3);
	        a[2] = 0xFFFFFFFFL & (id2 + id3);
	        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
	        a[4] = calcCrc(a, 12, id1, id2, id3);
	        return convertToByteArray(a, 12);
		}
		public boolean modelMatch(String str) { return str.matches("DEVO-12.*") || str.matches(".*devo12.*") || str.equals("12"); }	
	},
	DEVO10("Devo 10", "DEVO-10", 54, 1024, true, -1, -1, false) {
		public byte[] encode(long id1, long id2, long id3) {
	        long[] a = new long[5];
	        a[0] = 0xFFFFFFFFL & (id1 + id2);
	        a[1] = 0xFFFFFFFFL & (id1 ^ id2);
	        a[2] = 0xFFFFFFFFL & (id2 + id3);
	        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
	        a[4] = calcCrc(a, 10, id1, id2, id3);
	        return convertToByteArray(a, 10);
	    }
		public boolean modelMatch(String str) { return str.matches("DEVO-10.*") || str.matches(".*devo10.*") || str.equals("10"); }	
	},
	DEVO8("Devo 8", "DEVO-08", 54, 1024, true, -1, -1, false) {
		public byte[] encode(long id1, long id2, long id3) {
	        long[] a = new long[5];
	        a[0] = 0xFFFFFFFFL & (id1 + id2);
	        a[1] = 0xFFFFFFFFL & (id1 ^ id2);
	        a[2] = 0xFFFFFFFFL & (id1 + id3);
	        a[3] = 0xFFFFFFFFL & (id1 ^ id3);
	        a[4] = calcCrc(a, 8, id1, id2, id3);
	        return convertToByteArray(a, 8);
		}
		public boolean modelMatch(String str) { return str.matches("DEVO-8.*") || str.matches(".*devo8.*") || str.equals("8"); }	
	},
	DEVO7e("Devo 7e", "DEVO-7e", 0, 512, true, -1, -1, false) {
		public byte[] encode(long id1, long id2, long id3) {
	        long[] a = new long[5];
	        a[0] = 0xFFFFFFFFL & (id1 + id2);
	        a[1] = 0xFFFFFFFFL & (id2 ^ id2);
	        a[2] = 0xFFFFFFFFL & (id1 + id3);
	        a[3] = 0xFFFFFFFFL & (id2 ^ id3);
	        a[4] = calcCrc(a, 7, id1, id2, id3);
	        return convertToByteArray(a, 7);
		}
		public boolean modelMatch(String str) { return str.matches("DEVO-7E.*") || str.matches(".*devo7e.*") || str.equals("7e"); }	
	},
	DEVO6("Devo 6", "DEVO-06", 54, 1024, true, -1, -1, false) {
		public byte[] encode(long id1, long id2, long id3) {
	        long[] a = new long[5];
	        a[0] = 0xFFFFFFFFL & (id1 + id2);
	        a[1] = 0xFFFFFFFFL & (id1 ^ id2);
	        a[2] = 0xFFFFFFFFL & (id1 + id3);
	        a[3] = 0xFFFFFFFFL & (id1 ^ id3);
	        a[4] = calcCrc(a, 8, id1, id2, id3);
	        return convertToByteArray(a, 8);
		}
		public boolean modelMatch(String str) { return str.matches("DEVO-6.*") || str.matches(".*devo6.*") || str.equals("6"); }	
	};

	
	public abstract byte[] encode(long id1, long id2, long id3);
	public abstract boolean modelMatch(String str);
	
	private String id;
	private String name;
	private int rootSectorOffset;
	private int rootSectorCount;
	private boolean rootInverted;
	private int mediaSectorOffset;
	private int mediaSectorCount;
	private boolean mediaInverted;
	private Transmitter(
			String name,
			String id,
			int rootSectorOffset,
			int rootSectorCount,
			boolean rootInverted,
			int mediaSectorOffset,
			int mediaSectorCount,
			boolean mediaInverted)
	{
		this.name = name;
		this.id = id;
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

}
