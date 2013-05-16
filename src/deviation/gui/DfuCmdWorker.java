package deviation.gui;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import deviation.*;

class DfuCmdWorker extends SwingWorker<String, Double> implements Progress {
    private final JProgressBar fProgressBar;
    private final JButton fButton;
    private DfuFile dfuFile;
    private List<DfuDevice> devs;
    private MonitorUSB monitor;

    //private final JLabel fLabel;
    public DfuCmdWorker( List<DfuDevice> devs, DfuFile dfuFile, JProgressBar aProgressBar, JButton aButton, MonitorUSB monitor ) {
        fProgressBar = aProgressBar;
        fButton = aButton;
        this.monitor = monitor;
        this.devs = devs;
        this.dfuFile = dfuFile;
        //fLabel = aLabel;
    }

    @Override
    protected String doInBackground() throws Exception {
        DeviationUploader.sendDfuToDevice(devs, dfuFile, this);
        return "Finished";
    }

    @Override
    protected void process( List<Double> aDoubles ) {
        //update the percentage of the progress bar that is done
        int amount = fProgressBar.getMaximum() - fProgressBar.getMinimum();
        fProgressBar.setValue( ( int ) (fProgressBar.getMinimum() + ( amount * aDoubles.get( aDoubles.size() - 1 ))) );
    }

    @Override
    protected void done() {
        try {
            if (get().equals("Finished")) {
                System.out.println("Completed transfer");
                fProgressBar.setValue(100);
            }
        } catch (Exception e) {
            System.out.println("Completed failed");
            fProgressBar.setValue(0);
        }
        monitor.ReleaseDevices();
        fButton.setText("Send");
        /*      
      try {
        fLabel.setText( get() );
      } catch ( InterruptedException e ) {
        e.printStackTrace();
      } catch ( ExecutionException e ) {
        e.printStackTrace();
      }
         */
    }
    //Progress Interface
    public void update(Double status) {
        publish(status);
    }
    public boolean cancelled() {
        return isCancelled();
    }
}
