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
        List<DfuFile> dfuFiles;
        DevoDetect devoType;
        public ZipFile(String fname) {
            zipFile = fname;
            dfuFiles = new ArrayList<DfuFile>();
            files = GetFileList(fname);
            devoType = new DevoDetect();

            for (FileInfo file : files) {
                if (file.name().matches(".*\\.dfu")) {
                    DfuFile dfu = new DfuFile(file.data());
                    dfuFiles.add(dfu);
                    if (! devoType.Found()) {
                        for (DfuFile.ImageElement elem : dfu.imageElements()) {
                            if (devoType.Analyze(elem.name())) {
                                break;
                            }
                        }
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
        public List<DfuFile> listDfu() { return dfuFiles; }
        public DevoDetect devoInfo() { return devoType; }
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
    public List<DfuFile>GetDfuFiles() {
        List<DfuFile> dfus = new ArrayList<DfuFile>();
        for (ZipFile zipFile : zipFiles) {
            for (DfuFile dfu : zipFile.listDfu()) {
                dfus.add(dfu);
            }
        }
        return dfus;
    }
    public DevoDetect GetFirmwareInfo() {
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.devoInfo().type() == DevoDetect.Type.FIRMWARE) {
                return zipFile.devoInfo();
            }
        }
        return new DevoDetect();
    }
    public DfuFile GetFirmwareDfu() {
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.devoInfo().type() == DevoDetect.Type.FIRMWARE) {
                List<DfuFile>dfus = zipFile.listDfu();
                if (dfus.size() > 0) {
                    return dfus.get(0);
                }
            }
        }
        return null;
    }
    public DevoDetect GetLibraryInfo() {
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.devoInfo().type() == DevoDetect.Type.LIBRARY) {
                return zipFile.devoInfo();
            }
        }
        return new DevoDetect();
    }
    public List<DfuFile> GetLibraryDfus() {
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.devoInfo().type() == DevoDetect.Type.LIBRARY) {
                return zipFile.listDfu();
            }
        }
        return null;
    }
    public List<FileInfo> GetFilesystemFiles() {
        for (ZipFile zipFile : zipFiles) {
            if (zipFile.devoInfo().type() == DevoDetect.Type.LIBRARY) {
                return zipFile.list();
            }
        }
        return null;
    }
}