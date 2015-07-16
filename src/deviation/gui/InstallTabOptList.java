package deviation.gui;

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
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
	private final Box box;
	private final DeviationUploadGUI gui;
    private final FileGroup zipFiles;


	InstallTabOptList(DeviationUploadGUI gui, FileGroup zipFiles) {
		super();
		this.gui = gui;
		this.zipFiles = zipFiles;
        box = Box.createVerticalBox();
        setViewportView(box);
    	setPreferredSize(new Dimension(140, 100));

	}
	public void ShowIncremental() {		
	}
	public void ShowAdvanced() {
		box.removeAll();
		if (! showOptions()) {
			return;
		}
        box.add(new JLabel("Options:"));
        for (Options opt : AllOpts) {
        	JCheckBox chkbx = opt.getCheckbox();
        	box.add(chkbx);
        }

	}
	private boolean showOptions() {
    	DfuFile fw = zipFiles.GetFirmwareDfu();
        if (gui.getTxInfo().type().isUnknown() || (fw != null && fw.type().firmware() != Firmware.DEVIATION)) {
            return false;
        }
        return true;
	}
    public void reset_checkboxes() {
		box.removeAll();
    	for (Options c : AllOpts) {
    		c.disable();
    	}
    	if (! showOptions()) {
    		return;
    	}
    	ShowAdvanced();
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
        update_checkboxes();
    }
    private void update_checkboxes() {
    	if (FORMAT.get()) {
    		REPLACETX.set(true);
    		REPLACEHW.set(true);
    		REPLACEMODEL.set(true);
    	}
    	gui.getInstallTab().update_files_to_install();
    }
}
