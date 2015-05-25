package deviation.filesystem.DevoFS;

import java.nio.ByteBuffer;

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
	
}
