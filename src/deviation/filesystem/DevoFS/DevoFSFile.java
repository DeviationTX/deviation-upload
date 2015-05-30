package deviation.filesystem.DevoFS;

import de.waldheinz.fs.AbstractFsObject;
import java.io.IOException;
import de.waldheinz.fs.FsFile;
import de.waldheinz.fs.ReadOnlyException;
import java.nio.ByteBuffer;

public final class DevoFSFile extends AbstractFsObject implements FsFile {
    private ByteBuffer data;
    int size;
    
    public DevoFSFile(ByteBuffer data) {
    	super(false);
    	if (data != null) {
    		this.data = data.duplicate();
    	} else {
    		this.data = ByteBuffer.allocate(0);
    	}
    	size = this.data.capacity();
    }
    /**
     * Returns the length of this file in bytes. This is the length that
     * is stored in the directory entry that is associated with this file.
     * 
     * @return long the length that is recorded for this file
     */
    public long getLength() {
    	checkValid();
    	return size;
    }
    
    /**
     * Sets the size (in bytes) of this file. Because
     * {@link #write(long, java.nio.ByteBuffer) writing} to the file will grow
     * it automatically if needed, this method is mainly usefull for truncating
     * a file. 
     *
     * @param length the new length of the file in bytes
     * @throws ReadOnlyException if this file is read-only
     * @throws IOException on error updating the file size
     */
    public void setLength(long length) throws ReadOnlyException, IOException {
        checkWritable();
        if (length > data.capacity()) {
        	ByteBuffer tmp = ByteBuffer.allocate((int)length);
        	data.rewind();
        	tmp.put(data);
        	data = tmp;
        }
        size = (int)length;
    }
    
    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * Unless this file is {@link #isReadOnly() read-ony}, this method also
     * updates the "last accessed" field in the directory entry that is
     * associated with this file.
     * </p>
     * 
     * @param offset {@inheritDoc}
     * @param dest {@inheritDoc}
     * @see FatDirectoryEntry#setLastAccessed(long)
     */
    public void read(long offset, ByteBuffer dest) throws IOException {
        checkValid();
        int requestedSize = dest.remaining();
        if (requestedSize > size - offset) {
        	requestedSize = size - (int)offset;
        }
        data.position((int)offset);
        data.limit((int)offset + requestedSize);
        dest.put(data);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * If the data to be written extends beyond the current
     * {@link #getLength() length} of this file, an attempt is made to
     * {@link #setLength(long) grow} the file so that the data will fit.
     * Additionally, this method updates the "last accessed" and "last modified"
     * fields on the directory entry that is associated with this file.
     * </p>
     *
     * @param offset {@inheritDoc}
     * @param srcBuf {@inheritDoc}
     */
    public void write(long offset, ByteBuffer srcBuf)
            throws ReadOnlyException, IOException {

        checkWritable();
        int requestedSize = (int)offset + srcBuf.remaining();
        if (requestedSize > size) {
        	setLength(requestedSize);
        }
        data.limit(requestedSize);
        data.position((int)offset);
        data.put(srcBuf);
    }
    /**
     * Has no effect besides possibly throwing an {@code ReadOnlyException}. To
     * make sure that all data is written out to disk use the
     * {@link FatFileSystem#flush()} method.
     *
     * @throws ReadOnlyException if this {@code FatFile} is read-only
     */
    public void flush() throws ReadOnlyException {
        checkWritable();
        
        /* nothing else to do */
    }

}
