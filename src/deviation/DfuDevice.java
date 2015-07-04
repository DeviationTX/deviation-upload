package deviation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import de.ailis.usb4java.libusb.*;

public class DfuDevice
    {
        
        private Device dev;
        private DeviceHandle handle;
        private List<DfuInterface> interfaces;
        private DfuInterface selected_interface;
        private TxInfo txInfo;
        static final protected ReentrantLock lock = new ReentrantLock();

        public DfuDevice(Device dev) {
            this.handle = null;
            this.dev = dev;
            interfaces = new ArrayList<DfuInterface>();
            this.selected_interface = null;
            this.txInfo = null;
        }
        public void AddInterface(InterfaceDescriptor intf, ConfigDescriptor cfg) {
            interfaces.add(new DfuInterface(dev, intf, cfg));    
       }
        public Device Device() { return dev; }
        public DeviceHandle Handle() { return handle; }
        public int idVendor() {
            DeviceDescriptor ddesc = new DeviceDescriptor();
            LibUsb.getDeviceDescriptor(dev, ddesc);
            return 0xffff & (int)(ddesc.idVendor());
        }
        public int idProduct() {
            DeviceDescriptor ddesc = new DeviceDescriptor();
            LibUsb.getDeviceDescriptor(dev, ddesc);
            return 0xffff & (int)(ddesc.idProduct());
        }
        
        public DfuMemory Memory() { return selected_interface == null ? null :  selected_interface.Memory(); }
        public int bConfigurationValue() { return selected_interface == null ? 0 : selected_interface.bConfigurationValue(); }
        public int bInterfaceNumber() { return selected_interface == null ? 0 : selected_interface.bInterfaceNumber(); }
        public int bAlternateSetting() { return selected_interface == null ? 0 : selected_interface.bAlternateSetting(); }
        public boolean DFU_IFF_DFU() { return selected_interface == null ? false : selected_interface.DFU_IFF_DFU(); }
        
        public int open() {
        	lock.lock();
            if (handle == null) {
                handle = new DeviceHandle();
                int result = LibUsb.open(dev, handle);
                if (result != LibUsb.SUCCESS){
                	lock.unlock();
                }
                return result;
            }
            return 0;
        }
        public void close() {
        	if (lock.getHoldCount() == 1 && handle != null) {
        		LibUsb.close(handle);
                handle = null;
            }
        	lock.unlock();
        }
        public static boolean tryLock() {
        	return lock.tryLock();
        }
        public static void unLock() {
        	lock.unlock();
        }
        public int claim_and_set()  {
            if (selected_interface == null) {
                return -1;
            }
            int ret = LibUsb.claimInterface(handle, selected_interface.bInterfaceNumber());
            if (ret < 0) {
                return ret;
            }
            return LibUsb.setInterfaceAltSetting(handle, selected_interface.bInterfaceNumber(), selected_interface.bAlternateSetting());
        }
        public boolean SelectInterface(DfuInterface iface)
        {
            if(interfaces.contains(iface)) {
                selected_interface = iface;
                return true;
            }
            return false;
        }
        public DfuInterface SelectInterfaceByAddr(int address, Integer alt)
        {
        	for (DfuInterface iface : interfaces) {
        		if ((alt == null || alt == iface.bAlternateSetting()) && iface.Memory().find(address) != null) {
        			selected_interface = iface;
        			return iface;
        		}
        	}
        	return null;
        }
        public DfuInterface SelectInterfaceByAddr(int address) { return SelectInterfaceByAddr(address, null); }
        public List<DfuInterface>Interfaces() { return interfaces;}
        public String GetId() {
            String id = String.valueOf(idVendor()) + ":"
                    + String.valueOf(idProduct());
            for (DfuInterface i : interfaces) {
                id += ":";
                if (i.Memory() != null) {
                    id += i.Memory().name();
                }
            }
            return id;
        }
        
        @Override
        public boolean equals(Object o) {
            if(o instanceof DfuDevice) {
                DfuDevice dd = (DfuDevice)o;
                if (GetId().equals(dd.GetId())) {
                    return true;
                }
            }
            return false;
        }
        public void setTxInfo(TxInfo txInfo) {
        	this.txInfo = txInfo;
        	// If this transmitter incorrectly reports the memory interface, override it
        	txInfo.type().overrideSector(interfaces);
        }
        public TxInfo getTxInfo() { return txInfo; }
/*
        public byte bConfigurationValue() { return intf.getUsbConfiguration().getUsbConfigurationDescriptor().bConfigurationValue();}
*/
    };
