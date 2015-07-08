package deviation.gui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import deviation.DeviationUploader;
import deviation.FileInfo;
import deviation.UploaderPreferences;
import deviation.filesystem.TxInterface;
import deviation.gui.treemodel.FileSystemModel2;
import deviation.gui.treemodel.FilesToSendModel;

public class FileMgrTab extends JPanel {
	private static final long serialVersionUID = 1L;
    private static final String LIBPATH_DIR = "LibPathDir";
    private static final String SHOWSYNC = "ShowSyncDlg";
    private static final String LIBBTN_STR  = "From Lib";
    private static final String DIRBTN_STR  = "From Dir";
    private static enum FileBtn {DELETE, COPY};

	private DeviationUploadGUI gui;
	private JTextField libpathTxt;
	private JButton libpathBtn;
	private JButton deleteBtn;
	private JButton copyBtn;
	private JButton syncBtn;
	private JXTreeTable txTree;
	private JXTreeTable pcTree;
	private FilesToSendModel txModel;
	private FilesToSendModel f2sModel;
	private FileSystemModel2 dirModel;
	public FileMgrTab(DeviationUploadGUI gui)
	{
		this.gui = gui;
		
		JPanel FMPanel = this;
        GridBagLayout gbl_FMPanel = new GridBagLayout();
        gbl_FMPanel.columnWidths = new int[]{0, 0, 0, 0};
        gbl_FMPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
        gbl_FMPanel.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
        gbl_FMPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
       FMPanel.setLayout(gbl_FMPanel);

       
       URL imageStr = DeviationUploader.class.getResource("/trashcan.png");
       ImageIcon trashIcon = new ImageIcon(imageStr);
       deleteBtn = new JButton(new ImageIcon(trashIcon.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH)));
       deleteBtn.addActionListener(new FileTreeButtonListener(FileBtn.DELETE));
       GridBagConstraints gbc_Trash = new GridBagConstraints();
       gbc_Trash.insets = new Insets(0, 0, 5, 5);
       gbc_Trash.anchor = GridBagConstraints.WEST;
       gbc_Trash.gridx = 0;
       gbc_Trash.gridy = 0;
       gbc_Trash.fill = GridBagConstraints.BOTH;
       gbc_Trash.gridheight = 1;
       gbc_Trash.weightx = 0;
       FMPanel.add(deleteBtn, gbc_Trash);

