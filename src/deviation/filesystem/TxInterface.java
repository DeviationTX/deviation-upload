package deviation.filesystem;

import java.io.IOException;
import java.util.List;

import deviation.DfuDevice;
import deviation.FileInfo;
import deviation.Progress;

public interface TxInterface {
    public void setProgress(Progress progress);
    public DfuDevice getDevice();
    public boolean hasSeparateMediaDrive();
    public void Format(FSStatus status) throws IOException;
    public void Init(FSStatus status) throws IOException;
    public List <FileInfo> readAllDirs();
    public void readDir(String dirStr);
    public void copyFile(FileInfo file);
    public void fillFileData(FileInfo file);
    public void open();
    public void close();
    public FSStatus getFSStatus();
}
