package deviation;

import de.ailis.usb4java.libusb.ConfigDescriptor;
import de.ailis.usb4java.libusb.Device;
import de.ailis.usb4java.libusb.DeviceHandle;
import de.ailis.usb4java.libusb.InterfaceDescriptor;
import de.ailis.usb4java.libusb.LibUsb;

public class DfuInterface {
    public static final int MAX_DESC_STR_LEN = 253;
    private DfuMemory memory;
    private int configuration_number;
    private int interface_number;
    private int altsetting_number;
    private boolean DFU_IFF_DFU;
    
    public DfuInterface(Device dev, InterfaceDescriptor intf, ConfigDescriptor cfg) {
        this.configuration_number = 0xff & (int)cfg.bConfigurationValue();
        this.interface_number = 0xff & (int)intf.bInterfaceNumber();
        this.altsetting_number = 0xff & (int)intf.bAlternateSetting();
        this.DFU_IFF_DFU = intf.bInterfaceProtocol() == 2;
        DeviceHandle handle = new DeviceHandle();
        if(LibUsb.open(dev, handle) == 0) {
            StringBuffer name = new StringBuffer();
            LibUsb.getStringDescriptorAscii(handle, intf.iInterface(), name, MAX_DESC_STR_LEN);
            LibUsb.close(handle);
            memory = new DfuMemory(name.toString());
        } else {
            memory = new DfuMemory(null);
        }
    }
    public DfuMemory Memory() { return memory; }
    public int bConfigurationValue() { return configuration_number; }
    public int bInterfaceNumber() { return interface_number; }
    public int bAlternateSetting() { return altsetting_number; }
    public boolean DFU_IFF_DFU() { return DFU_IFF_DFU; }

}
