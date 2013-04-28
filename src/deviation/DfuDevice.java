
import de.ailis.usb4java.libusb.*;

public class DfuDevice
    {
        private Device dev;
        private DeviceHandle handle;
        private int configuration_number;
        private int interface_number;
        private int altsetting_number;
        private boolean DFU_IFF_DFU;

        public DfuDevice(Device dev, InterfaceDescriptor intf, ConfigDescriptor cfg) {
            this.handle = null;
            this.dev = dev;
            this.configuration_number = 0xff & (int)cfg.bConfigurationValue();
            this.interface_number = 0xff & (int)intf.bInterfaceNumber();
            this.altsetting_number = 0xff & (int)intf.bAlternateSetting();
            this.DFU_IFF_DFU = intf.bInterfaceProtocol() == 2;
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
        public int bConfigurationValue() { return configuration_number; }
        public int bInterfaceNumber() { return interface_number; }
        public int bAlternateSetting() { return altsetting_number; }
        public boolean DFU_IFF_DFU() { return DFU_IFF_DFU; }
        public int open() {
            if (handle == null) {
                handle = new DeviceHandle();
                return LibUsb.open(dev, handle);
            }
            return 0;
        }
        public void close() {
            if (handle != null) {
                LibUsb.close(handle);
            }
        }
        public int claim_and_set()  {
            int ret = LibUsb.claimInterface(handle, interface_number);
            if (ret < 0) {
                return ret;
            }
            return LibUsb.setInterfaceAltSetting(handle, interface_number, altsetting_number);
        }
/*
        public byte bConfigurationValue() { return intf.getUsbConfiguration().getUsbConfigurationDescriptor().bConfigurationValue();}
*/
    };
