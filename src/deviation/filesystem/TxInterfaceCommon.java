package deviation.filesystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import deviation.FileInfo;

public class TxInterfaceCommon {
    protected static List<FileInfo> readDirRecur(String parent, FsDirectory dir) throws IOException {
    	List <FileInfo>files = new ArrayList<FileInfo>();
        Iterator<FsDirectoryEntry> itr = dir.iterator();
    	while(itr.hasNext()) {
    		FsDirectoryEntry entry = itr.next();
    		if (entry.isDirectory()) {
    			if (entry.getName().equals(".") || entry.getName().equals(".."))
    				continue;
    			System.out.format("DIR: %s\n", entry.getName());
    			files.addAll(readDirRecur(parent + entry.getName() + "/", entry.getDirectory()));
    		} else {
    			files.add(new FileInfo(parent + entry.getName(), (int)entry.getFile().getLength()));
    			System.out.format("FILE: %s (%d)\n", entry.getName(), entry.getFile().getLength());
    		}
    	}
    	return files;
    }
    protected void readDir(FileSystem fs, String dirStr) {
        String[] dirs = dirStr.split("/");
        try {
        	FsDirectory dir = fs.getRoot();
        	for (String subdir : dirs) {
        		if (subdir.equals("")) {
        			continue;
        		}
        		dir = dir.getEntry(subdir).getDirectory();
        	}
        	Iterator<FsDirectoryEntry> itr = dir.iterator();
        	while(itr.hasNext()) {
        		FsDirectoryEntry entry = itr.next();
        		System.out.println(entry.getName());
        	}
        } catch (IOException e) { e.printStackTrace(); }
    }

}
