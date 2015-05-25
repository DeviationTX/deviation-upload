package deviation.filesystem.DevoFS;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import de.waldheinz.fs.BlockDevice;

public class DevoFSLayout {
	private BlockDevice dev;
	private DevoFSFileSystem root;
	private int sectorSize;
	private int size;
	private final static int START_SECTOR = 0xFF;
	private final static int EMPTY_SECTOR = 0x00;
	private List<DevoFSDirectoryEntry> elems;
	private HashMap<Integer, DevoFSDirectory> dirMap;
 	
	DevoFSLayout(DevoFSFileSystem root, BlockDevice dev, DevoFSDirectory rootDir) {
		this.root = root;
		this.dev = dev;
		elems = new ArrayList<DevoFSDirectoryEntry>();
		dirMap = new HashMap<Integer, DevoFSDirectory>();
		dirMap.put(0, rootDir);
		sectorSize = 0;
		size = 0;
		try {
			sectorSize = dev.getSectorSize();
			size = (int)dev.getSize();
		} catch (Exception e) {}
	}
	static List<DevoFSDirectoryEntry> parse(DevoFSFileSystem root, BlockDevice dev, DevoFSDirectory rootDir) {
		DevoFSLayout layout = new DevoFSLayout(root, dev, rootDir);
		return layout.parse();
	}
	public List<DevoFSDirectoryEntry> parse() {
		try {
			ByteBuffer buf = ByteBuffer.allocate(size);
			dev.read(0, buf); //Read the entire FLASH memory
			ByteBuffer data = alignData(buf);
			while(data.hasRemaining()) {
				if (! getNextEntry(data)) {
					break;
				}
			}
		} catch (Exception e) {System.out.println(e); }
		return elems;
	}
	private ByteBuffer alignData(ByteBuffer buf) {
		byte data[] = new byte [buf.capacity()];
		Arrays.fill(data,  (byte)0);
		buf.position(0);
		int sectorHeader = 0;
		while(buf.position() < buf.capacity() && (sectorHeader = ((int)buf.get()&0xff)) != START_SECTOR) {
			buf.position(buf.position() + sectorSize);
		}
		if (sectorHeader != START_SECTOR) {
			System.err.format("Failed to find start sector");
			return ByteBuffer.wrap(data);
		}
		int start = buf.position()-1;
		int datapos = 0;
		while(buf.hasRemaining()) {
			buf.get(data, datapos, sectorSize-1);
			datapos += sectorSize -1;
			if (buf.hasRemaining()) {
				if (((int)buf.get()&0xff) == EMPTY_SECTOR) {
					break;
				}
			}
		}
		if (start != 0 && ! buf.hasRemaining()) {
			buf.position(0);
			buf.limit(start);
			while(buf.hasRemaining() && ((int)buf.get()&0xff) != EMPTY_SECTOR) {
				buf.get(data, datapos, sectorSize-1);
				datapos += sectorSize -1;
			}
		}
		return ByteBuffer.wrap(data);
	}
	private boolean getNextEntry(ByteBuffer data) {
		DevoFSEntry header = new DevoFSEntry(data);
		DevoFSDirectoryEntry entry;
		if (header.getType() == DevoFSEntry.DIR_ENTRY) {
			DevoFSDirectory dir = new DevoFSDirectory(root);
			dirMap.put(header.getId(), dir);
			entry = new DevoFSDirectoryEntry(root, dirMap.get(header.getParentId()), header.getName(), dir);
			elems.add(entry);
		} else if (header.getType() == DevoFSEntry.FILE_ENTRY){
			DevoFSFile file = new DevoFSFile(header.getData());
			entry = new DevoFSDirectoryEntry(root, dirMap.get(header.getParentId()) ,header.getName(), file);
			elems.add(entry);
		} else if (header.getType() == DevoFSEntry.LAST_ENTRY){
			return false;
		}
		return true;
	}
}
