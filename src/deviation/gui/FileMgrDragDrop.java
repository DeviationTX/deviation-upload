package deviation.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;

import deviation.FileInfo;
import deviation.IOUtil;
import deviation.filesystem.TxInterface;
import deviation.gui.treemodel.FilesToSendModel;

/* This class is currently unused because Java does not seem to provide a hook
 * to get the data on drop as opposed to on start of drag.
 * We really need a deferred fetch because it can take a long time.
 * The code as it is now requires The user to initiate a drag, then wait for
 * the file data to be loaded before continuing the drag/drop.
 * We want an interface like 7-zip where the file is generated only on drop
 * completion
 */
public class FileMgrDragDrop {
	public static final int DRAG = 0x01;
	public static final int DROP = 0x02;
	public static final int DRAGDROP = 0x03;
	public static void setup(DeviationUploadGUI gui, JXTreeTable tree, int mode) {
		if ((mode & DRAG) != 0) {
			tree.setDragEnabled(true);
		}
		if ((mode & DROP) != 0) {
			tree.setDropMode(DropMode.ON_OR_INSERT);
		}
		tree.setTransferHandler(new TreeTransferHandler(gui));
	}

	static class FileTransferable implements Transferable 
	{
		final private List<File> files;
		final private DataFlavor[] flavors;

		/**
		 * A drag-and-drop object for transfering a file.
		 * @param file file to transfer -- this file should already exist,
		 * otherwise it may not be accepted by drag targets.
		 */
		public FileTransferable(Collection<File> files) {
			this.files = Collections.unmodifiableList(
					new ArrayList<File>(files));
			this.flavors = new DataFlavor[] 
					{ DataFlavor.javaFileListFlavor };
		}

		public List<File> getFiles() { return this.files; }

		@Override public Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException 
		{
			if (isDataFlavorSupported(flavor)) {
				return this.files;
			} else {
				return null;
			}
		}

		@Override public DataFlavor[] getTransferDataFlavors() {
			return this.flavors;
		}

		@Override public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.javaFileListFlavor.equals(flavor);
		}
	}
	static class TreeTransferHandler extends TransferHandler {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		DeviationUploadGUI gui;
		DataFlavor[] flavors = new DataFlavor[1];
		DefaultMutableTreeNode[] nodesToRemove;
		Path tempDir;
		public TreeTransferHandler(DeviationUploadGUI gui) {
			this.gui = gui;
			flavors[0] = DataFlavor.javaFileListFlavor;
			try {
				tempDir = Files.createTempDirectory("deviation");
			} catch (Exception e) { e.printStackTrace(); }
		}

		public boolean canImport(TransferHandler.TransferSupport support) {
			if(!support.isDrop()) {
				return false;
			}
			support.setShowDropLocation(true);
			if(!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				return false;
			}
			return true;
		}

		protected Transferable createTransferable(JComponent c) {
			JXTreeTable tree = (JXTreeTable)c;
			List<File>files = new ArrayList<File>();
			int[] rows = tree.getSelectedRows();
			if(rows != null) {            	 
				for (int row: rows) {
					TreePath path = tree.getPathForRow(row);
					Object node = path.getLastPathComponent();
					if (node instanceof deviation.FileInfo) {
						FileInfo fileinfo = (FileInfo)node;
						if (fileinfo.data() == null) {
							TxInterface fs =gui.getTxInterface();
							fs.open();
							try {
								if (gui.getFSStatus().isFormatted()) {
									fs.Init(gui.getFSStatus());
								} else {
									fs.Init(gui.getFSStatus());
								}
							} catch (Exception e) { e.printStackTrace(); }
							fileinfo = new FileInfo(fileinfo);
							fs.fillFileData(fileinfo);
							fs.close();
						}
						File f = new File(tempDir.resolve(fileinfo.name()).toUri());
						try {
							// if file doesnt exists, then create it
							if (!f.exists()) {
								File parent = f.getParentFile();
								if(!parent.exists() && !parent.mkdirs()){
									throw new IllegalStateException("Couldn't create dir: " + parent);
								}
								f.createNewFile();
							}
							FileOutputStream fop = new FileOutputStream(f);
							fop.write(fileinfo.data());
							fop.flush();
							fop.close();

							files.add(f);
						} catch (Exception e) {e.printStackTrace(); }
					}
					//System.out.println(node.getClass().getName() + " " + node);
				}
				return new FileTransferable(files);
			}
			return null;
		}

		protected void exportDone(JComponent source, Transferable data, int action) {
			if((action & MOVE) == MOVE) {
				JTree tree = (JTree)source;
				DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
				// Remove nodes saved in nodesToRemove in createTransferable.
				for(int i = 0; i < nodesToRemove.length; i++) {
					model.removeNodeFromParent(nodesToRemove[i]);
				}
			}
		}

		public int getSourceActions(JComponent c) {
			return COPY_OR_MOVE;
		}

		public boolean importData(TransferHandler.TransferSupport support) {
			if(!canImport(support)) {
				return false;
			}
			JXTreeTable tree = (JXTreeTable)support.getComponent();
			JXTreeTable.DropLocation dropLocation =
					(JXTreeTable.DropLocation)support.getDropLocation();


			Transferable transferable = support.getTransferable();

			List<?> transferData;
			try {
				transferData = (List<?>)transferable.getTransferData(
						DataFlavor.javaFileListFlavor);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} catch (UnsupportedFlavorException e) {
				e.printStackTrace();
				return false;
			}

			int row = dropLocation.getRow();
			boolean isInsert = dropLocation.isInsertRow();
			if (row == -1) {
				return false;
			}
			TreePath path = tree.getPathForRow(row);
			Object node = path.getLastPathComponent();
			String parentDir;
			if (node instanceof deviation.FileInfo) {
				FileInfo file = (FileInfo)node;
				parentDir = file.parentDir();
			} else {
				if (isInsert) {
					String[]filedir = ((String)node).split("/");
					if (filedir.length > 1) {
						filedir = Arrays.copyOfRange(filedir, 0, filedir.length-1);
					} else {
						filedir = new String[0];
					}
					parentDir = String.join("/", filedir);
				} else {
					parentDir = (String)node;
				}
			}
			FilesToSendModel txModel = (FilesToSendModel)tree.getTreeTableModel();
			List<FileInfo> files = txModel.getFiles();
			for (Object obj: transferData) {
				File f = (File)obj;
				
		        byte[] data;
		        try {
		            data = IOUtil.readFile(f.getPath());
		            String fname = parentDir.equals("") ? f.getName() : parentDir + "/" + f.getName();
		        	files.add(new FileInfo(fname.toUpperCase(), data));
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
			}
			txModel.update(files);
			gui.getFileMgrTab().LocalFilesChanged();
			return true;
		}
		public String toString() {
			return getClass().getName();
		}
	}
}
