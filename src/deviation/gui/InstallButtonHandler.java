package deviation.gui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.SwingWorker.StateValue;


public class InstallButtonHandler extends AbstractAction{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6946668719177899532L;
	public enum buttonState {INSTALL, CANCEL};
	private final DeviationUploadGUI gui;
	private final FilesToSend fileList;
	private FileInstaller installer;
	private final String normalLbl;
	private final String normalDesc;
	private final String cancelDesc;
	private final String cancelLbl;
	
	public InstallButtonHandler(DeviationUploadGUI gui, FilesToSend fileList, String text, String normalDesc, String cancelDesc) {
		this.gui = gui;
		this.fileList = fileList;
		normalLbl = text;
		cancelLbl = "Cancel";
		this.normalDesc = normalDesc;
		this.cancelDesc = cancelDesc;
	}
	public void actionPerformed(ActionEvent arg0) {
		if (installer == null) {
			if (gui.getTxInterface().getDevice() == null) {
				return;
			}
			installer = new FileInstaller(gui, fileList);
			installer.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					switch (event.getPropertyName()) {
					case "state":
						switch ((StateValue) event.getNewValue()) {
						case DONE:
							setState(buttonState.INSTALL);
							gui.getProgressBar().setValue(100);
							installer = null;
							break;
						case STARTED:
						case PENDING:
							setState(buttonState.CANCEL);
							break;
						}
						break;
					}
				}
			});
			installer.execute();
		} else {
			installer.cancel(true);
		}
		
	}
	public void setState(buttonState state) {
		if (state == buttonState.INSTALL) {
			putValue(NAME, normalLbl);
			putValue(SHORT_DESCRIPTION, normalDesc);
		} else {
			putValue(NAME, cancelLbl);
			putValue(SHORT_DESCRIPTION, cancelDesc);
		}
	}

}
