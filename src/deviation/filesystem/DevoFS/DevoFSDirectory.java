package deviation.filesystem.DevoFS;

import de.waldheinz.fs.AbstractFsObject;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import java.io.IOException;
import java.util.Iterator;

public final class DevoFSDirectory
	extends AbstractFsObject
	implements FsDirectory {
	DevoFSFileSystem root;


	DevoFSDirectory(DevoFSFileSystem root) {
		super(false);
		this.root = root;
	}
    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * According to the FAT file system specification, leading and trailing
     * spaces in the {@code name} are ignored by this method.
     * </p>
     * 
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public DevoFSDirectoryEntry addFile(String name) throws IOException {
    	return root.addFile(this, name);
    }
    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * According to the FAT file system specification, leading and trailing
     * spaces in the {@code name} are ignored by this method.
     * </p>
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public DevoFSDirectoryEntry addDirectory(String name) throws IOException {
    	return root.addDirectory(this, name);
    }
    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * According to the FAT file system specification, leading and trailing
     * spaces in the {@code name} are ignored by this method.
     * </p>
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     */
    public DevoFSDirectoryEntry getEntry(String name) {
    	return root.findEntry(this, name);
    }
    
    public void flush() throws IOException {
    }
    
    public Iterator<FsDirectoryEntry> iterator() {
    	return root.dirIterator(this);
    }
    /**
     * Remove the entry with the given name from this directory.
     * 
     * @param name the name of the entry to remove
     * @throws IOException on error removing the entry
     * @throws IllegalArgumentException on an attempt to remove the dot entries
     */
    public void remove(String name)
            throws IOException, IllegalArgumentException {
    	root.removeEntry(this, name);
    }
}

