package deviation.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;

import deviation.FileInfo;
import deviation.UploaderPreferences;
import deviation.filesystem.TxInterface;
import deviation.gui.treemodel.FileSystemModel2;
import deviation.gui.treemodel.FilesToSendModel;

public class FileMgrTab extends JPanel {
	private static final long serialVersionUID = 1L;
    private static final String LIBPATH_DIR = "LibPathDir";
    private static final String LIBBTN_STR  = "From Lib";
    private static final String DIRBTN_STR  = "From Dir";

	private DeviationUploadGUI gui;
	private JTextField libpathTxt;
	private JButton libpathBtn;
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
        gbl_FMPanel.columnWidths = new int[]{0, 45, 0, 0, 0};
        gbl_FMPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
        gbl_FMPanel.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
        gbl_FMPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
       FMPanel.setLayout(gbl_FMPanel);

       JLabel lblFirmware = new JLabel("Lib Path");
       GridBagConstraints gbc_lblFirmware = new GridBagConstraints();
       gbc_lblFirmware.anchor = GridBagConstraints.EAST;
       gbc_lblFirmware.insets = new Insets(0, 0, 5, 5);
       gbc_lblFirmware.gridx = 1;
       gbc_lblFirmware.gridy = 0;
       add(lblFirmware, gbc_lblFirmware);
       
       UploaderPreferences prefs = new UploaderPreferences();
       String startDir = prefs.get(LIBPATH_DIR, System.getProperty("user.home"));
       libpathTxt = new JTextField();
       libpathTxt.setText(startDir);
       GridBagConstraints gbc_textField = new GridBagConstraints();
       gbc_textField.gridwidth = 2;
       gbc_textField.insets = new Insets(0, 0, 5, 5);
       gbc_textField.fill = GridBagConstraints.HORIZONTAL;
       gbc_textField.gridx = 2;
       gbc_textField.gridy = 0;
       add(libpathTxt, gbc_textField);
       libpathTxt.setColumns(10);
       
       libpathBtn = new JButton("...");
       libpathBtn.addActionListener(new FileChooserBtnListener(libpathTxt));
       GridBagConstraints gbc_button = new GridBagConstraints();
       gbc_button.insets = new Insets(0, 0, 5, 0);
       gbc_button.gridx = 4;
       gbc_button.gridy = 0;
       add(libpathBtn, gbc_button);
        
     //Create the radio buttons.
       RadioBtnListener radioListener = new RadioBtnListener();
       JRadioButton libButton = new JRadioButton(LIBBTN_STR);
       libButton.setActionCommand(LIBBTN_STR);
       libButton.addActionListener(radioListener);
       libButton.setSelected(true);
       JRadioButton dirButton = new JRadioButton(DIRBTN_STR);
       dirButton.setActionCommand(DIRBTN_STR);
       dirButton.addActionListener(radioListener);
       ButtonGroup group = new ButtonGroup();
       group.add(libButton);
       group.add(dirButton);
       
       GridBagConstraints gbc_radioBtn1 = new GridBagConstraints();
       gbc_radioBtn1.gridwidth = 1;
       gbc_radioBtn1.insets = new Insets(0, 0, 5, 5);
       gbc_radioBtn1.fill = GridBagConstraints.HORIZONTAL;
       gbc_radioBtn1.gridx = 2;
       gbc_radioBtn1.gridy = 1;
       add(libButton, gbc_radioBtn1);

       GridBagConstraints gbc_radioBtn2 = new GridBagConstraints();
       gbc_radioBtn2.gridwidth = 1;
       gbc_radioBtn2.insets = new Insets(0, 0, 5, 5);
       gbc_radioBtn2.fill = GridBagConstraints.HORIZONTAL;
       gbc_radioBtn2.gridx = 2;
       gbc_radioBtn1.gridy = 2;
       add(dirButton, gbc_radioBtn2);

       txModel = new FilesToSendModel(null);
        //create the tree by passing in the root node
        txTree = new JXTreeTable(txModel);
        txTree.setDragEnabled(true);
        txTree.setDropMode(DropMode.ON_OR_INSERT);
        txTree.setTransferHandler(new TreeTransferHandler());
        JScrollPane scrollpane = new JScrollPane(txTree);
        GridBagConstraints gbc_FM_txTree = new GridBagConstraints();
        gbc_FM_txTree.insets = new Insets(0, 0, 5, 5);
        gbc_FM_txTree.anchor = GridBagConstraints.WEST;
        gbc_FM_txTree.gridx = 0;
        gbc_FM_txTree.gridy = 1;
        gbc_FM_txTree.fill = GridBagConstraints.BOTH;
        gbc_FM_txTree.gridheight = 7;
        gbc_FM_txTree.weightx = 1;
        FMPanel.add(scrollpane, gbc_FM_txTree);

