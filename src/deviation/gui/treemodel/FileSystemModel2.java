package deviation.gui.treemodel;

import java.io.File;

import org.jdesktop.swingx.treetable.FileSystemModel;


public class FileSystemModel2 extends FileSystemModel{
	public FileSystemModel2 (String path) {
		super(new File(path));
	}
	public void refresh(String path) {
		setRoot(new File(path));
	}
	@Override
    public int getColumnCount() {
        return 2;
    }
}
