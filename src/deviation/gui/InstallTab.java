package deviation.gui;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
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

public class InstallTab extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_DIR = "DefaultDir";
    
    private JTextField firmwareTxt;
    private JTextField libraryTxt;
    private JTextField txtFwVersion;
    private JTextField txtFwSize;
    private JTextField txtFwUsed;
    private JTextField txtLibVersion;
    private JTextField txtLibSize;
    private JTextField txtLibUsed;
    private enum Checkbox {
    	FORMATROOT ("Format Root"),
    	FORMATMEDIA ("Format Media"),
    	INSTALLLIB ("Install Library"),
    	REPLACETX ("Replace tx.ini"),
    	REPLACEHW ("Replace hardware.ini"),
    	REPLACEMODEL ("Replace models");
    	
    	private final JCheckBox chkbx;
    	private Checkbox(String str) {
    		chkbx = new JCheckBox(str);
    	}
    	public void disable() {
    		chkbx.setEnabled(false);
    		chkbx.setSelected(false);
    	}
    	public void set(boolean val) {
    		chkbx.setEnabled(true);
    		chkbx.setSelected(val);
    	}
    	public JCheckBox get() { return chkbx; }
    	//public int idx() { return ordinal(); }
    };
    
    private JButton btnInstall;
    private JButton filesystemBtn;

    private FileInstaller fileInstaller;
    private FileGroup zipFiles;
    DeviationUploadGUI gui;

    DfuFile fw;
    List<DfuFile> libs;
    public InstallTab(DeviationUploadGUI gui) {
        this.gui = gui;
        zipFiles = new FileGroup();
        fw = null;
        libs = null;
        fileInstaller = new FileInstaller(gui.getMonitor(), gui.getProgressBar());
        
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
        
        filesystemBtn = new JButton("...");
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
        
        AbstractAction action = fileInstaller.getButtonAction("Install/Upgrade", "Install firmware and/or filesystem", "Cancel installation");
        btnInstall = new JButton(action);
        //btnInstall.setEnabled(false);

        btnInstall.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
            }
        });

        int row = 2;
        for (Checkbox box : Checkbox.values()) {
        	JCheckBox chkbx = box.get();
        	checkboxSetCallback(chkbx);
        	GridBagConstraints gbc_chckbx = new GridBagConstraints();
        	gbc_chckbx.anchor = GridBagConstraints.WEST;
        	gbc_chckbx.gridwidth = 2;
        	gbc_chckbx.insets = new Insets(0, 0, 5, 5);
        	gbc_chckbx.gridx = 0;
        	gbc_chckbx.gridy = row++;
        	add(chkbx, gbc_chckbx);
        }
        
        GridBagConstraints gbc_btnInstall = new GridBagConstraints();
        gbc_btnInstall.gridwidth = 4;
        gbc_btnInstall.gridx = 0;
        gbc_btnInstall.gridy = row;
        add(btnInstall, gbc_btnInstall);
        
        reset_checkboxes();
    }
    public void refresh() {
    	reset_checkboxes();
    }
    private void checkboxSetCallback(JCheckBox chkbx) {
    	chkbx.addItemListener(new ItemListener() {
    	    public void itemStateChanged(ItemEvent e) {
    	    	update_checkboxes();
    	    }
    	});
    }
    public void parseZipFiles() {
        fw = zipFiles.GetFirmwareDfu();
        libs = zipFiles.GetLibraryDfus();
        reset_checkboxes();
        update_filechooser();
    }
    private void update_filechooser() {
    	String fwFile = zipFiles.firmwareZip();
    	String libFile = zipFiles.libraryZip();
    	firmwareTxt.setText(fwFile);
    	if(fwFile.equals(libFile) && fw != null && zipFiles.hasLibrary()) {
    		//one zip file with both lib and 
    		libFile = "";
    		filesystemBtn.setEnabled(false);
        	libraryTxt.setEnabled(false);
    	} else {
    		filesystemBtn.setEnabled(true);
        	libraryTxt.setEnabled(true);
    	}
    	libraryTxt.setText(libFile);
    }
    private void reset_checkboxes() {
    	for (Checkbox c : Checkbox.values()) {
    		c.disable();
    	}
        if (gui.getTxInfo().type() == Transmitter.DEVO_UNKNOWN || (fw != null && fw.type().firmware() != Firmware.DEVIATION)) {
            return;
        }
        boolean chkboxval = (gui.getFatType() == FatStatus.NO_FAT || gui.getFatType() == FatStatus.MEDIA_FAT);
        Checkbox.FORMATROOT.set(chkboxval);
        Checkbox.REPLACETX.set(chkboxval);
        Checkbox.REPLACEHW.set(chkboxval);
        Checkbox.REPLACEMODEL.set(chkboxval);
        if (gui.getTxInfo().type().hasMediaFS()) {
            if (gui.getFatType() == FatStatus.NO_FAT || gui.getFatType() == FatStatus.ROOT_FAT) {
                Checkbox.FORMATMEDIA.set(true);
            } else {
                Checkbox.FORMATMEDIA.set(false);
            }
        }
        if (zipFiles.hasLibrary()) {
        	Checkbox.INSTALLLIB.set(true);
        }
        update_checkboxes();
    }
    private void update_checkboxes() {
    	if (Checkbox.FORMATROOT.get().isSelected()) {
    		Checkbox.REPLACETX.set(true);
    		Checkbox.REPLACEHW.set(true);
    		Checkbox.REPLACEMODEL.set(true);
    	}
    	update_files_to_install();
    }
    private void update_install_button() {
        boolean enabled = true;
        if(fw != null) {
            if(! gui.getTxInfo().matchModel(fw.type().model())) {
                enabled = false;
            }
        }
        if(zipFiles.hasLibrary()) {
            if(libs.size() > 0 && ! gui.getTxInfo().matchModel(libs.get(0).type().model())) {
                enabled = false;
            }
        }
        if (fw == null && ! zipFiles.hasLibrary()) {
            enabled = false;
        }
        btnInstall.setEnabled(enabled);
    }
    private void update_files_to_install() {
        txtFwVersion.setText("");
        txtFwSize.setText("");
        txtLibVersion.setText("");
        txtLibSize.setText("");
        fileInstaller.clearFiles();
        fileInstaller.setLibraryDfus(null);
    	fileInstaller.setFirmwareDfu(null);
    	long totalSize = 0;
        if (fw!= null) {
            txtFwVersion.setText(fw.type().version());
           	fileInstaller.setFirmwareDfu(fw);
            int size = 0;
            for (DfuFile.ImageElement elem : fw.imageElements()) {
                size += elem.data().length;
            }
            txtFwSize.setText(String.valueOf(size / 1024) + " kb");
            totalSize += size;
        }
        if (Checkbox.INSTALLLIB.get().isSelected() && zipFiles.hasLibrary()) {
        	if (libs.size() > 0) {
        		txtLibVersion.setText(libs.get(0).type().version());
                fileInstaller.setLibraryDfus(libs);
        	} else if (fw != null) {
        		txtLibVersion.setText(fw.type().version());
        	} else {
        		DevoDetect type = new DevoDetect();
        		txtLibVersion.setText(type.version());
        	}
            
            int size = 0;
            for (DfuFile fsDfu : libs) {
                for (DfuFile.ImageElement elem : fsDfu.imageElements()) {
                    size += elem.data().length;
                }
            }
            for (FileInfo file : zipFiles.GetFilesystemFiles()) {
                if (! Checkbox.REPLACETX.get().isSelected() && file.name().equalsIgnoreCase("tx.ini"))
                   	continue;
                if (! Checkbox.REPLACEHW.get().isSelected() && file.name().equalsIgnoreCase("hardware.ini"))
                  	continue;
                if (! Checkbox.REPLACEMODEL.get().isSelected() && file.name().matches("(?i:models/.*)"))
                	continue;
                size += file.size();
                fileInstaller.addFile(file);
            }
            txtLibSize.setText(String.valueOf(size / 1024) + " kb");
            totalSize += size;
        }
        fileInstaller.formatRoot(Checkbox.FORMATROOT.get().isSelected());
        fileInstaller.formatMedia(Checkbox.FORMATMEDIA.get().isSelected());
        fileInstaller.setTotalBytes(totalSize);
        update_install_button();
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
            String startDir = prefs.get(DEFAULT_DIR, System.getProperty("user.home"));
            fc.setCurrentDirectory(new File(startDir));
            FileNameExtensionFilter ff = new FileNameExtensionFilter("Zip Files", "zip");
            fc.addChoosableFileFilter(ff);
            fc.setFileFilter(ff);
            int returnVal = fc.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
            	startDir = fc.getSelectedFile().getParent();
            	if (startDir != null) {
            		prefs.put(DEFAULT_DIR,  startDir);
            	}
                String fname = fc.getSelectedFile().getPath();
                if (! txtField.getText().isEmpty()) {
                    zipFiles.RemoveFile(txtField.getText());
                }
                zipFiles.AddFile(fname);
                txtField.setText(fname);
                parseZipFiles();
//                } catch (IOException ex) {
//                    System.err.println("Caught IOException: " + ex.getMessage());
//                }

            }
        }
    }


}
