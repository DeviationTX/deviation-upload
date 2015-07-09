package deviation.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import deviation.FileInfo;
import deviation.Progress;
import deviation.filesystem.TxInterface;

public class FileCopyFromTx extends SwingWorker<Boolean, Integer> implements Progress {
	private final DeviationUploadGUI gui;
	private final List<FileInfo> files;
	private final Path destDir;
	private int numFiles;

	public FileCopyFromTx(DeviationUploadGUI gui, List<FileInfo> files, String destDir) {
		this.gui = gui;
		this.files = files;
		this.destDir = Paths.get(destDir);
	}
    @Override
    protected void process( List<Integer> count ) {
        //update the percentage of the progress bar that is done
    	for (Integer c: count) {
    		numFiles += c;
    	}
    	JProgressBar progressBar = gui.getProgressBar();
        int amount = progressBar.getMaximum() - progressBar.getMinimum();
        progressBar.setValue( ( int ) (progressBar.getMinimum() + ( amount * 1.0 * numFiles / files.size())));
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
		numFiles = 0;
		update(0);
		TxInterface fs =gui.getTxInterface();
		fs.open();
		try {
			fs.Init(gui.getFSStatus());
		} catch (Exception e) { e.printStackTrace(); }
		for (FileInfo file: files) {
			file = new FileInfo(file); //Make a separate copy to fill with data
			fs.fillFileData(file);
			File f = new File(destDir.resolve(file.name()).toUri());
			try {
				// if file doesn't exists, then create it
				if (!f.exists()) {
					File parent = f.getParentFile();
					if(!parent.exists() && !parent.mkdirs()){
						throw new IllegalStateException("Couldn't create dir: " + parent);
					}
					f.createNewFile();
				}
				FileOutputStream fop = new FileOutputStream(f);
				fop.write(file.data());
				fop.flush();
				fop.close();
			} catch (Exception e) {e.printStackTrace(); }
			update(1);
		}
		fs.close();
		return true;
	}

}
