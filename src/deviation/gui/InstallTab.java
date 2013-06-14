package deviation.gui;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import deviation.*;
import deviation.DevoDetect.Firmware;
import deviation.DevoFat.FatStatus;
import deviation.TxInfo.TxModel;

public class InstallTab extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private JTextField firmwareTxt;
    private JTextField libraryTxt;
    private JTextField txtFwVersion;
    private JTextField txtFwSize;
    private JTextField txtFwUsed;
    private JTextField txtLibVersion;
    private JTextField txtLibSize;
    private JTextField txtLibUsed;
    private JCheckBox chckbxFormatRoot;
    private JCheckBox chckbxFormatMedia;
    private JCheckBox chckbxReplaceTxini;
    private JCheckBox chckbxReplaceModels;
    
    private JButton btnInstall;

    private ZipFileGroup zipFiles;
    DeviationUploadGUI gui;
    DfuCmdWorker worker;
    
    DevoDetect fw;
    DevoDetect lib;
    public InstallTab(DeviationUploadGUI gui) {
        this.gui = gui;
        zipFiles = new ZipFileGroup();
        fw = new DevoDetect();
        lib = new DevoDetect();
        
        GridBagLayout gbl_BinSendPanel = new GridBagLayout();
        gbl_BinSendPanel.columnWidths = new int[]{0, 45, 0, 0, 0};
        gbl_BinSendPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
        gbl_BinSendPanel.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
        gbl_BinSendPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        setLayout(gbl_BinSendPanel);
        
        JLabel lblFirmware = new JLabel("Firmware");
        GridBagConstraints gbc_lblFirmware = new GridBagConstraints();
        gbc_lblFirmware.anchor = GridBagConstraints.EAST;
        gbc_lblFirmware.insets = new Insets(0, 0, 5, 5);
        gbc_lblFirmware.gridx = 0;
        gbc_lblFirmware.gridy = 0;
        add(lblFirmware, gbc_lblFirmware);
        
        firmwareTxt = new JTextField();
        GridBagConstraints gbc_textField = new GridBagConstraints();
        gbc_textField.gridwidth = 2;
        gbc_textField.insets = new Insets(0, 0, 5, 5);
        gbc_textField.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField.gridx = 1;
        gbc_textField.gridy = 0;
        add(firmwareTxt, gbc_textField);
        firmwareTxt.setColumns(10);
        
        JButton firmwareBtn = new JButton("...");
        firmwareBtn.addActionListener(new FileChooserBtnListener(firmwareTxt));
        GridBagConstraints gbc_button = new GridBagConstraints();
        gbc_button.insets = new Insets(0, 0, 5, 0);
        gbc_button.gridx = 3;
        gbc_button.gridy = 0;
        add(firmwareBtn, gbc_button);
        
        JLabel lblFilesystem = new JLabel("Filesystem");
        GridBagConstraints gbc_lblFilesystem = new GridBagConstraints();
        gbc_lblFilesystem.anchor = GridBagConstraints.EAST;
        gbc_lblFilesystem.insets = new Insets(0, 0, 5, 5);
        gbc_lblFilesystem.gridx = 0;
        gbc_lblFilesystem.gridy = 1;
        add(lblFilesystem, gbc_lblFilesystem);
        
        libraryTxt = new JTextField();
        GridBagConstraints gbc_textField_1 = new GridBagConstraints();
        gbc_textField_1.gridwidth = 2;
        gbc_textField_1.insets = new Insets(0, 0, 5, 5);
        gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField_1.gridx = 1;
        gbc_textField_1.gridy = 1;
        add(libraryTxt, gbc_textField_1);
        libraryTxt.setColumns(10);
        
        JButton filesystemBtn = new JButton("...");
        filesystemBtn.addActionListener(new FileChooserBtnListener(libraryTxt));
        GridBagConstraints gbc_button_1 = new GridBagConstraints();
        gbc_button_1.insets = new Insets(0, 0, 5, 0);
        gbc_button_1.gridx = 3;
        gbc_button_1.gridy = 1;
        add(filesystemBtn, gbc_button_1);
        
        JPanel panel_1 = new JPanel();
        GridBagConstraints gbc_panel_1 = new GridBagConstraints();
        gbc_panel_1.gridheight = 3;
        gbc_panel_1.gridwidth = 2;
        gbc_panel_1.insets = new Insets(0, 0, 5, 0);
        gbc_panel_1.fill = GridBagConstraints.BOTH;
        gbc_panel_1.gridx = 2;
        gbc_panel_1.gridy = 2;
        add(panel_1, gbc_panel_1);
        
        GridBagLayout gbl_panel_1 = new GridBagLayout();
        gbl_panel_1.columnWidths = new int[]{0, 0, 0, 0, 0};
        gbl_panel_1.rowHeights = new int[]{0, 0, 0, 0, 0};
        gbl_panel_1.columnWeights = new double[]{0.0, 1.0, 1.0, 1.0, Double.MIN_VALUE};
        gbl_panel_1.rowWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
        panel_1.setLayout(gbl_panel_1);
        
        JLabel lblVersion = new JLabel("Version");
        GridBagConstraints gbc_lblVersion = new GridBagConstraints();
        gbc_lblVersion.insets = new Insets(0, 0, 5, 0);
        gbc_lblVersion.gridx = 1;
        gbc_lblVersion.gridy = 0;
        panel_1.add(lblVersion, gbc_lblVersion);

        JLabel lblSize = new JLabel("Size");
        GridBagConstraints gbc_lblSize = new GridBagConstraints();
        gbc_lblSize.insets = new Insets(0, 0, 5, 5);
        gbc_lblSize.gridx = 2;
        gbc_lblSize.gridy = 0;
        panel_1.add(lblSize, gbc_lblSize);
        
        /*
        JLabel lblUsed = new JLabel("% Used");
        GridBagConstraints gbc_lblUsed = new GridBagConstraints();
        gbc_lblUsed.insets = new Insets(0, 0, 5, 0);
        gbc_lblUsed.gridx = 3;
        gbc_lblUsed.gridy = 0;
        panel_1.add(lblUsed, gbc_lblUsed);
        */
        
        JLabel lblFirmware_1 = new JLabel("Firmware");
        GridBagConstraints gbc_lblFirmware_1 = new GridBagConstraints();
        gbc_lblFirmware_1.anchor = GridBagConstraints.EAST;
        gbc_lblFirmware_1.insets = new Insets(0, 0, 5, 5);
        gbc_lblFirmware_1.gridx = 0;
        gbc_lblFirmware_1.gridy = 1;
        panel_1.add(lblFirmware_1, gbc_lblFirmware_1);
        
        txtFwVersion = new JTextField();
        txtFwVersion.setEditable(false);
        GridBagConstraints gbc_txtFwVersion = new GridBagConstraints();
        gbc_txtFwVersion.insets = new Insets(0, 0, 5, 5);
        gbc_txtFwVersion.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtFwVersion.gridx = 1;
        gbc_txtFwVersion.gridy = 1;
        panel_1.add(txtFwVersion, gbc_txtFwVersion);
        txtFwVersion.setColumns(10);

        txtFwSize = new JTextField();
        txtFwSize.setEditable(false);
        GridBagConstraints gbc_textField_2 = new GridBagConstraints();
        gbc_textField_2.insets = new Insets(0, 0, 5, 5);
        gbc_textField_2.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField_2.gridx = 2;
        gbc_textField_2.gridy = 1;
        panel_1.add(txtFwSize, gbc_textField_2);
        txtFwSize.setColumns(10);
        
        /*
        txtFwUsed = new JTextField();
        txtFwUsed.setEditable(false);
        GridBagConstraints gbc_textField_3 = new GridBagConstraints();
        gbc_textField_3.insets = new Insets(0, 0, 5, 0);
        gbc_textField_3.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField_3.gridx = 3;
        gbc_textField_3.gridy = 1;
        panel_1.add(txtFwUsed, gbc_textField_3);
        txtFwUsed.setColumns(10);
        */
        
        JLabel lblLibrary_1 = new JLabel("Library");
        GridBagConstraints gbc_lblFilesystem_1 = new GridBagConstraints();
        gbc_lblFilesystem_1.anchor = GridBagConstraints.EAST;
        gbc_lblFilesystem_1.insets = new Insets(0, 0, 0, 5);
        gbc_lblFilesystem_1.gridx = 0;
        gbc_lblFilesystem_1.gridy = 2;
        panel_1.add(lblLibrary_1, gbc_lblFilesystem_1);
        
        txtLibVersion = new JTextField();
        txtLibVersion.setEditable(false);
        GridBagConstraints gbc_txtFsVersion = new GridBagConstraints();
        gbc_txtFsVersion.insets = new Insets(0, 0, 5, 5);
        gbc_txtFsVersion.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtFsVersion.gridx = 1;
        gbc_txtFsVersion.gridy = 2;
        panel_1.add(txtLibVersion, gbc_txtFsVersion);
        txtLibVersion.setColumns(10);

        txtLibSize = new JTextField();
        txtLibSize.setEditable(false);
        GridBagConstraints gbc_textField_4 = new GridBagConstraints();
        gbc_textField_4.insets = new Insets(0, 0, 0, 5);
        gbc_textField_4.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField_4.gridx = 2;
        gbc_textField_4.gridy = 2;
        panel_1.add(txtLibSize, gbc_textField_4);
        txtLibSize.setColumns(10);
        
        /*
        txtLibUsed = new JTextField();
        txtLibUsed.setEditable(false);
        GridBagConstraints gbc_textField_5 = new GridBagConstraints();
        gbc_textField_5.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField_5.gridx = 3;
        gbc_textField_5.gridy = 2;
        panel_1.add(txtLibUsed, gbc_textField_5);
        txtLibUsed.setColumns(10);
        */
        
        InstallBtnAction action = new InstallBtnAction(gui.getMonitor(), gui.getProgressBar(), "Install/Upgrade", "Install firmware and/or filesystem", "Cancel installation");
        btnInstall = new JButton(action);
        //btnInstall.setEnabled(false);

        btnInstall.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            }
        });
        
        chckbxFormatRoot = new JCheckBox("Format Root");
        GridBagConstraints gbc_chckbxFormatRoot = new GridBagConstraints();
        gbc_chckbxFormatRoot.anchor = GridBagConstraints.WEST;
        gbc_chckbxFormatRoot.gridwidth = 2;
        gbc_chckbxFormatRoot.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxFormatRoot.gridx = 0;
        gbc_chckbxFormatRoot.gridy = 2;
        add(chckbxFormatRoot, gbc_chckbxFormatRoot);
                
        chckbxFormatMedia = new JCheckBox("Format Media");
        GridBagConstraints gbc_chckbxFormatMedia = new GridBagConstraints();
        gbc_chckbxFormatMedia.anchor = GridBagConstraints.WEST;
        gbc_chckbxFormatMedia.gridwidth = 2;
        gbc_chckbxFormatMedia.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxFormatMedia.gridx = 0;
        gbc_chckbxFormatMedia.gridy = 3;
        add(chckbxFormatMedia, gbc_chckbxFormatMedia);
        
        chckbxReplaceTxini = new JCheckBox("Replace tx.ini");
        GridBagConstraints gbc_chckbxReplaceTxini = new GridBagConstraints();
        gbc_chckbxReplaceTxini.anchor = GridBagConstraints.WEST;
        gbc_chckbxReplaceTxini.gridwidth = 2;
        gbc_chckbxReplaceTxini.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxReplaceTxini.gridx = 0;
        gbc_chckbxReplaceTxini.gridy = 4;
        add(chckbxReplaceTxini, gbc_chckbxReplaceTxini);
        
        chckbxReplaceModels = new JCheckBox("Replace models");
        GridBagConstraints gbc_chckbxReplaceModels = new GridBagConstraints();
        gbc_chckbxReplaceModels.anchor = GridBagConstraints.WEST;
        gbc_chckbxReplaceModels.gridwidth = 2;
        gbc_chckbxReplaceModels.insets = new Insets(0, 0, 5, 5);
        gbc_chckbxReplaceModels.gridx = 0;
        gbc_chckbxReplaceModels.gridy = 5;
        add(chckbxReplaceModels, gbc_chckbxReplaceModels);
        GridBagConstraints gbc_btnInstall = new GridBagConstraints();
        gbc_btnInstall.gridwidth = 4;
        gbc_btnInstall.gridx = 0;
        gbc_btnInstall.gridy = 6;
        add(btnInstall, gbc_btnInstall);
        
        update_checkboxes();
        update_install_button();
    }
    public void parseZipFiles() {
        fw = zipFiles.GetFirmwareInfo();
        txtFwVersion.setText("");
        txtFwSize.setText("");
        txtLibVersion.setText("");
        txtLibSize.setText("");
        if (fw.Found()) {
            txtFwVersion.setText(fw.version());
            DfuFile fwDfu = zipFiles.GetFirmwareDfu();
            if (fwDfu != null) {
                int size = 0;
                for (DfuFile.ImageElement elem : fwDfu.imageElements()) {
                    size += elem.data().length;
                }
                txtFwSize.setText(String.valueOf(size / 1024) + " kb");
            }            
        }
        lib = zipFiles.GetLibraryInfo();
        if (lib.Found()) {
            txtLibVersion.setText(lib.version());
            
            int size = 0;
            for (DfuFile fsDfu : zipFiles.GetLibraryDfus()) {
                for (DfuFile.ImageElement elem : fsDfu.imageElements()) {
                    size += elem.data().length;
                }
            }
            for (FileInfo file : zipFiles.GetFilesystemFiles()) {
                if (! file.name().matches(".*\\.dfu")) {
                    size += file.size();
                }
            }
            txtLibSize.setText(String.valueOf(size / 1024) + " kb");

        }
        update_checkboxes();
        update_install_button();
    }
    private void update_checkboxes() {
            chckbxFormatRoot.setSelected(false);
            chckbxFormatRoot.setEnabled(false);
            chckbxFormatMedia.setSelected(false);
            chckbxFormatMedia.setEnabled(false);
            chckbxReplaceTxini.setSelected(false);
            chckbxReplaceTxini.setEnabled(false);
            chckbxReplaceModels.setSelected(false);
            chckbxReplaceModels.setEnabled(false);
        if (gui.getTxInfo().type() == TxModel.DEVO_UNKNOWN || lib.firmware() != Firmware.DEVIATION){
            return;
        }
        if (gui.getFatType() == FatStatus.NO_FAT || gui.getFatType() == FatStatus.MEDIA_FAT) {
            chckbxFormatRoot.setSelected(true);
            chckbxReplaceTxini.setSelected(true);
            chckbxReplaceModels.setSelected(true);            
        } else {
            chckbxFormatRoot.setEnabled(true);
            chckbxReplaceTxini.setEnabled(true);
            chckbxReplaceModels.setEnabled(true);            
        }
        if (gui.getTxInfo().type() == TxModel.DEVO12) {
            if (gui.getFatType() == FatStatus.NO_FAT || gui.getFatType() == FatStatus.ROOT_FAT) {
                chckbxFormatMedia.setSelected(true);                
            } else {
                chckbxFormatMedia.setEnabled(true);                
            }
        }
        List<DfuDevice> devs = gui.getMonitor().GetDevices();
        if (devs != null) {
            DfuDevice dev = devs.get(0);
            dev.SelectInterface(dev.Interfaces().get(0));
            if (dev.open() != 0) {
                System.out.println("Error: Unable to open device");
                return;
            }
            dev.claim_and_set();
            Dfu.setIdle(dev);
                 
            DevoFat fat = new DevoFat(dev, gui.getTxInfo().type());
            try {
            fat.Init(FatStatus.ROOT_AND_MEDIA_FAT);
            } catch (Exception e) { System.out.println(e); }
            fat.readDir("/media");
            dev.close();
            gui.getMonitor().ReleaseDevices();
        }
    }
    private void update_install_button() {
        boolean enabled = true;
        if(fw.Found()) {
            if(! gui.getTxInfo().matchModel(fw.model())) {
                enabled = false;
            }
        }
        if(lib.Found()) {
            if(! gui.getTxInfo().matchModel(lib.model())) {
                enabled = false;
            }
        }
        if (!fw.Found() && ! lib.Found()) {
            enabled = false;
        }
        btnInstall.setEnabled(enabled);
    }
    private class FileChooserBtnListener implements ActionListener {
        JTextField txtField;
        public FileChooserBtnListener(JTextField txt) {
            super();
            txtField = txt;
        }
        public void actionPerformed(ActionEvent e) {
            final JFileChooser fc = new JFileChooser();
            FileNameExtensionFilter ff = new FileNameExtensionFilter("Zip Files", "zip");
            fc.addChoosableFileFilter(ff);
            fc.setFileFilter(ff);
            int returnVal = fc.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String fname = fc.getSelectedFile().getPath();
                if (! txtField.getText().isEmpty()) {
                    zipFiles.RemoveZipFile(txtField.getText());
                }
                zipFiles.AddZipFile(fname);
                txtField.setText(fname);
                parseZipFiles();
//                } catch (IOException ex) {
//                    System.err.println("Caught IOException: " + ex.getMessage());
//                }

            }
        }
    }


}
