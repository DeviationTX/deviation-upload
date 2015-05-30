package deviation.filesystem.DevoFS;

import de.waldheinz.fs.AbstractFileSystem;
import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FsDirectoryEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * Implements the {@code FileSystem} interface for the FAT family of file
 * systems. This class always uses the "long file name" specification when
 * writing directory entries.
 * </p><p>
 * For creating (aka "formatting") FAT file systems please refer to the
 * {@link SuperFloppyFormatter} class.
 * </p>
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class DevoFSFileSystem extends AbstractFileSystem {
	private final int ENTRY_SIZE = 16;
	private final DevoFSDirectory rootDir;
    List<DevoFSDirectoryEntry> elems;
    BlockDevice dev;

    public DevoFSFileSystem(BlockDevice device, boolean readOnly) throws IOException {
        super(readOnly);
        dev = device;
        rootDir = new DevoFSDirectory(this);
        elems = DevoFSLayout.parse(this, dev, rootDir);
    }
    private DevoFSFileSystem(BlockDevice device) throws IOException {
        super(false);
        dev = device;
        rootDir = new DevoFSDirectory(this);    	
        elems = new ArrayList<DevoFSDirectoryEntry>();
    }
    /**
     * Reads the file system structure from the specified {@code BlockDevice}
     * and returns a fresh {@code FatFileSystem} instance to read or modify
     * it.
     *
     * @param device the {@code BlockDevice} holding the file system
     * @param readOnly if the {@code FatFileSystem} should be in read-only mode
     * @return the {@code FatFileSystem} instance for the device
     * @throws IOException on read error or if the file system structure could
     *      not be parsed
     */
    public static DevoFSFileSystem read(BlockDevice device, boolean readOnly)
            throws IOException
    {        
        return new DevoFSFileSystem(device, readOnly);
    }
    public static DevoFSFileSystem format(BlockDevice device)
    		throws IOException
    {
    	return new DevoFSFileSystem(device);
    }

    public DevoFSDirectory getRoot() {
    	return rootDir;
    }
    /**
     * Flush all changed structures to the device.
     * 
     * @throws IOException on write error
     */
    public void flush() throws IOException {
    	DevoFSLayout.write(dev, rootDir, elems);
    }
    /**
     * The free space of this file system.
     *
     * @return if -1 this feature is unsupported
     */
    public long getFreeSpace() {
    	long size = 0;
    	try {
    		//Leave one sector empty and account for 1 byte per sector overhead
    		size = dev.getSize() - dev.getSectorSize() - (dev.getSize() / dev.getSectorSize() - 1);
    		for (DevoFSDirectoryEntry elem: elems) {
    			size -= ENTRY_SIZE;
    			if (elem.isFile()) {
    				size -= elem.getFile().getLength();
    			}
    		}
    	} catch(Exception e) { e.printStackTrace(); };
    	return size;
    }
    /**
     * The total size of this file system.
     *
     * @return if -1 this feature is unsupported
     */
    public long getTotalSpace() {
    	try {
			return dev.getSize();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return 0;
    }
    /**
     * The usable space of this file system.
     *
     * @return if -1 this feature is unsupported
     */
    public long getUsableSpace() {
    	return -1;
    }
    protected DevoFSDirectoryEntry addFile(DevoFSDirectory parent, String name) {
    	DevoFSFile file = new DevoFSFile(null);
    	DevoFSDirectoryEntry elem = new DevoFSDirectoryEntry(this, parent, name, file);
    	elems.add(elem);
    	return elem;
    }
    protected DevoFSDirectoryEntry addDirectory(DevoFSDirectory parent, String name) {
    	DevoFSDirectory dir = new DevoFSDirectory(this);
    	DevoFSDirectoryEntry elem = new DevoFSDirectoryEntry(this, parent, name, dir);
    	elems.add(elem);
    	return elem;
    }
    protected DevoFSDirectoryEntry findEntry(DevoFSDirectory parent, String name) {
    	for (DevoFSDirectoryEntry elem : elems) {
    		if (elem.getParent() == parent && elem.getName().equalsIgnoreCase(name)) {
    			return elem;
    		}
    	}
    	return null;
    }
    protected void removeEntry(DevoFSDirectory parent, String name) {
    	for (DevoFSDirectoryEntry elem : elems) {
    		if (elem.getParent() == parent && elem.getName().equalsIgnoreCase(name)) {
    			elems.remove(elem);
    			return;
    		}
    	}    	
    }
    protected Iterator<FsDirectoryEntry> dirIterator(final DevoFSDirectory parent) {
    	return new Iterator<FsDirectoryEntry>() {
    		private int idx = 0;
    		final DevoFSDirectory dir = parent;

    		public boolean hasNext() {
    			int nextIdx = idx;
    			while (nextIdx < elems.size() && elems.get(nextIdx).getParent() != dir) {
    				nextIdx++;
    			}
    			return nextIdx == elems.size() ? false: true;
    		}

    		public FsDirectoryEntry next() {
    			while (idx < elems.size() && elems.get(idx).getParent() != dir) {
    				idx++;
    			}
    			return elems.get(idx++);
    		}

    		/**
    		 * @see java.util.Iterator#remove()
    		 */
    		public void remove() {
    			throw new UnsupportedOperationException();
    		}
    	};
    }
}
