package deviation.gui;

import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import deviation.filesystem.TxInterface;
import deviation.filesystem.TxInterface.FSStatus;
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
	private final boolean format_root;
	private final boolean format_media;
	private final long totalBytes;

	private long bytesTransferred;

	public FileInstaller(DeviationUploadGUI gui, FilesToSend fileList) {
		this.gui = gui;
		firmwareDfu = fileList.getFirmwareDfu();
		libraryDfus = fileList.getLibraryDfus();
		files = fileList.getFiles();
		format_root = fileList.formatRoot();
		format_media = fileList.formatMedia();
		totalBytes = fileList.getTotalBytes();
	}

	private void updateRoot(TxInterface fs) {
		if (! format_root && ! format_media && files.size() == 0)
			return;
		DfuDevice dev =fs.getDevice(); //This will 
		if (dev.open() != 0) {
			System.out.println("Error: Unable to open device");
			return;
		}
		fs.setProgress(this);
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
		} catch (Exception e) { e.printStackTrace(); }
		for (FileInfo file: files) {
			if (cancelled()) {
				break;
			}
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