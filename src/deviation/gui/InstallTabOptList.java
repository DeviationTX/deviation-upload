package deviation.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import deviation.Transmitter;
import deviation.DevoDetect.Firmware;
import deviation.DfuFile;
import deviation.FileGroup;


public class InstallTabOptList extends JScrollPane {
	public class Options {
    	private final JCheckBox chkbx;
    	private final String off;
    	private final String on;
    	public Options(String str, String off, String on) {
    		chkbx = new JCheckBox(str);
        	chkbx.addItemListener( new ItemListener() {
        	    public void itemStateChanged(ItemEvent e) {
        	    	update_checkboxes();
        	    }
        	});
    		this.off = off;
    		this.on  = on;
    	}
    	public void disable() {
    		chkbx.setEnabled(false);
    		chkbx.setSelected(false);
    	}
    	public void set(boolean val) {
    		chkbx.setEnabled(true);
    		chkbx.setSelected(val);
    		chkbx.setToolTipText(val ? on : off);
    	}
    	public boolean get() {
    		return chkbx.isSelected();
    	}
    	public JCheckBox getCheckbox() { return chkbx; }
    	//public int idx() { return ordinal(); }
    };
    public final Options FORMAT = new Options(
    		"Format",
    		"Filesystem not detected, format required",
    		"Filesystem already installed, no format needed");
    public final Options INSTALLPROTO = new Options(
    		"Install Protocols", "1", "2");
    public final Options INSTALLLIB = new Options(
    		"Install Library", "1", "2");
    public final Options REPLACETX = new Options(
    		"Replace tx.ini", "1", "2");
    public final Options REPLACEHW = new Options(
    		"Replace hardware.ini", "1", "2");
    public final Options REPLACEMODEL = new Options(
    		"Replace models", "1", "2");
    public final Options AllOpts[] = new Options[] {FORMAT, INSTALLPROTO, INSTALLLIB, REPLACETX, REPLACEHW, REPLACEMODEL};
    public final int AUTOMATIC    = 0;
    public final int DFU_ONLY     = 1;
    public final int INCREMENTAL  = 2;
    public final int FULL_INSTALL = 3;
    public final int ADVANCED     = 4;
	private final Box box;
	private final DeviationUploadGUI gui;
    private final FileGroup zipFiles;
    private final JComboBox<String> combobox;


	InstallTabOptList(DeviationUploadGUI gui, FileGroup zipFiles) {
		super();
		this.gui = gui;
		this.zipFiles = zipFiles;
        box = Box.createVerticalBox();
        setViewportView(box);
    	setPreferredSize(new Dimension(140, 100));

        combobox = new JComboBox<String>();
        combobox.addItem("Automatic");
        combobox.addItem("DFU Only");
        combobox.addItem("Incremental");
        combobox.addItem("Full Install");
        combobox.addItem("Advanced");
        combobox.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		updateMode();
        	}
        });
	}
	public JComboBox<String> getModeBox() {
		return combobox;
	}
	public void ShowAutomatic() {		
		box.removeAll();
		if (! showOptions()) {
			return;
		}
		if (zipFiles.GetFirmwareDfu() != null) {
			box.add(new JLabel("Installing firmware"));
		}
		if (FORMAT.get()) {
			box.add(new JLabel (isFormatted() ?
					"Re-formatting filesystem" :
					"Formatting un-formatted filesystem"));
		}
		if (INSTALLPROTO.get()) {
			box.add(new JLabel(gui.getTxInfo().type().needsFsProtocols() == Transmitter.ProtoFiles.ALL ?
					"Replacing all protocol files" :
					"Replacing existing protocol files"));
		}
		if (INSTALLLIB.get()) {
			box.add(new JLabel("Installing images and translations"));
		}
		if (REPLACEMODEL.get()) {
			box.add(new JLabel("Installing model files (this will overwrite any existing files)"));
		}
		if (REPLACEHW.get()) {
			box.add(new JLabel("Installing hardware.ini (this will overwrite any existing file)"));
		}
		if (REPLACETX.get()) {
			box.add(new JLabel("Installing tx.ini (this will overwrite any existing file)"));
		}
		gui.getFrame().repaint();
	}
	public void ShowAdvanced() {
		box.removeAll();
		if (! showOptions()) {
			return;
		}
        box.add(new JLabel("Options:"));
        box.add(FORMAT.getCheckbox());
        if (FORMAT.get() || isFormatted()) {
        	if (showProtocol()) {
        		box.add(INSTALLPROTO.getCheckbox());
        	}
        	if (zipFiles.hasLibrary()) {
        		box.add(INSTALLLIB.getCheckbox());
        	}
        	box.add(REPLACETX.getCheckbox());
        	box.add(REPLACEHW.getCheckbox());
        	box.add(REPLACEMODEL.getCheckbox());
        }
		gui.getFrame().repaint();
	}
	private boolean showOptions() {
    	DfuFile fw = zipFiles.GetFirmwareDfu();
        if (gui.getTxInfo().type().isUnknown() || (fw != null && fw.type().firmware() != Firmware.DEVIATION)) {
            return false;
        }
        return true;
	}
	private boolean showProtocol() {
		return gui.getTxInfo().type().needsFsProtocols() != Transmitter.ProtoFiles.NONE && zipFiles.hasProtocol();
	}
	private boolean isFormatted() {
		return gui.getFSStatus().isFormatted();
	}
