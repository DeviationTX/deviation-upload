package deviation.gui.treemodel;

import java.util.Iterator;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;

public class FSModel extends AbstractTreeTableModel {
	private final static String[] COLUMN_NAMES = {"Name", "Size"};
	final FileSystem fs;
	public FSModel(FileSystem fs) {
		super(new Object());
		this.fs = fs;
	}
	@Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }
    
    @Override
    public boolean isCellEditable(Object node, int column) {
        return false;
    }
    @Override
    public boolean isLeaf(Object node) {
    	return (node instanceof FsDirectoryEntry && ((FsDirectoryEntry)node).isFile());
    }
    @Override
    public int getChildCount(Object parent) {
    	FsDirectory dir;
    	int count = 0;
    	try {
    		if (parent instanceof FsDirectoryEntry) {
    			FsDirectoryEntry entry = (FsDirectoryEntry)parent;
    			if (! entry.isDirectory())
    				return 0;
    			dir = entry.getDirectory();
    		} else {
    			dir = fs.getRoot();
    		}
    		Iterator<FsDirectoryEntry> itr = dir.iterator();
    		while(itr.hasNext()) {
    			itr.next();
    			count++;
    		}
    	} catch(Exception e) {e.printStackTrace();}
    	return count;
    }

    @Override
    public Object getChild(Object parent, int index) {
    	FsDirectory dir;
    	int count = 0;
    	try {
    		if (parent instanceof FsDirectoryEntry) {
    			FsDirectoryEntry entry = (FsDirectoryEntry)parent;
    			if (! entry.isDirectory())
    				return null;
    			dir = entry.getDirectory();
    		} else {
    			dir = fs.getRoot();
    		}
    		Iterator<FsDirectoryEntry> itr = dir.iterator();
    		while(itr.hasNext()) {
    			FsDirectoryEntry entry = itr.next();
    			if (count == index)
    				return entry;
    			count++;
    		}
    	} catch(Exception e) {e.printStackTrace();}
    	return null;
    }
    @Override
    public int getIndexOfChild(Object parent, Object child) {
    	FsDirectory dir;
    	int count = 0;
    	try {
    		if (parent instanceof FsDirectoryEntry) {
    			FsDirectoryEntry entry = (FsDirectoryEntry)parent;
    			if (! entry.isDirectory())
    				return 0;
    			dir = entry.getDirectory();
    		} else {
    			dir = fs.getRoot();
    		}
    		Iterator<FsDirectoryEntry> itr = dir.iterator();
    		while(itr.hasNext()) {
    			FsDirectoryEntry entry = itr.next();
    			if (child == entry)
    				return count;
    			count++;
    		}
    	} catch(Exception e) {e.printStackTrace();}
    	return 0;
    }
    @Override
    public Object getValueAt(Object node, int column) {
    	FsDirectoryEntry entry = (FsDirectoryEntry)node;
    	if (column == 0) {
    		return entry.getName();
    	}
    	if (column == 1) {
    		int size = 0;
			try {
				if (entry.isFile()) {
					size = (int)entry.getFile().getLength();
				}
			} catch(Exception e) {e.printStackTrace(); }
    		return size;
    	}
    	return "";
    }
}