        f2sModel = new FilesToSendModel(gui.getInstallTab().getFilesToSend());
        dirModel = new FileSystemModel2(startDir);
        //create the tree by passing in the root node
        pcTree = new JXTreeTable(dirModel);
        pcTree.setDragEnabled(true);
        pcTree.setDropMode(DropMode.ON_OR_INSERT);
        pcTree.setTransferHandler(new TreeTransferHandler());
        scrollpane = new JScrollPane(pcTree);
        GridBagConstraints gbc_FM_pcTree = new GridBagConstraints();
        gbc_FM_pcTree.insets = new Insets(0, 0, 5, 5);
        gbc_FM_pcTree.anchor = GridBagConstraints.WEST;
        gbc_FM_pcTree.gridx = 4;
        gbc_FM_pcTree.gridy = 1;
        gbc_FM_pcTree.gridheight = 7;
        gbc_FM_pcTree.fill = GridBagConstraints.BOTH;
        gbc_FM_pcTree.weightx = 1;
        FMPanel.add(scrollpane, gbc_FM_pcTree);
	}
	private List <FileInfo> getFilesFromTx() {
		TxInterface fs =gui.getTxInterface();
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
		f2sModel.refresh();
	}
	public void updateTx() {
		if (gui.getTxInterface() == null) {
			txModel.update(null);
		} else {
			//txModel.update(getFilesFromTx());
		}
	}
	private void changeRadioState(String selected) {
		if (selected.equals(LIBBTN_STR)) {
			libpathTxt.setEnabled(false);
			libpathBtn.setEnabled(false);
			pcTree.setTreeTableModel(f2sModel);
		} else {
			libpathTxt.setEnabled(true);
			libpathBtn.setEnabled(true);
			pcTree.setTreeTableModel(dirModel);
		}		
	}
	
	private class RadioBtnListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			changeRadioState(e.getActionCommand());
		}
	}
    private class FileChooserBtnListener implements ActionListener {
        JTextField txtField;
        public FileChooserBtnListener(JTextField txt) {
            super();
            txtField = txt;
        }
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = new JFileChooser();
            UploaderPreferences prefs = new UploaderPreferences();
            String startDir = txtField.getText();
            fc.setCurrentDirectory(new File(startDir));
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            //
            // disable the "All files" option.
            //
            fc.setAcceptAllFileFilterUsed(false);
            //    
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) { 
            	startDir = fc.getSelectedFile().getPath();
            	if (startDir != null) {
            		prefs.put(LIBPATH_DIR,  startDir);
            		txtField.setText(startDir);
            		((FileSystemModel2)pcTree.getTreeTableModel()).refresh(startDir);
            	}
              System.out.println("getSelectedFile() : " 
                 +  startDir);
             }
        }
    }
    class TreeTransferHandler extends TransferHandler {
        DataFlavor nodesFlavor;
        DataFlavor[] flavors = new DataFlavor[1];
        DefaultMutableTreeNode[] nodesToRemove;

        public TreeTransferHandler() {
            try {
                String mimeType = DataFlavor.javaJVMLocalObjectMimeType +
                                  ";class=\"" +
                    javax.swing.tree.DefaultMutableTreeNode[].class.getName() +
                                  "\"";
                nodesFlavor = new DataFlavor(mimeType);
                flavors[0] = nodesFlavor;
            } catch(ClassNotFoundException e) {
                System.out.println("ClassNotFound: " + e.getMessage());
            }
        }

        public boolean canImport(TransferHandler.TransferSupport support) {
            if(!support.isDrop()) {
                return false;
            }
            support.setShowDropLocation(true);
            if(!support.isDataFlavorSupported(nodesFlavor)) {
                return false;
            }
            // Do not allow a drop on the drag source selections.
            JTree.DropLocation dl =
                    (JTree.DropLocation)support.getDropLocation();
            JTree tree = (JTree)support.getComponent();
            int dropRow = tree.getRowForPath(dl.getPath());
            int[] selRows = tree.getSelectionRows();
            for(int i = 0; i < selRows.length; i++) {
                if(selRows[i] == dropRow) {
                    return false;
                }
            }
            // Do not allow MOVE-action drops if a non-leaf node is
            // selected unless all of its children are also selected.
            int action = support.getDropAction();
            if(action == MOVE) {
                return haveCompleteNode(tree);
            }
            // Do not allow a non-leaf node to be copied to a level
            // which is less than its source level.
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode target =
                (DefaultMutableTreeNode)dest.getLastPathComponent();
            TreePath path = tree.getPathForRow(selRows[0]);
            DefaultMutableTreeNode firstNode =
                (DefaultMutableTreeNode)path.getLastPathComponent();
            if(firstNode.getChildCount() > 0 &&
                   target.getLevel() < firstNode.getLevel()) {
                return false;
            }
            return true;
        }

        private boolean haveCompleteNode(JTree tree) {
            int[] selRows = tree.getSelectionRows();
            TreePath path = tree.getPathForRow(selRows[0]);
            DefaultMutableTreeNode first =
                (DefaultMutableTreeNode)path.getLastPathComponent();
            int childCount = first.getChildCount();
            // first has children and no children are selected.
            if(childCount > 0 && selRows.length == 1)
                return false;
            // first may have children.
            for(int i = 1; i < selRows.length; i++) {
                path = tree.getPathForRow(selRows[i]);
                DefaultMutableTreeNode next =
                    (DefaultMutableTreeNode)path.getLastPathComponent();
                if(first.isNodeChild(next)) {
                    // Found a child of first.
                    if(childCount > selRows.length-1) {
                        // Not all children of first are selected.
                        return false;
                    }
                }
            }
            return true;
        }

        protected Transferable createTransferable(JComponent c) {
            JXTreeTable tree = (JXTreeTable)c;
            List<Object>files = new ArrayList<Object>();
            int[] rows = tree.getSelectedRows();
             if(rows != null) {
                 for (int row: rows) {
                 	TreePath path = tree.getPathForRow(row);
                 	Object node = path.getLastPathComponent();
                 	files.add(node);
                 	//System.out.println(node.getClass().getName() + " " + node);
                 }
                 //return new NodesTransferable(files);
            	System.out.println(Arrays.toString(rows));
            	return null;
                // Make up a node array of copies for transfer and
                // another for/of the nodes that will be removed in
                // exportDone after a successful drop.
/*
                List<DefaultMutableTreeNode> copies =
                    new ArrayList<DefaultMutableTreeNode>();
                List<DefaultMutableTreeNode> toRemove =
                    new ArrayList<DefaultMutableTreeNode>();
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)paths[0].getLastPathComponent();
                DefaultMutableTreeNode copy = copy(node);
                copies.add(copy);
                toRemove.add(node);
                for(int i = 1; i < paths.length; i++) {
                    DefaultMutableTreeNode next =
                        (DefaultMutableTreeNode)paths[i].getLastPathComponent();
                    // Do not allow higher level nodes to be added to list.
                    if(next.getLevel() < node.getLevel()) {
                        break;
                    } else if(next.getLevel() > node.getLevel()) {  // child node
                        copy.add(copy(next));
                        // node already contains child
                    } else {                                        // sibling
                        copies.add(copy(next));
                        toRemove.add(next);
                    }
                    */
                //}
                //DefaultMutableTreeNode[] nodes =
                //    copies.toArray(new DefaultMutableTreeNode[copies.size()]);
                //nodesToRemove =
                //    toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
                //return new NodesTransferable(nodes);
            }
            return null;
        }

        /** Defensive copy used in createTransferable. */
        private DefaultMutableTreeNode copy(TreeNode node) {
            return new DefaultMutableTreeNode(node);
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
            // Extract transfer data.
            DefaultMutableTreeNode[] nodes = null;
            try {
                Transferable t = support.getTransferable();
                nodes = (DefaultMutableTreeNode[])t.getTransferData(nodesFlavor);
            } catch(UnsupportedFlavorException ufe) {
                System.out.println("UnsupportedFlavor: " + ufe.getMessage());
            } catch(java.io.IOException ioe) {
                System.out.println("I/O error: " + ioe.getMessage());
            }
            // Get drop location info.
            JTree.DropLocation dl =
                    (JTree.DropLocation)support.getDropLocation();
            int childIndex = dl.getChildIndex();
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode parent =
                (DefaultMutableTreeNode)dest.getLastPathComponent();
            JTree tree = (JTree)support.getComponent();
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            // Configure for drop mode.
            int index = childIndex;    // DropMode.INSERT
            if(childIndex == -1) {     // DropMode.ON
                index = parent.getChildCount();
            }
            // Add data to model.
            for(int i = 0; i < nodes.length; i++) {
                model.insertNodeInto(nodes[i], parent, index++);
            }
            return true;
        }

        public String toString() {
            return getClass().getName();
        }

        public class NodesTransferable implements Transferable {
            DefaultMutableTreeNode[] nodes;

            public NodesTransferable(DefaultMutableTreeNode[] nodes) {
                this.nodes = nodes;
             }

            public Object getTransferData(DataFlavor flavor)
                                     throws UnsupportedFlavorException {
                if(!isDataFlavorSupported(flavor))
                    throw new UnsupportedFlavorException(flavor);
                return nodes;
            }

            public DataFlavor[] getTransferDataFlavors() {
                return flavors;
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return nodesFlavor.equals(flavor);
            }
        }
    }

}
