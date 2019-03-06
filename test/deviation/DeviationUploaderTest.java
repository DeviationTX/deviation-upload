package deviation;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.util.FileDisk;
import deviation.filesystem.DevoFS.DevoFSFileSystem;
import deviation.filesystem.FSUtils;
import deviation.filesystem.FileDisk2;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.Assert.*;

public class DeviationUploaderTest {

    @Test
    public void an_exsample_test_current_dir_contains_files() {
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();

        assertThat(files).isNotNull();
        assertThat(files.length).isGreaterThan(0);
    }

    public void test() throws Exception {
        BlockDevice bd = new FileDisk(new File("test.fat"), false);
        FileSystem fs = FatFileSystem.read(bd, false);
        FileGroup zips = new FileGroup();
        zips.AddFile("test.zip");
        for (FileInfo file : zips.GetFilesystemFiles()) {
            FSUtils.copyFile(fs, file);
        }
        fs.close();
        bd.close();
            /*
            String[]dirs = "/media/".split("/");
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
            */
    }


    private void test1_recur(String indent, FsDirectory dir) {
        Iterator<FsDirectoryEntry> itr = dir.iterator();
        while (itr.hasNext()) {
            try {
                FsDirectoryEntry entry = itr.next();
                if (entry.isDirectory()) {
                    System.out.format("%sDIR: %s\n", indent, entry.getName());
                    test1_recur(indent + "    ", entry.getDirectory());
                } else {
                    System.out.format("%sFILE: %s (%d)\n", indent, entry.getName(), entry.getFile().getLength());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void test1() throws Exception {

        FileDisk2 f = new FileDisk2(new File("test.devofs"), false, 4096);
        DevoFSFileSystem fs = new DevoFSFileSystem(f, false);
        FsDirectory dir = fs.getRoot();
        test1_recur("", dir);
        ByteBuffer dest = ByteBuffer.allocate((int) f.getSize());
        Arrays.fill(dest.array(), (byte) 0);
        f.write(0, dest);
        fs.close();

    }


}