package deviation.gui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JProgressBar;

import deviation.DevoFat;
import deviation.DevoFat.FatStatus;
import deviation.DfuDevice;
import deviation.DfuFile;
import deviation.FileInfo;

class FileInstaller {
	private final JProgressBar progressBar;
	private MonitorUSB monitor;
	private ButtonAction buttonAction;

	private DfuFile firmwareDfu;
	private List<DfuFile> libraryDfus;
	private List<FileInfo> files;
	private boolean format_root;
	private boolean format_media;
	private final int SECTOR_SIZE = 0x1000;

	class ButtonAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		private String normalLbl;
		private String normalDesc;
		private String cancelDesc;
		private String cancelLbl;

		private DfuCmdWorker worker;		

		public ButtonAction(String text, String normalDesc, String cancelDesc) {
			super(text);
			normalLbl = text;


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
/*
				if (firmwareDfu != null) {
					setCancelState(true);
					worker = new DfuCmdWorker( devs, firmwareDfu, progressBar, FileInstaller.this, monitor);
					worker.execute();
				}
				if (libraryDfus.size() > 0) {
					for(DfuFile dfu : libraryDfus) {
						setCancelState(true);
						worker = new DfuCmdWorker( devs, dfu, progressBar, FileInstaller.this, monitor);
						worker.execute();
					}
				}
*/
				updateRoot();
			} else {
				//Note that the worker disables the cancel state
				worker.cancel(false);
			}
		}
		private void updateRoot() {
			List<DfuDevice> devs = monitor.GetDevices();
			if (devs == null)
				return;
			DfuDevice dev = devs.get(0);
    		if (dev.open() != 0) {
    			System.out.println("Error: Unable to open device");
    			return;
    		}
    		dev.claim_and_set();

			DevoFat fat = new DevoFat(dev);
			try {
				if (format_root) {
					fat.Format(FatStatus.ROOT_FAT);
				} else {
					fat.Init(FatStatus.ROOT_FAT);
				}
				if (format_media) {
					fat.Format(FatStatus.MEDIA_FAT);
				} else {
					fat.Init(FatStatus.MEDIA_FAT);
				}
			} catch (Exception e) { System.out.println(e); }
			for (FileInfo file: files) {
				fat.copyFile(file);
			}
			fat.close();
			dev.close();
			monitor.ReleaseDevices();
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
	public FileInstaller(MonitorUSB monitor, JProgressBar progressBar) {
		this.progressBar = progressBar;
		this.monitor = monitor;
		buttonAction = null;
		firmwareDfu = null;
		libraryDfus = new ArrayList<DfuFile>();
		files = new ArrayList<FileInfo>();
	}
	public AbstractAction getButtonAction(String text, String normalDesc, String cancelDesc) {
		buttonAction = new ButtonAction(text, normalDesc, cancelDesc);
		return buttonAction;
	}
	public void setCancelState(boolean state)  { buttonAction.setCancelState(state); }
	public void setFirmwareDfu(DfuFile dfu) { firmwareDfu = dfu; }
	public void setLibraryDfus(List<DfuFile> dfus) { libraryDfus = dfus; }
	public void clearFiles() { files.clear(); }
	public void addFile(FileInfo file) { files.add(file); }
	public void formatRoot(boolean fmt) { format_root = fmt; }
	public void formatMedia(boolean fmt) { format_media = fmt; }
}