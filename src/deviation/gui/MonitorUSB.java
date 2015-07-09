package deviation.gui;

import java.util.List;

import javax.swing.SwingWorker;

import de.ailis.usb4java.libusb.DeviceList;
import de.ailis.usb4java.libusb.LibUsb;
import deviation.Dfu;
import deviation.DfuDevice;
import deviation.TxInfo;

//This will execute in the Swing Event thread
public class MonitorUSB extends SwingWorker<String, DfuDevice> {

    String strObject;
    DeviceList devices;
    DfuDevice dfuDev;
    DeviationUploadGUI gui;
    int pollTime;
    private int vendorId;
    private int productId;

    public MonitorUSB(DeviationUploadGUI gui, int pollTime, int vid, int pid){
        devices = null;
        dfuDev = null;
        this.gui = gui;
    	this.pollTime = pollTime;
    	vendorId = vid;
    	productId = pid;
    }

    public void process(List<DfuDevice> dfuDevs) {
		//Must be done without holding the lock and executed in the EventDispatchThread
    	//Only the last event is of interest
		gui.RefreshDevices(dfuDevs.get(dfuDevs.size()-1));    	
    }
    
    public String doInBackground() {
        //Detect Plug/Unplug events
    	while(true) {
    		boolean state_changed = false;
    		if (DfuDevice.tryLock()) {
    			try {
    				DeviceList devices = new DeviceList();
    				LibUsb.getDeviceList(null, devices);
    				List<DfuDevice> devs = Dfu.findDevices(devices);
    				DfuDevice dev = null;
    				for(DfuDevice dev1: devs) {
    					if (dev1.idVendor() == vendorId && dev1.idProduct() == productId) {
    						dev = dev1;
    						break;
    					}
    				}
    				if (dev == null) {
    					if (dfuDev != null) {
    						LibUsb.freeDeviceList(this.devices, true);
    						dfuDev = null;
    						System.out.println("Unplug detected");
    						state_changed = true;
    						//Signal disconnect
    					}
    				} else {
    					if(dfuDev == null || !dfuDev.equals(dev)) {
    						if (dfuDev != null) {
    							LibUsb.freeDeviceList(this.devices, true);
    						}
    						dfuDev = dev;
    						dfuDev.setTxInfo(TxInfo.getTxInfo(dfuDev));
    						this.devices = devices;
    						System.out.println("Hotplug detected");
    						state_changed = true;
    						//Signal connect/change
    					} else {
    						LibUsb.freeDeviceList(devices, true);
    					}
    				}
    			} finally {
    				DfuDevice.unLock();
    			}
    			if (state_changed) {
    				publish(dfuDev);
    			}
    		}
    		try {
    			Thread.sleep(pollTime);
    		} catch(Exception e) { e.printStackTrace();}
    	}
    }
}
