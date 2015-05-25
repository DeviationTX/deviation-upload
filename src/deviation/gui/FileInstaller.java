package deviation.gui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import deviation.filesystem.TxInterface;
import deviation.filesystem.TxInterface.FatStatus;
import deviation.DeviationUploader;
import deviation.DfuDevice;
import deviation.DfuFile;
import deviation.FileInfo;
import deviation.Progress;

class FileInstaller extends SwingWorker<String, Integer> implements Progress {
	private final JProgressBar progressBar;
	private MonitorUSB monitor;
	private ButtonAction buttonAction;

	private DfuFile firmwareDfu;
	private List<DfuFile> libraryDfus;
	private List<FileInfo> files;
	private boolean format_root;
	private boolean format_media;
	private List<DfuDevice>  devs;
	private long bytesTransferred;
	private long totalBytes;

	class ButtonAction extends AbstractAction {
		private static final long serialVersionUID = 1L;

		private String normalLbl;
		private String normalDesc;
		private String cancelDesc;
		private String cancelLbl;

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
				devs = monitor.GetDevices();
				if (devs == null) {
					return;
				}
				setCancelState(true);
				FileInstaller.this.execute();
			} else {
				//Note that the worker disables the cancel state
				FileInstaller.this.cancel(false);
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
	public void setTotalBytes(long bytes) { totalBytes = bytes; }

	private void updateRoot(List<DfuDevice> devs) {
		if (! format_root && ! format_media && files.size() == 0)
			return;
		DfuDevice dev = devs.get(0);
		if (dev.open() != 0) {
			System.out.println("Error: Unable to open device");
			return;
		}
		dev.claim_and_set();

		TxInterface fat = new TxInterface(dev, this);
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
	}
    @Override
    protected void process( List<Integer> blocks ) {
        //update the percentage of the progress bar that is done
    	for (Integer block : blocks) {
    		bytesTransferred += block;
    	}
        int amount = progressBar.getMaximum() - progressBar.getMinimum();
        progressBar.setValue( ( int ) (progressBar.getMinimum() + ( amount * 1.0 * bytesTransferred / totalBytes)));
    }
    @Override
    protected void done() {
        try {
            if (get().equals("Finished")) {
                System.out.println("Completed transfer");
                progressBar.setValue(100);
            }
        } catch (Exception e) {
            System.out.println("Completed failed");
            progressBar.setValue(0);
        }
        monitor.ReleaseDevices();
        buttonAction.setCancelState(false);
    }
	public void update(Integer block) {
		publish(block);
	}
	public boolean cancelled() {
		return isCancelled();
	}
	@Override
	protected String doInBackground() throws Exception {
		//List<DfuDevice> devs = monitor.GetDevices();  //ReleaseDevices is called in done()
		bytesTransferred = 0;
		if (firmwareDfu != null) {
			DeviationUploader.sendDfuToDevice(devs, firmwareDfu, this);
		}
		if (libraryDfus != null && libraryDfus.size() > 0) {
			for(DfuFile dfu : libraryDfus) {
				DeviationUploader.sendDfuToDevice(devs, dfu, this);
			}
		}
		updateRoot(devs);
		
        return "Finished";
	}

}