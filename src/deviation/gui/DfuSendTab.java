package deviation.gui;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import deviation.*;

public class DfuSendTab extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_DIR = "DefaultDir";
    private JTextField DFU_txtFile;
    private JTextField DFU_txtStartAddress;
    private JTextField DFU_txtEndAddress;
    private JTextField DFU_txtSize;
    private JTextField DFU_txtUsed;
    private JTabbedPane DFU_InfoTabbedPane;
    private JButton DFU_btnSend;
    private final DeviationUploadGUI gui;
    private final FilesToSend fileList;

    private DfuFile dfuFile;

    public DfuSendTab(DeviationUploadGUI gui) {
    	this.gui = gui;
        dfuFile = null;
        fileList = new FilesToSend();
        
        JPanel DFUPanel = this;
        GridBagLayout gbl_DFUPanel = new GridBagLayout();
        gbl_DFUPanel.columnWidths = new int[]{0, 0, 0, 0};
        gbl_DFUPanel.rowHeights = new int[]{0, 0, 0, 0};
        gbl_DFUPanel.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
        gbl_DFUPanel.rowWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
        DFUPanel.setLayout(gbl_DFUPanel);

        JLabel DFU_lblFile = new JLabel("File");
        GridBagConstraints gbc_DFU_lblFile = new GridBagConstraints();
        gbc_DFU_lblFile.insets = new Insets(0, 0, 5, 5);
        gbc_DFU_lblFile.anchor = GridBagConstraints.WEST;
        gbc_DFU_lblFile.gridx = 0;
        gbc_DFU_lblFile.gridy = 0;
        DFUPanel.add(DFU_lblFile, gbc_DFU_lblFile);

        DFU_txtFile = new JTextField();
        GridBagConstraints gbc_DFU_txtFile = new GridBagConstraints();
        gbc_DFU_txtFile.insets = new Insets(0, 0, 5, 5);
        gbc_DFU_txtFile.fill = GridBagConstraints.HORIZONTAL;
        gbc_DFU_txtFile.gridx = 1;
        gbc_DFU_txtFile.gridy = 0;
        DFUPanel.add(DFU_txtFile, gbc_DFU_txtFile);
        DFU_txtFile.setColumns(10);

        JButton DFU_btnFile = new JButton("...");
        DFU_btnFile.addActionListener(new FileChooserBtnListener());
        //DFU_btnFile.setAction(action);
        GridBagConstraints gbc_DFU_btnFile = new GridBagConstraints();
        gbc_DFU_btnFile.insets = new Insets(0, 0, 5, 0);
        gbc_DFU_btnFile.gridx = 2;
        gbc_DFU_btnFile.gridy = 0;
        DFUPanel.add(DFU_btnFile, gbc_DFU_btnFile);

        DFU_InfoTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        GridBagConstraints gbc_DFUInfoPanel = new GridBagConstraints();
        gbc_DFUInfoPanel.insets = new Insets(0, 0, 5, 0);
        gbc_DFUInfoPanel.gridwidth = 3;
        gbc_DFUInfoPanel.fill = GridBagConstraints.BOTH;
        gbc_DFUInfoPanel.gridx = 0;
        gbc_DFUInfoPanel.gridy = 1;
        DFUPanel.add(DFU_InfoTabbedPane, gbc_DFUInfoPanel);

        DFU_btnSend = new JButton(new InstallButtonHandler(gui, fileList, "Send", "Install DFU onto transmitter", "Cancel DFU installation"));
        DFU_btnSend.setText("Send");
        GridBagConstraints gbc_DFU_btnSend = new GridBagConstraints();
        gbc_DFU_btnSend.gridwidth = 3;
        gbc_DFU_btnSend.insets = new Insets(0, 0, 0, 5);
        gbc_DFU_btnSend.gridx = 0;
        gbc_DFU_btnSend.gridy = 2;
        DFUPanel.add(DFU_btnSend, gbc_DFU_btnSend);

    }
    public void refresh() {}
    private class FileChooserBtnListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = new JFileChooser();
            UploaderPreferences prefs = new UploaderPreferences();
            String startDir = prefs.get(DEFAULT_DIR, System.getProperty("user.home"));
            fc.setCurrentDirectory(new File(startDir));
            FileNameExtensionFilter ff = new FileNameExtensionFilter("DFU Files", "dfu");
            fc.addChoosableFileFilter(ff);
            fc.setFileFilter(ff);
            int returnVal = fc.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
            	startDir = fc.getSelectedFile().getParent();
            	if (startDir != null) {
            		prefs.put(DEFAULT_DIR,  startDir);
            	}
                String fname = fc.getSelectedFile().getPath();
                try {
                    boolean first = true;
                    dfuFile = new DfuFile(fname);
                    Transmitter type = TransmitterList.UNKNOWN();
                    int size = 0;
                    for (DfuFile.ImageElement elem : dfuFile.imageElements()) {
                    	size += elem.data().length;
                        DFU_InfoTabbedPane.addTab(elem.name(), new DfuInfoPanel(elem));
                        if (first) {
                            type = TxInfo.getModelFromString(elem.name());
                            first = false;
                        } else if(type != TxInfo.getModelFromString(elem.name())) {
                            throw new IOException("Found multiple Tx types in dfu");
                        }
                    }
                    DFU_txtFile.setText(fname);
                    fileList.clearFiles();
                    fileList.setLibraryDfus(null);
                	fileList.setFirmwareDfu(null);
                    if(gui.getTxInfo().matchModel(type)) {
                        DFU_btnSend.setEnabled(true);
                        fileList.setFirmwareDfu(dfuFile);
                        fileList.setTotalBytes(size);
                    } else{
                        DFU_btnSend.setEnabled(false);
                        System.out.format("Error: Dfu Tx type '%s' does not match transmitter type '%s'%n",
                                TxInfo.typeToString(type),
                                TxInfo.typeToString(gui.getTxInfo().type()));
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            }
        }
    }
    private class DfuInfoPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        DfuInfoPanel(DfuFile.ImageElement elem) {
            long data_len = elem.data().length;
            GridBagLayout gbl_DFUInfoPanel = new GridBagLayout();
            gbl_DFUInfoPanel.columnWidths = new int[]{0, 0, 0, 0};
            gbl_DFUInfoPanel.rowHeights = new int[]{0, 0, 0, 0, 0};
            gbl_DFUInfoPanel.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
            gbl_DFUInfoPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
            this.setLayout(gbl_DFUInfoPanel);

            JLabel DFU_lblStartAddress = new JLabel("Start Address");
            GridBagConstraints gbc_DFU_lblStartAddress = new GridBagConstraints();
            gbc_DFU_lblStartAddress.anchor = GridBagConstraints.EAST;
            gbc_DFU_lblStartAddress.insets = new Insets(0, 0, 5, 5);
            gbc_DFU_lblStartAddress.gridx = 0;
            gbc_DFU_lblStartAddress.gridy = 0;
            this.add(DFU_lblStartAddress, gbc_DFU_lblStartAddress);

            DFU_txtStartAddress = new JTextField(String.format("0x%08x",elem.address()));
            DFU_txtStartAddress.setEditable(false);
            GridBagConstraints gbc_DFU_txtStartAddress = new GridBagConstraints();
            gbc_DFU_txtStartAddress.insets = new Insets(0, 0, 5, 5);
            gbc_DFU_txtStartAddress.fill = GridBagConstraints.HORIZONTAL;
            gbc_DFU_txtStartAddress.gridx = 1;
            gbc_DFU_txtStartAddress.gridy = 0;
            this.add(DFU_txtStartAddress, gbc_DFU_txtStartAddress);
            DFU_txtStartAddress.setColumns(10);

            JLabel DFU_lblEndAddress = new JLabel("End Address");
            GridBagConstraints gbc_DFU_lblEndAddress = new GridBagConstraints();
            gbc_DFU_lblEndAddress.anchor = GridBagConstraints.EAST;
            gbc_DFU_lblEndAddress.insets = new Insets(0, 0, 5, 5);
            gbc_DFU_lblEndAddress.gridx = 0;
            gbc_DFU_lblEndAddress.gridy = 1;
            this.add(DFU_lblEndAddress, gbc_DFU_lblEndAddress);

            DFU_txtEndAddress = new JTextField(String.format("0x%08x",elem.address() + data_len));
            DFU_txtEndAddress.setEditable(false);
            GridBagConstraints gbc_DFU_txtEndAddress = new GridBagConstraints();
            gbc_DFU_txtEndAddress.insets = new Insets(0, 0, 5, 5);
            gbc_DFU_txtEndAddress.fill = GridBagConstraints.HORIZONTAL;
            gbc_DFU_txtEndAddress.gridx = 1;
            gbc_DFU_txtEndAddress.gridy = 1;
            this.add(DFU_txtEndAddress, gbc_DFU_txtEndAddress);
            DFU_txtEndAddress.setColumns(10);

            JLabel DFU_lblSize = new JLabel("Size");
            GridBagConstraints gbc_DFU_lblSize = new GridBagConstraints();
            gbc_DFU_lblSize.anchor = GridBagConstraints.EAST;
            gbc_DFU_lblSize.insets = new Insets(0, 0, 5, 5);
            gbc_DFU_lblSize.gridx = 0;
            gbc_DFU_lblSize.gridy = 2;
            this.add(DFU_lblSize, gbc_DFU_lblSize);

            DFU_txtSize = new JTextField(String.valueOf(data_len/1024) + " kb");
            DFU_txtSize.setEditable(false);
            GridBagConstraints gbc_DFU_txtSize = new GridBagConstraints();
            gbc_DFU_txtSize.insets = new Insets(0, 0, 5, 5);
            gbc_DFU_txtSize.fill = GridBagConstraints.HORIZONTAL;
            gbc_DFU_txtSize.gridx = 1;
            gbc_DFU_txtSize.gridy = 2;
            this.add(DFU_txtSize, gbc_DFU_txtSize);
            DFU_txtSize.setColumns(10);

            JLabel DFU_lblUsed = new JLabel("% Used");
            GridBagConstraints gbc_DFU_lblUsed = new GridBagConstraints();
            gbc_DFU_lblUsed.insets = new Insets(0, 0, 0, 5);
            gbc_DFU_lblUsed.anchor = GridBagConstraints.EAST;
            gbc_DFU_lblUsed.gridx = 0;
            gbc_DFU_lblUsed.gridy = 3;
            this.add(DFU_lblUsed, gbc_DFU_lblUsed);

            DFU_txtUsed = new JTextField();
            DFU_txtUsed.setEditable(false);
            GridBagConstraints gbc_DFU_txtUsed = new GridBagConstraints();
            gbc_DFU_txtUsed.insets = new Insets(0, 0, 0, 5);
            gbc_DFU_txtUsed.fill = GridBagConstraints.HORIZONTAL;
            gbc_DFU_txtUsed.gridx = 1;
            gbc_DFU_txtUsed.gridy = 3;
            this.add(DFU_txtUsed, gbc_DFU_txtUsed);
            DFU_txtUsed.setColumns(10);
        }
    }

}