       imageStr = DeviationUploader.class.getResource("/copy.png");
       ImageIcon copyIcon = new ImageIcon(imageStr);
       copyBtn = new JButton(new ImageIcon(copyIcon.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH)));
       copyBtn.addActionListener(new FileTreeButtonListener(FileBtn.COPY));
       GridBagConstraints gbc_Copy = new GridBagConstraints();
       gbc_Copy.insets = new Insets(0, 0, 5, 5);
       gbc_Copy.anchor = GridBagConstraints.WEST;
       gbc_Copy.gridx = 1;
       gbc_Copy.gridy = 0;
       gbc_Copy.fill = GridBagConstraints.BOTH;
       gbc_Copy.gridheight = 1;
       gbc_Copy.weightx = 0;
       FMPanel.add(copyBtn, gbc_Copy);

       imageStr = DeviationUploader.class.getResource("/sync.png");
       ImageIcon syncIcon = new ImageIcon(imageStr);
       syncBtn = new JButton(new ImageIcon(syncIcon.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH)));
       syncBtn.addActionListener(new SyncButtonListener());
       syncBtn.setEnabled(false);
       GridBagConstraints gbc_Sync = new GridBagConstraints();
       gbc_Sync.insets = new Insets(0, 0, 5, 5);
       gbc_Sync.anchor = GridBagConstraints.EAST;
       gbc_Sync.gridx = 3;
       gbc_Sync.gridy = 0;
       gbc_Sync.fill = GridBagConstraints.BOTH;
       gbc_Sync.gridheight = 1;
       gbc_Sync.weightx = 0;
       FMPanel.add(syncBtn, gbc_Sync);

       txModel = new FilesToSendModel(null);
        //create the tree by passing in the root node
        txTree = new JXTreeTable(txModel);
        FileMgrDragDrop.setup(gui, txTree, FileMgrDragDrop.DROP);
        txTree.addMouseListener(new TreeMouseListener());
        JScrollPane scrollpane = new JScrollPane(txTree);
        GridBagConstraints gbc_FM_txTree = new GridBagConstraints();
        gbc_FM_txTree.insets = new Insets(0, 0, 5, 5);
        gbc_FM_txTree.anchor = GridBagConstraints.WEST;
        gbc_FM_txTree.gridx = 0;
        gbc_FM_txTree.gridy = 1;
        gbc_FM_txTree.fill = GridBagConstraints.BOTH;
        gbc_FM_txTree.gridheight = 7;
        gbc_FM_txTree.gridwidth = 4;
        gbc_FM_txTree.weightx = 1;
        FMPanel.add(scrollpane, gbc_FM_txTree);
        
	}
	private List <FileInfo> getFilesFromTx() {
		TxInterface fs =gui.getTxInterface();
		if (fs == null) {
			return new ArrayList<FileInfo>();
		}
		fs.open();
		try {
			if (gui.getFSStatus().isFormatted()) {
				fs.Init(gui.getFSStatus());
			} else {
				fs.Init(gui.getFSStatus());
			}
		} catch (Exception e) { e.printStackTrace(); }
		List <FileInfo> files = fs.readAllDirs();
		fs.close();
		System.out.format("Count: %d\n", files.size());
		for (FileInfo file: files) {
			System.out.println("FILE: " + file.name());
		}
		return files;
	}
	public void updateFileList() {
		//getFilesFromTx();
	}
	public void updateTx() {
		if (gui.getTxInterface() == null) {
			txModel.update(null);
		} else {
			txModel.update(getFilesFromTx());
		}
	}
	private class FileTreeButtonListener implements ActionListener {
		private FileBtn buttonType;
		public FileTreeButtonListener(FileBtn buttonType) {
            super();
			this.buttonType = buttonType;
		}
		private void addChildren(List<FileInfo>files, Object parent) {
			int count = txModel.getChildCount(parent);
			for (int i = 0; i < count; i++) {
        		Object obj = txModel.getChild(parent,  i);
        		if (obj instanceof FileInfo) {
        			files.add((FileInfo)obj);
        		} else {
        			addChildren(files, obj);
        		}
				
			}
			
		}
        public void actionPerformed(ActionEvent e) {
        	List<FileInfo> files = new ArrayList<FileInfo>();
        	int selected[] = txTree.getSelectedRows();
        	for (int row: selected) {
        		Object obj = txTree.getModel().getValueAt(row,  -1);
        		if (obj instanceof FileInfo) {
        			files.add((FileInfo)obj);        			
        		} else {
        			addChildren(files, obj);
        		}
        	}
        	if (files.size() == 0) {
        		return;
        	}
        	if (buttonType == FileBtn.COPY) {
        		UploaderPreferences prefs = new UploaderPreferences();
    			String copyDir = prefs.get(LIBPATH_DIR);
                final JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(copyDir));
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                // disable the "All files" option.
                fc.setAcceptAllFileFilterUsed(false);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                	copyDir = fc.getSelectedFile().getPath();
                	if (copyDir != null) {               		
                		prefs.put(LIBPATH_DIR, copyDir);
                		//Copy files here
                		FileCopyFromTx copy = new FileCopyFromTx(gui, files, copyDir);
                		copy.execute();
                	}

                }
        	} else if (buttonType == FileBtn.DELETE) {
        		//Delete files here
        		List <FileInfo> txFiles = txModel.getFiles();
        		for (FileInfo file: files) {
        			file.setDeleted();
        		}
        		txModel.refresh();
        		LocalFilesChanged();
        	}
        }
	}
	public void LocalFilesChanged() {
		ShowSyncDialog();
		syncBtn.setEnabled(true);
	}
	private void ShowSyncDialog() {
		UploaderPreferences prefs = new UploaderPreferences();
		String showDlg = prefs.get(SHOWSYNC, "1");
		if (showDlg.equals("0")) {
			return;
		}
		JCheckBox checkbox = new JCheckBox("Do not show this message again.");
		String message = "Changes to the transmitter filesystem have not yet been saved.\nYou must press the 'Sync' button to save them";
		Object[] params = {message, checkbox};
		JOptionPane.showMessageDialog(gui.getFrame(), params, "Changes not yet saved", JOptionPane.WARNING_MESSAGE);
		boolean dontShow = checkbox.isSelected();
		if (dontShow) {
			prefs.put(SHOWSYNC, "0");
		}
	}
	private class SyncButtonListener implements ActionListener {
		public SyncButtonListener() {
			super();
		}
		public void actionPerformed(ActionEvent e) {
			FilesToSend files = new FilesToSend();
			for (FileInfo file: txModel.getFiles()) {
				if (file.deleted() || file.data() != null) {
					files.addFile(file);
				}
			}
			FileInstaller installer = new FileInstaller(gui, files);
			installer.execute();
			syncBtn.setEnabled(false);
		}
	}
	public void ShowTextEditorDialog(FileInfo file) {
		TextEditor edit = new TextEditor(file);
		edit.addWindowListener(new WindowAdapter() 
		{
			  public void windowClosed(WindowEvent e)
			  {
				  TextEditor edit = (TextEditor)e.getWindow();
				  if (edit.changed()) {
					  FileInfo fileinfo = edit.getFileInfo();
					  List<FileInfo>files = txModel.getFiles();
					  for(FileInfo f: files) {
						  if (f.name().equals(fileinfo.name())) {
							  f.setData(fileinfo.data());
							  LocalFilesChanged();
						  }
					  }
				  }
			  }
		});
		edit.setVisible(true);
	   }

	private class TreeMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(final MouseEvent e) {
			if (e.getClickCount() != 2) {
				return;
			}

			final int rowIndex = txTree.rowAtPoint(e.getPoint());

			if (rowIndex < 0) {
				return;
			}

			final Object obj = txTree.getPathForRow(rowIndex).getLastPathComponent();
			if (obj instanceof FileInfo) {
				FileInfo fileinfo = (FileInfo)obj;
				File file = null;
				System.out.format("Double click on: %s\n", fileinfo.name());
				fileinfo = new FileInfo(fileinfo);
				TxInterface fs =gui.getTxInterface();
				fs.open();
				try {
					fs.Init(gui.getFSStatus());
					fs.fillFileData(fileinfo);
					if (fileinfo.name().toUpperCase().endsWith(".INI")){
						//Use internal editor for INI files
						ShowTextEditorDialog(fileinfo);
					} else {
						//Use viewer for all other files
						Path tempDir = Files.createTempDirectory("deviation");
						file = new File(tempDir.resolve(fileinfo.baseName()).toUri());
						if (!file.exists()) {
							file.createNewFile();
						}
						FileOutputStream fop = new FileOutputStream(file);
						fop.write(fileinfo.data());
						fop.flush();
						fop.close();
						Desktop.getDesktop().open(file);
					}
				} catch (Exception e1) { e1.printStackTrace(); }
				fs.close();
			}
		}
	}
}
