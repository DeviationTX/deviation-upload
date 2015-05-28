package deviation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class FileGroup {

	private List<FileInfo> files;
	private List<DfuFile> dfuFiles;
    
    public FileGroup() {
    	files = new ArrayList<FileInfo>();
    	dfuFiles = new ArrayList<DfuFile>();
    }
    private void analyzeZipFile(ZipFile zip) {
        List<FileInfo> files = zip.list();

        for (FileInfo file : files) {
            if (file.name().matches("(?i:.*\\.dfu)")) {
                DfuFile dfu = new DfuFile(file);
                dfu.setOwner(zip.name());
                if (! dfu.type().Found()) {
                	DevoDetect type = new DevoDetect();
                	if (type.Analyze(zip.name())) {
                		dfu.setType(type);
                	}
                }
                dfuFiles.add(dfu);
            } else {
            	if (file.name().matches("(?i:.*\\.zip)") || file.name().matches("(?i:.*\\.md)")) {
            		continue;
            	}
            	this.files.add(file);
            }
        }
    }
    public void AddFile(String file) {
    	if (file.matches("(?i:.*\\.zip)")) {
    		ZipFile zfile = new ZipFile(file);
    		analyzeZipFile(zfile);
    	}
    }
    public void RemoveFile(String zip) {
    	for (Iterator<FileInfo> iter = files.listIterator(); iter.hasNext(); ) {
    		FileInfo file = iter.next();
    		if (file.owner().equalsIgnoreCase(zip)) {
    	        iter.remove();
    	    }
    	}
    	for (Iterator<DfuFile> iter = dfuFiles.listIterator(); iter.hasNext(); ) {
    		DfuFile dfu = iter.next();
    		if (dfu.owner().equalsIgnoreCase(zip)) {
    	        iter.remove();
    	    }
    	}
    }
    public DevoDetect GetFirmwareInfo() {
    	DfuFile dfu = GetFirmwareDfu();
    	if (dfu != null) {
    		return dfu.type();
    	}
    	return new DevoDetect();
    }
    public DfuFile GetFirmwareDfu() {
    	for (DfuFile dfu : dfuFiles) {
    		if (dfu.type().isFirmware()) {
                return dfu;
    		}
        }
        return null;
    }
    public List<DfuFile> GetLibraryDfus() {
        List<DfuFile> dfus = new ArrayList<DfuFile>();
        for (DfuFile dfu : dfuFiles) {
        	if (dfu.type().isLibrary()) {
        		dfus.add(dfu);
        	}
        }
        return dfus;
    }
    public List<FileInfo> GetFilesystemFiles() {
    	return files;
    }
    public String firmwareZip() {
    	DfuFile dfu = GetFirmwareDfu();
    	if (dfu != null) {
    		return dfu.owner();
    	}
    	return "";
    }
    public String libraryZip() {
    	if (files.size() > 0) {
    		return files.get(0).owner();
    	}
    	for (DfuFile dfu : dfuFiles) {
    		if (dfu.type().isLibrary()) {
    			return dfu.owner();
    		}
    	}
    	return "";
    }
    public boolean hasLibrary() {
    	return libraryZip() != "";
    }
}