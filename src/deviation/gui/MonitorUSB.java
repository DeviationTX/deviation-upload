package deviation.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import de.ailis.usb4java.libusb.DeviceList;
import de.ailis.usb4java.libusb.LibUsb;
import deviation.Dfu;
import deviation.DfuDevice;

//This will execute in the Swing Event thread
public class MonitorUSB implements ActionListener{

    String strObject;
    DeviceList devices;
    DfuDevice dfuDev;
    DeviationUploadGUI gui;

    public MonitorUSB(DeviationUploadGUI gui){
        devices = null;
        dfuDev = null;
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent evt) {
        //Detect Plug/Unplug events
        boolean state_changed = false;
        if (DfuDevice.tryLock()) {
            try {
                DeviceList devices = new DeviceList();
                LibUsb.getDeviceList(null, devices);
                DfuDevice dev = Dfu.findFirstDevice(devices);
                if (dev != null) {
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
                //Must be done without holding the lock
                gui.RefreshDevices(dfuDev);
            }
        }
    }
}
