package deviation;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

public class ZipFileGroup {

    private List<ZipFile> zipFiles;

    private class ZipFile {
        String zipFile;
        List<FileInfo> files;
        DfuFile dfuFirmware;
        List<DfuFile> dfuLibs;
        DevoDetect devoType;
        public ZipFile(String fname) {
            zipFile = fname;
            dfuLibs = new ArrayList<DfuFile>();
            files = GetFileList(fname);
            devoType = new DevoDetect();

            for (FileInfo file : files) {
            	if (file.name().equals("tx.ini")) {
            		devoType.isLibrary(true);
            	}
                if (file.name().matches(".*\\.dfu")) {
                    DfuFile dfu = new DfuFile(file.data());
                    DevoDetect type = new DevoDetect();
                    for (DfuFile.ImageElement elem : dfu.imageElements()) {
                        if (type.Analyze(elem.name())) {
                            devoType.update(type);
                            break;
                        }
                    }
                    if (type.isFirmware()) {
                    	dfuFirmware = dfu;
                    } else if (type.isLibrary()) {
                    	dfuLibs.add(dfu);
                    }
                }
            }
            if (! devoType.Found()) {
                devoType.Analyze(fname);
            }
        }
        private List<FileInfo> GetFileList(String fname) {
            List<FileInfo>files = new ArrayList<FileInfo>();
            try {
                ZipInputStream zis =
                        new ZipInputStream(new FileInputStream(fname));
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null)
                {
                    if (ze.isDirectory()) // Ignore directory-only entries stored in
                        continue;          // archive.
                    byte[] buffer = null;
                    byte[] data = new byte[4096];
                    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                    int len;
                    while((len = zis.read(data, 0, data.length)) > 0) {
                        ostream.write(data, 0, len);
                    }
                    ostream.flush();
                    if (ostream.size() != ze.getSize()) {
                        System.out.format("Only read %d bytes from %s:%s (expected %d)%n", ostream.size(), zipFile, fname, ze.getSize());
                    } else {
                        buffer = ostream.toByteArray();
                    }
                    files.add(new FileInfo(ze, buffer));
                    zis.closeEntry();
                }
                zis.close();
            }
            catch (Exception e) {}
            return files;
        }
        public byte[] read(String fname) {
            byte[] buffer = null;
            try {
                ZipInputStream zis =
                    new ZipInputStream(new FileInputStream(zipFile));
               ZipEntry ze;
               while ((ze = zis.getNextEntry()) != null)
               {
                  if (ze.getName().equals(fname)) {
                     byte[] data = new byte[4096];
                     ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                     int len;
                     while((len = zis.read(data, 0, data.length)) > 0) {
                         ostream.write(data, 0, len);
                     }
                     ostream.flush();
                     if (ostream.size() != ze.getSize()) {
                         System.out.format("Only read %d bytes from %s:%s (expected %d)%n", ostream.size(), zipFile, fname, ze.getSize());
                     } else {
                         buffer = ostream.toByteArray();
                     }
                  }
                  zis.closeEntry();
               }
               zis.close();
            } catch(Exception e) {}
            return buffer;
        }

        public List<FileInfo> list() { return files; }
        public String name() { return zipFile; }
        //public List<DfuFile> listDfu() { return dfuFiles; }
        public DevoDetect devoInfo() { return devoType; }
        public DfuFile getFirmwareDfu() { return dfuFirmware; }
        public List<DfuFile> getLibraryDfus() { return dfuLibs; }
    }
    
    public ZipFileGroup() {
        zipFiles = new ArrayList<ZipFile>();
    }
    public void AddZipFile(String zip) {
        ZipFile zfile = new ZipFile(zip);
        zipFiles.add(zfile);
    }
    public void RemoveZipFile(String zip) {
        int index = 0;
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.name().equals(zip)) {
                zipFiles.remove(index);
                return;
            }
            index++;
        }
    }
    //public List<DfuFile>GetDfuFiles() {
    //    List<DfuFile> dfus = new ArrayList<DfuFile>();
    //    for (ZipFile zipFile : zipFiles) {
    //        for (DfuFile dfu : zipFile.listDfu()) {
    //            dfus.add(dfu);
    //        }
    //    }
    //    return dfus;
    //}
    public DevoDetect GetFirmwareInfo() {
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.devoInfo().isFirmware()) {
                return zipFile.devoInfo();
            }
        }
        return new DevoDetect();
    }
    public DfuFile GetFirmwareDfu() {
        for (ZipFile zipFile : zipFiles) {
        	DfuFile fw = zipFile.getFirmwareDfu();
        	if (fw != null) {
        		return fw;
        	}
        }
        return null;
    }
    public DevoDetect GetLibraryInfo() {
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.devoInfo().isLibrary()) {
                return zipFile.devoInfo();
            }
        }
        return new DevoDetect();
    }
    public List<DfuFile> GetLibraryDfus() {
        List<DfuFile> dfus = new ArrayList<DfuFile>();
        for (ZipFile zipFile : zipFiles) {
        	dfus.addAll(zipFile.getLibraryDfus());
        }
        return dfus;

    }
    public List<FileInfo> GetFilesystemFiles() {
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.devoInfo().isLibrary()) {
                return zipFile.list();
            }
        }
        return null;
    }
    public String firmwareZip() {
    	for (ZipFile zipFile : zipFiles) {
    		if (zipFile.devoInfo().isFirmware()) {
    			return zipFile.name();
    		}
    	}
    	return "";
    }
    public String libraryZip() {
    	for (ZipFile zipFile : zipFiles) {
    		if (zipFile.devoInfo().isLibrary()) {
    			return zipFile.name();
    		}
    	}
    	return "";
    }
}