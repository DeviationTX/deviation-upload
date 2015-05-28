package deviation.filesystem.DevoFS;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;

public class DevoFSEntry {
	public final static int LAST_ENTRY = 0x00;
	public final static int FILE_ENTRY = 0xF7;
	public final static int DIR_ENTRY  = 0x7F;
	private int type;
	private int parentId;
	private int id;
	private String name;
	private int size;
	private ByteBuffer data;
	public DevoFSEntry(ByteBuffer data) {
		type = (int)data.get() & 0xff;
		if (type == LAST_ENTRY) {
			return;
		}
		parentId = (int)data.get() & 0xff;
		byte byteName[] = new byte[12];
		data.get(byteName, 0, 8); //basename
		byteName[8] =0;
		name = new String(byteName).split("\0")[0];
		data.get(byteName, 0, 3); //extension
		byteName[3] =0;
		String ext = new String(byteName).split("\0")[0];
		if (ext.length() > 0) {
			name = name + "." + ext;
		}
		if (type == DIR_ENTRY) {
			id = data.get();
			data.get();
			data.get();
		} else {
			size = ((((int)data.get() & 0xff) << 16)
					| (((int)data.get() & 0xff) << 8)
					| ((int)data.get() & 0xff));
			data.limit(data.position()+size);
			this.data = data.slice();
			data.limit(data.capacity());
			data.position(data.position()+size);
		}
	}
	public int getType() { return type; }
	public int getParentId() { return parentId; }
	public int getId() { return id; }
	public int getSize() { return size; }
	public String getName() { return name; }
	public ByteBuffer getData() { return data; }
	private static void writeName(ByteBuffer buf, String filename) {
		byte name[] = filename.getBytes(Charset.forName("UTF-8"));
		int namepos;
		int bufpos;
		for (namepos = 0, bufpos = 0; namepos < name.length; namepos++, bufpos++) {
			if (name[namepos] != 0 && name[namepos] != '.') {
				buf.put(name[namepos]);
			} else {
				break;
			}
		}
		while (bufpos < 8) {
			buf.put((byte)0);
			bufpos++;
		}
		for (; namepos < name.length; namepos++, bufpos++) {
			if (name[namepos] == '.') {
				continue;
			}
			if (name[namepos] == 0) {
				break;
			}
			buf.put(name[namepos]);
		}
		while (bufpos < 11) {
			buf.put((byte)0);
			bufpos++;
		}
		
	}
	public static void toDisk(ByteBuffer buf, HashMap<DevoFSDirectory, Integer> mapDir, DevoFSDirectoryEntry elem) {
		buf.put(elem.isDirectory() ? (byte)DIR_ENTRY : (byte)FILE_ENTRY);
		int parent = mapDir.get(elem.getParent());
		buf.put((byte)parent);
		writeName(buf, elem.getName());
		try {
			if (elem.isDirectory()) {
				int id = mapDir.get(elem.getDirectory());
				buf.put((byte)id);
				buf.put((byte)0);
				buf.put((byte)0);
			} else {
				long size = elem.getFile().getLength();
				buf.put((byte)((size >> 16) & 0xff));
				buf.put((byte)((size >>  8) & 0xff));
				buf.put((byte)((size >>  0) & 0xff));
				buf.limit(buf.position() + (int)size);
				elem.getFile().read(0, buf);
				buf.limit(buf.capacity());
			}
		} catch (Exception e) { System.out.println(e); }
	}
}