/*  private void reset_checkboxes() {
		//box.removeAll();
        boolean chkboxval = (! gui.getFSStatus().isFormatted());
        FORMAT.set(chkboxval);
        REPLACETX.set(chkboxval);
        REPLACEHW.set(chkboxval);
        REPLACEMODEL.set(chkboxval);
        if (zipFiles.hasLibrary()) {
        	INSTALLLIB.set(true);
        }
        if (gui.getTxInfo().type().needsFsProtocols() != Transmitter.ProtoFiles.NONE && zipFiles.hasProtocol()) {
        	INSTALLPROTO.set(true);
        }
        //update_checkboxes();
    }
*/
    private void update_checkboxes() {
    	if (FORMAT.get()) {
    		REPLACETX.set(true);
    		REPLACEHW.set(true);
    		REPLACEMODEL.set(true);
    	}
//    	ShowAdvanced();
    	if (combobox.getSelectedIndex() != 4) {
    		ShowAutomatic();
    	} else {
    		ShowAdvanced();
    	}
    	gui.getInstallTab().update_files_to_install();
    }
    public void recompute_checkboxes() {
    	updateMode();
    }
    private void updateMode() {
    	for (Options c : AllOpts) {
    		c.disable();
    	}
    	if (! showOptions()) {
    		return;
    	}
        boolean needFormat = (! gui.getFSStatus().isFormatted());
    	if (needFormat) {
    		if (combobox.getSelectedIndex() == AUTOMATIC ||
    			combobox.getSelectedIndex() == INCREMENTAL) {
    			combobox.setSelectedIndex(FULL_INSTALL);
    		}
    	} else if (combobox.getSelectedIndex() == AUTOMATIC) {
    		combobox.setSelectedIndex(INCREMENTAL);
    	}
		if (combobox.getSelectedIndex() == FULL_INSTALL) {
			FORMAT.set(true);
		}
        if ((FORMAT.get() || isFormatted()) && gui.getTxInfo().type().needsFsProtocols() != Transmitter.ProtoFiles.NONE && zipFiles.hasProtocol()) {
        	INSTALLPROTO.set(true);
        }
    	if (combobox.getSelectedIndex() != DFU_ONLY) {
            REPLACETX.set(needFormat);
            REPLACEHW.set(needFormat);
            REPLACEMODEL.set(needFormat);
            if (zipFiles.hasLibrary()) {
            	INSTALLLIB.set(true);
            }
    		update_checkboxes();
    	}
    }
}
