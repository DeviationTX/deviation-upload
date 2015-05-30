package deviation.filesystem.DevoFS;

import de.waldheinz.fs.AbstractFsObject;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import java.io.IOException;

/**
 * Represents an entry in a {@link FatLfnDirectory}. Besides implementing the
 * {@link FsDirectoryEntry} interface for FAT file systems, it allows access
 * to the {@link #setArchiveFlag(boolean) archive},
 * {@link #setHiddenFlag(boolean) hidden},
 * {@link #setReadOnlyFlag(boolean) read-only} and
 * {@link #setSystemFlag(boolean) system} flags specifed for the FAT file
 * system.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 * @since 0.6
 */
public final class DevoFSDirectoryEntry
	extends AbstractFsObject
	implements FsDirectoryEntry {
        
    private DevoFSDirectory parent;
    private String fileName;
    private boolean isDir;
    private DevoFSFile file;
    private DevoFSDirectory directory;

    DevoFSDirectoryEntry(DevoFSFileSystem root, DevoFSDirectory parent, String name) {
    	super(false);
    	this.parent = parent;
    	fileName = name.toLowerCase();    	
    }
    DevoFSDirectoryEntry(DevoFSFileSystem root, DevoFSDirectory parent, String name, DevoFSDirectory dir) {
    	this(root, parent, name);
    	isDir = true;
    	directory = dir;
    }
    DevoFSDirectoryEntry(DevoFSFileSystem root, DevoFSDirectory parent, String name, DevoFSFile file) {
    	this(root, parent, name);
    	isDir = false;
    	this.file = file;
    }
    public String getName() {
        checkValid();
        
        return fileName;
    }
    
    public FsDirectory getParent() {
        checkValid();
        
        return parent;
    }
    
    public void setName(String newName) throws IOException {
    	fileName = newName.toLowerCase();
    }
    public void setLastModified(long lastModified) {
        checkWritable();
    }
    
    public DevoFSFile getFile() throws IOException {
    	if (! isDir) {
    		return file;
    	}
    	throw new IOException();
    }
    
    public DevoFSDirectory getDirectory() throws IOException {
    	if (isDir) {
    		return directory;
    	}
    	throw new IOException();
    }
    
     public long getLastModified() {
        return 0;
    }

    public long getCreated() {
        return 0;
    }

    public long getLastAccessed() {
        return 0;
    }

    public boolean isFile() {
    	return isDir ? false : true;
    }

    public boolean isDirectory() {
    	return isDir ? true : false;
    }

    public boolean isDirty() {
    	return false;
    }


}
