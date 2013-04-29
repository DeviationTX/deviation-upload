import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

import de.ailis.usb4java.libusb.*;

public class DeviationUploader
{
    public static final int MAX_DESC_STR_LEN = 253;
/*
    public static String get_alt_name(DfuDevice dev)
    {
        int alt_name_str_idx;
        int ret;
        StringBuffer name = new StringBuffer();
        ConfigDescriptor cfg = new ConfigDescriptor();

        if(LibUsb.getConfigDescriptorByValue(dev.Device(), (byte)dev.bConfigurationValue(), cfg) != 0) {
            return null;
        }
        alt_name_str_idx = cfg.iface()[dev.bInterfaceNumber()].altsetting()[dev.bAlternateSetting()].iInterface();
        ret = -1;
        if (alt_name_str_idx != 0) {
            DeviceHandle handle = new DeviceHandle();
            if(LibUsb.open(dev.Device(), handle) != 0) {
                return null;
            }
            LibUsb.getStringDescriptorAscii(handle, alt_name_str_idx, name, MAX_DESC_STR_LEN);
            LibUsb.close(handle);
        }
        LibUsb.freeConfigDescriptor(cfg);
        return name.toString();
    }
*/

    public static void main(String[] args)
    {
        DeviceList devices = new DeviceList();
        LibUsb.init(null);
        LibUsb.getDeviceList(null, devices);
        List<DfuDevice> devs = Dfu.findDevices(devices);
        for(DfuDevice dev : devs) {
            System.out.format("Found: %s [%05x:%04x] cfg=%d, intf=%d, alt=%d, name='%s'%n",
                dev.DFU_IFF_DFU() ? "DFU" : "Runtime",
                dev.idVendor(),
                dev.idProduct(),
                dev.bConfigurationValue(),
                dev.bInterfaceNumber(),
                dev.bAlternateSetting(),
                dev.Memory().name());
            //DfuFuncDescriptor desc = new DfuFuncDescriptor(dev);
        }
        int idx = 0;
        for (DfuDevice dev : devs) {
            dev.open();
            dev.claim_and_set();
            Dfu.setIdle(dev);
            byte [] data = Dfu.FetchFromDevice(dev, 0x08000400, 0x40);

            try{
                File file = new File("output" + Integer.toString(idx) + ".txt");
                FileOutputStream fop = new FileOutputStream(file);
                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }
                fop.write(data);
                fop.flush();
                fop.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            dev.close();
            idx++;
        }
        LibUsb.freeDeviceList(devices, true);
        LibUsb.exit(null);
    }
}
