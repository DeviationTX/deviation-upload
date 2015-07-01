package deviation.gui;

import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import deviation.filesystem.TxInterface;
import deviation.filesystem.FSStatus;
import deviation.DeviationUploader;
import deviation.DfuDevice;
import deviation.DfuFile;
import deviation.FileInfo;
import deviation.Progress;

class FileInstaller extends SwingWorker<Boolean, Integer> implements Progress {
	private final DeviationUploadGUI gui;

	private final DfuFile firmwareDfu;
	private final List<DfuFile> libraryDfus;
	private final List<FileInfo> files;
	private final boolean format;
	private final long totalBytes;

	private long bytesTransferred;

	public FileInstaller(DeviationUploadGUI gui, FilesToSend fileList) {
		this.gui = gui;
		firmwareDfu = fileList.getFirmwareDfu();
		libraryDfus = fileList.getLibraryDfus();
		files = fileList.getFiles();
		format = fileList.format();
		totalBytes = fileList.getTotalBytes();
	}

	private void updateRoot(TxInterface fs) {
		if (! format && files.size() == 0)
			return;
		fs.open();
		fs.setProgress(this);

		try {
			if (format) {
				fs.Format(new FSStatus(gui.getTxInfo().type(), false, false));
			} else {
				fs.Init(gui.getFSStatus());
			}
		} catch (Exception e) { e.printStackTrace(); }
		for (FileInfo file: files) {
			if (cancelled()) {
				break;
			}
			fs.copyFile(file);
		}
		fs.close();
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
	public void update(Integer block) {
		publish(block);
	}
	public boolean cancelled() {
		boolean ret = Thread.currentThread().isInterrupted();
		boolean ret1 = isCancelled();
		return ret1 || ret;
	}
	public void failIfInterrupted() throws InterruptedException {
		if (cancelled()) {
			throw new InterruptedException("Canceled");
		}
	}
	@Override
	protected Boolean doInBackground() throws Exception {
		TxInterface tx = gui.getTxInterface();
		DfuDevice dev = tx.getDevice();  //ReleaseDevices is called in done()
		if (dev == null || dev.open() != 0) {
			return false;
		}
		bytesTransferred = 0;
		try {
			if (firmwareDfu != null) {
				DeviationUploader.sendDfuToDevice(dev, firmwareDfu, this);
				failIfInterrupted();
			}
			if (libraryDfus != null && libraryDfus.size() > 0) {
				for(DfuFile dfu : libraryDfus) {
					DeviationUploader.sendDfuToDevice(dev, dfu, this);
					failIfInterrupted();
				}
			}
			updateRoot(tx);
			failIfInterrupted();
		}
		finally {
			dev.close();
		}
        return true;
	}

}