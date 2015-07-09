/*
 * Copyright (C) 2009-2013 Matthias Treydte <mt@waldheinz.de>
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package deviation.filesystem;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.ReadOnlyException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/* This is a copy of the FileDisk class, but with an adjustable sector size.
 * Because FileDisk is 'final' I had to copy the whole thing rather than extend it.
 * Also, BYTES_PER_SECTOR is no longer static
 */

/**
 * This is a {@code BlockDevice} that uses a {@link File} as it's backing store.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public final class FileDisk2 implements BlockDevice {

    /**
     * The number of bytes per sector for all {@code FileDisk} instances.
     */
    public final int BYTES_PER_SECTOR;

    private final RandomAccessFile raf;
    private final FileChannel fc;
    private final boolean readOnly;
    private final double bytesPerSec;
    private boolean closed;

    /**
     * Creates a new instance of {@code FileDisk} for the specified
     * {@code File}.
     *
     * @param file the file that holds the disk contents
     * @param readOnly if the file should be opened in read-only mode, which
     *      will result in a read-only {@code FileDisk} instance
     * @throws FileNotFoundException if the specified file does not exist
     * @see #isReadOnly() 
     */
    public FileDisk2(File file, boolean readOnly, int bytesPerSector, double bytesPerSec) throws FileNotFoundException {
    	BYTES_PER_SECTOR = bytesPerSector;
        if (!file.exists()) throw new FileNotFoundException();

        this.readOnly = readOnly;
        this.closed = false;
        final String modeString = readOnly ? "r" : "rw"; //NOI18N
        this.raf = new RandomAccessFile(file, modeString);
        this.fc = raf.getChannel();
        this.bytesPerSec = bytesPerSec;
    }
    public FileDisk2(File file, boolean readOnly, int bytesPerSector) throws FileNotFoundException {
    	this(file,  readOnly,  bytesPerSector, 0.0);
    }

    private FileDisk2(RandomAccessFile raf, boolean readOnly, int bytesPerSector, double bytesPerSec) {
    	BYTES_PER_SECTOR = bytesPerSector;
        this.closed = false;
        this.raf = raf;
        this.fc = raf.getChannel();
        this.readOnly = readOnly;
        this.bytesPerSec = bytesPerSec;
    }
    private FileDisk2(RandomAccessFile raf, boolean readOnly, int bytesPerSector) {
    	this(raf, readOnly, bytesPerSector, 0.0);
    }

	/**
     * Creates a new {@code FileDisk} of the specified size. The
     * {@code FileDisk} returned by this method will be writable.
     *
     * @param file the file to hold the {@code FileDisk} contents
     * @param size the size of the new {@code FileDisk}
     * @return the created {@code FileDisk} instance
     * @throws IOException on error creating the {@code FileDisk}
     * @throws IllegalArgumentException if size is &lt; 0
     */
    public static FileDisk2 create(File file, long size, int bytesPerSector)
            throws IOException, IllegalArgumentException {
        
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        
        try {
            final RandomAccessFile raf =
                    new RandomAccessFile(file, "rw"); //NOI18N
            raf.setLength(size);
            
            return new FileDisk2(raf, false, bytesPerSector);
        } catch (FileNotFoundException ex) {
            throw new IOException(ex);
        }
    }
    
    public long getSize() throws IOException {
        checkClosed();
        
        return raf.length();
    }

    public void read(long devOffset, ByteBuffer dest) throws IOException {
        checkClosed();

        int toRead = dest.remaining();
        try {
        	Thread.sleep((int)(1000 * toRead / bytesPerSec));
        } catch (Exception e) {e.printStackTrace(); }
        if ((devOffset + toRead) > getSize()) throw new IOException(
                "reading past end of device");

        while (toRead > 0) {
            final int read = fc.read(dest, devOffset);
            if (read < 0) throw new IOException();
            toRead -= read;
            devOffset += read;
        }
    }

    public void write(long devOffset, ByteBuffer src) throws IOException {
        checkClosed();

        if (this.readOnly) throw new ReadOnlyException();
        
        int toWrite = src.remaining();

        if ((devOffset + toWrite) > getSize()) throw new IOException(
                "writing past end of file");

        while (toWrite > 0) {
            final int written = fc.write(src, devOffset);
            if (written < 0) throw new IOException();
            toWrite -= written;
            devOffset += written;
        }
    }

    public void flush() throws IOException {
        checkClosed();
    }

    public int getSectorSize() {
        checkClosed();
        
        return BYTES_PER_SECTOR;
    }

    public void close() throws IOException {
        if (isClosed()) return;

        this.closed = true;
        this.fc.close();
        this.raf.close();
    }
    
    public boolean isClosed() {
        return this.closed;
    }

    private void checkClosed() {
        if (closed) throw new IllegalStateException("device already closed");
    }

    public boolean isReadOnly() {
        checkClosed();
        
        return this.readOnly;
    }

}