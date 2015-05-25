package deviation.filesystem;

import java.nio.ByteBuffer;
import java.util.Arrays;

import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsFile;
import deviation.FileInfo;

public class FSUtils {
	public FSUtils() {
	}
	public void copyFile(FileSystem fs, FileInfo file) {
		String[]filepath = file.name().toUpperCase().split("/");
		String filename = filepath[filepath.length-1];
		
		String[]filedir;
		if (filepath.length > 1) {
			filedir = Arrays.copyOfRange(filepath, 0, filepath.length-1);
		} else {
			filedir = new String[0];
		}
		FsDirectory fsdir;
    	FsDirectoryEntry fs_entry;
		try {
			fsdir = fs.getRoot();
		} catch (Exception e) {
			System.err.println("Couldn't get root dir: " + e.getMessage());
			return;
		}
        for (String subdir : filedir) {
            if (subdir.equals("")) {
               continue;
            }
            try {
            	fs_entry = fsdir.getEntry(subdir);
            	if (fs_entry == null) {
            		fs_entry = fsdir.addDirectory(subdir);
            		if (fs_entry == null) {
            			System.err.println("Couldn't create directory '" + subdir);
            			return;
            		}
            	}
            	fsdir = fs_entry.getDirectory();
            } catch (Exception e) {
            	System.err.println("Unexpected error: " + e.getMessage());
            	return;
            }
        }
		try {
			fs_entry = fsdir.getEntry(filename);
			if (fs_entry == null) {
				fs_entry = fsdir.addFile(filename);
				if (fs_entry == null) {
					System.err.println("Failed to create file '" + file.name());
					return;
				}
			}
			if (! fs_entry.isFile()) {
				System.err.println(file.name() + " exists but is not a file");
				return;
			}
			FsFile fs_file = fs_entry.getFile();
			ByteBuffer byteBuffer = ByteBuffer.allocate(file.data().length);
			byteBuffer.put(file.data());
			byteBuffer.flip();
			fs_file.write(0, byteBuffer);
		} catch (Exception e) {
        	System.err.println("Unexpected error: " + e.getMessage());
        	return;
		}
	}
}
