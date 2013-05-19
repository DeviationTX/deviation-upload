package deviation.gui;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JProgressBar;

import deviation.DfuDevice;
import deviation.DfuFile;

class InstallBtnAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    
    private String normalLbl;
    private String normalDesc;
    private String cancelDesc;
    private String cancelLbl;
    private final JProgressBar progressBar;
    
    private MonitorUSB monitor;
    private DfuCmdWorker worker;
    
    public InstallBtnAction(MonitorUSB monitor, JProgressBar progressBar, String text, String normalDesc, String cancelDesc) {
        super(text);
        normalLbl = text;
        this.progressBar = progressBar;
        this.monitor = monitor;
        this.normalDesc = normalDesc;
        this.cancelDesc = cancelDesc;
        this.cancelLbl = "Cancel";
        putValue(SHORT_DESCRIPTION, normalDesc);
    }
    public void actionPerformed(ActionEvent e) {
        if (getValue(NAME).equals(normalLbl)) {
            List<DfuDevice> devs = monitor.GetDevices();
            if (devs == null) {
                return;
            }
            setCancelState(true);
            
            DfuFile dfuFile = null;
            worker = new DfuCmdWorker( devs, dfuFile, progressBar, this, monitor);
            worker.execute();
        } else {
            //Note that the worker disables the cancel state
            worker.cancel(false);
        }
    }
    public void setCancelState(boolean state) {
        if (state) {
            putValue(NAME, cancelLbl);
            putValue(SHORT_DESCRIPTION, cancelDesc);
        } else {
            putValue(NAME, normalLbl);
            putValue(SHORT_DESCRIPTION, normalDesc);
        }
    }
}