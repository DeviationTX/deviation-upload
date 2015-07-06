package deviation.gui.treemodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import deviation.FileInfo;
import deviation.gui.FilesToSend;

public class FilesToSendModel extends AbstractTreeTableModel {
	private final static String[] COLUMN_NAMES = {"Name", "Size"};
	List<FileInfo> files;
	
	public FilesToSendModel(FilesToSend files) {
		super(new Object());
		if (files != null) {
			this.files = files.getFiles();
		} else {
			this.files = new ArrayList<FileInfo>();
		}
		sort();
	}
	public List <FileInfo> getFiles() { return files; }
	public void refresh() {
		System.out.println("Firing Root");
		super.modelSupport.fireNewRoot();
	}
	public void update(List<FileInfo> files) {
		if (files != null) {
			this.files = files;
		} else {
			this.files = new ArrayList<FileInfo>();
		}		
		sort();
		refresh();
	}
	private void sort() {
		Collections.sort(files, new Comparator<FileInfo>() {
			public int compare(FileInfo first, FileInfo second) {
				return first.name().compareTo(second.name());				
			}
		});
	}
	private boolean fileMatchesDir(FileInfo file, String dir) {
		if (dir.equals("")) {
			return true;
		}
		return file.name().startsWith(dir + "/");
	}
	private List<Object> getDirList(String dir) {
		List<Object> entries = new ArrayList<Object>();
		boolean seen = false;
		
		for (int i = 0; i < files.size(); i++) {
			FileInfo file = files.get(i);
			if (file.deleted()) {
				continue;
			}
			if (fileMatchesDir(file, dir)) {
				seen = true;
				String name = file.name();
				int dirend = name.indexOf("/",dir.length()+1);
				if (dirend >= 0) {
					name = name.substring(0, dirend);
					if (! entries.contains(name)) {
						entries.add(name);
						
					}
				} else {
					entries.add(file);
				}			
			} else if (seen) {
				break;
			}			
		}
		return entries;
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
    	return (node instanceof FileInfo);
    }
    @Override
    public int getChildCount(Object parent) {
    	String dir = (parent instanceof String) ? (String)parent : "";
    	List<Object> entries = getDirList(dir);
    	//System.out.format("getChildCount for '%s' returned %d\n", dir, entries.size());
    	return entries.size();
    }

    @Override
    public Object getChild(Object parent, int index) {
    	String dir = (parent instanceof String) ? (String)parent : "";
    	List<Object> entries = getDirList(dir);
    	return entries.get(index);
    }
    @Override
    public int getIndexOfChild(Object parent, Object child) {
    	String dir = (parent instanceof String) ? (String)parent : "";
    	List<Object> entries = getDirList(dir);
    	return entries.indexOf(child);
    }
    @Override
    public Object getValueAt(Object node, int column) {
		if (column == -1) { 
			return node;
		}
    	if (node instanceof FileInfo) {
    		if (column == 0) {
    			String name = ((FileInfo)node).name();
    			if (name.lastIndexOf("/") != -1) {
    				name = name.substring(name.lastIndexOf("/")+1);
    			}
    			return name;
    		} else {
    			return ((FileInfo)node).size();
    		}
    	} else {
    		if (column == 0) {
    			return node;
    		} else {
    			return 0;
    		}
    	}
    }
}
