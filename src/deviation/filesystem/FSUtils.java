package deviation.filesystem;

import java.nio.ByteBuffer;
import java.util.Arrays;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsFile;
import deviation.FileInfo;

public class FSUtils {
	public static void copyFile(FileSystem fs, FileInfo file) {
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
            		fs_entry.setLastModified(0); //Directories get an epoch date
            	}
            	fsdir = fs_entry.getDirectory();
            } catch (Exception e) {
            	e.printStackTrace();
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
			fs_entry.setLastModified(file.time());
		} catch (Exception e) {
			e.printStackTrace();
        	return;
		}
	}
    public static boolean DetectFS(BlockDevice blkDev, FSType type)
    {
    	if (type == FSType.FAT) {
    		try {
    			ByteBuffer buf = ByteBuffer.allocate(0x200);
    			blkDev.read(0,  buf);
    			byte fatBytes[] = buf.array();
        		//Magic bytes indicating FAT:
        		//end of sector (510,511) must be 0x55aa
        		//Fat type (54,55,56,57,58) must be FAT16 (we actually only check the 1st 2 bytes as sufficient)
    			if (fatBytes[510] == 0x55 && ((int)fatBytes[511] & 0xff) == 0xaa
    					&& fatBytes[54] == 0x46 && fatBytes[55] == 0x41)
    			{
    				return true;
    			}
    		} catch (Exception e) { e.printStackTrace(); }
    	} else {
    		return true;
    	}
    	return false;
    }

}
