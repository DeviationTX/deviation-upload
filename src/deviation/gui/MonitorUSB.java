package deviation.gui;

import java.util.List;
import java.util.TimerTask;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.ailis.usb4java.libusb.DeviceList;
import de.ailis.usb4java.libusb.LibUsb;
import deviation.Dfu;
import deviation.DfuDevice;

public class MonitorUSB extends TimerTask{

    String strObject;
    Lock lock;
    DeviceList devices;
    List<DfuDevice> dfuDevs;
    DeviationUploadGUI gui;

    public MonitorUSB(DeviationUploadGUI gui){
        devices = null;
        lock = new ReentrantLock();
        dfuDevs = null;
        this.gui = gui;
    }

    public void run(){
        //Detect Plug/Unplug events
        boolean state_changed = false;
        if (lock.tryLock()) {
            try {
                DeviceList devices = new DeviceList();
                LibUsb.getDeviceList(null, devices);
                List<DfuDevice> devs = Dfu.findDevices(devices);
                if (devs.isEmpty()) {
                    if (dfuDevs != null) {
                        LibUsb.freeDeviceList(this.devices, true);
                        dfuDevs = null;
                        System.out.println("Unplug detected");
                        state_changed = true;
                        //Signal disconnect
                    }
                } else {
                    if(dfuDevs == null || dfuDevs.size() != devs.size() || ! dfuDevs.containsAll(devs)) {
                        if (dfuDevs != null) {
                            LibUsb.freeDeviceList(this.devices, true);
                        }
                        dfuDevs = devs;
                        this.devices = devices;
                        System.out.println("Hotplug detected");
                        state_changed = true;
                        //Signal connect/change
                    } else {
                        LibUsb.freeDeviceList(devices, true);
                    }
                }
            } finally {
                if (state_changed) {
                    gui.RefreshDevices(dfuDevs);
                }
                if (! state_changed || dfuDevs == null) {
                    lock.unlock();
                }
            }
            if (state_changed) {
                //Must be done without holding the lock
                gui.RefreshDevices();
            }
        }
    }
    public List<DfuDevice> GetDevices() {
        // This will take the lock if successful
        lock.lock();
        if (dfuDevs == null) {
            lock.unlock();
            return null;
        }
        return dfuDevs;
    }
    public void ReleaseDevices() {
        lock.unlock();
    }
}
