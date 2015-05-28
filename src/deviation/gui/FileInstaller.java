package deviation.gui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import deviation.filesystem.TxInterface;
import deviation.filesystem.TxInterface.FSStatus;
import deviation.DeviationUploader;
import deviation.DfuDevice;
import deviation.DfuFile;
import deviation.FileInfo;
import deviation.Progress;

class FileInstaller extends SwingWorker<String, Integer> implements Progress {
	private final DeviationUploadGUI gui;
	private ButtonAction buttonAction;

	private DfuFile firmwareDfu;
	private List<DfuFile> libraryDfus;
	private List<FileInfo> files;
	private boolean format_root;
	private boolean format_media;
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
				DfuDevice dev = gui.getTxInterface().getDevice();
				if (dev == null) {
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
	public FileInstaller(DeviationUploadGUI gui) {
		this.gui = gui;
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

	private void updateRoot(TxInterface fs) {
		if (! format_root && ! format_media && files.size() == 0)
			return;
		DfuDevice dev =fs.getDevice(); //This will 
		if (dev.open() != 0) {
			System.out.println("Error: Unable to open device");
			return;
		}
		dev.claim_and_set();

		try {
			if (format_root) {
				fs.Format(FSStatus.ROOT_FS);
			} else {
				fs.Init(FSStatus.ROOT_FS);
			}
			if (format_media) {
				fs.Format(FSStatus.MEDIA_FS);
			} else {
				fs.Init(FSStatus.MEDIA_FS);
			}
		} catch (Exception e) { System.out.println(e); }
		for (FileInfo file: files) {
			fs.copyFile(file);
		}
		fs.close();
		dev.close();
	}
    @Override
    protected void process( List<Integer> blocks ) {
        //update the percentage of the progress bar that is done
    	for (Integer block : blocks) {
    		bytesTransferred += block;
    	}
    	JProgressBar progressBar = gui.getProgressBar();
        int amount = progressBar.getMaximum() - progressBar.getMinimum();
        progressBar.setValue( ( int ) (progressBar.getMinimum() + ( amount * 1.0 * bytesTransferred / totalBytes)));
    }
    @Override
    protected void done() {
        try {
            if (get().equals("Finished")) {
                System.out.println("Completed transfer");
                gui.getProgressBar().setValue(100);
            }
        } catch (Exception e) {
            System.out.println("Completed failed");
            gui.getProgressBar().setValue(0);
        }
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
		TxInterface tx = gui.getTxInterface();
		DfuDevice dev = tx.getDevice();  //ReleaseDevices is called in done()
		if (dev.open() != 0) {
			return "Failed";
		}
		bytesTransferred = 0;
		if (firmwareDfu != null) {
			DeviationUploader.sendDfuToDevice(dev, firmwareDfu, this);
		}
		if (libraryDfus != null && libraryDfus.size() > 0) {
			for(DfuFile dfu : libraryDfus) {
				DeviationUploader.sendDfuToDevice(dev, dfu, this);
			}
		}
		updateRoot(tx);
		dev.close();
        return "Finished";
	}

}