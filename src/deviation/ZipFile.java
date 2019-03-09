package deviation;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFile {

    private static final Logger LOG = Logger.getLogger(ZipFile.class.getName());

    String zipFile;
    List<FileInfo> files;
    public ZipFile(String fname) {
        zipFile = fname;
        files = GetFileList(fname);

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
                    LOG.warning(String.format("Only read %d bytes from %s:%s (expected %d)", ostream.size(), zipFile, fname, ze.getSize()));
                } else {
                    buffer = ostream.toByteArray();
                }
                FileInfo file = new FileInfo(ze, buffer);
                file.setOwner(fname);
                files.add(file);
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
                     LOG.warning(String.format("Only read %d bytes from %s:%s (expected %d)", ostream.size(), zipFile, fname, ze.getSize()));
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
}
