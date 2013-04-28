import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

import de.ailis.usb4java.libusb.*;

public class DeviationUploader
{
    public static final int MAX_DESC_STR_LEN = 253;

    public static class DfuFuncDescriptor
    {
        public static final byte USB_DT_DFU =  0x21;
        public DfuFuncDescriptor(DfuDevice dev)
        {
            ConfigDescriptor cfg = new ConfigDescriptor();
            if(LibUsb.getConfigDescriptorByValue(dev.Device(), (byte)dev.bConfigurationValue(), cfg) != 0) {
                return;
            }

            /* Extra descriptors can be shared between alternate settings but
             * libusb may attach them to one setting. Therefore go through all.
             * Note that desc_index is per alternate setting, hits will not be
             * counted from one to another */
            for (InterfaceDescriptor desc : cfg.iface()[dev.bInterfaceNumber()].altsetting()) {
                //byte extra[] = new byte[desc.extraLength()];
                //desc.extra().get(extra, 0, desc.extraLength());
                byte extra[] = new byte[desc.extra().remaining()];
                desc.extra().get(extra, 0, desc.extra().remaining());
                //byte resbuf[] = Dfu.find_descriptor(extra, USB_DT_DFU, 0);
                if(extra != null) {
                    for (int i = 0; i < extra.length; i++) {
                        System.out.format("%02x ", extra[i]);
                    }
                    System.out.format("%n");
                }
            }
            LibUsb.freeConfigDescriptor(cfg);
        }
    };
    public static byte [] find_descriptor(byte [] desc_list, int desc_type, int desc_index)
    {
        int p = 0;
        int hit = 0;

        while (p + 1 < desc_list.length) {
            int desclen;

            desclen = 0xff & (int)desc_list[p];
            if (desclen == 0) {
                        System.out.println("Error: Invalid descriptor list\n");
                        return null;
                }
                if (desc_list[p + 1] == desc_type && hit++ == desc_index) {
                        if (p + desclen > desc_list.length)
                                desclen = desc_list.length - p;
                        byte result[] = Arrays.copyOfRange(desc_list, p, p + desclen);
                        return result;
                }
                p += (int) desc_list[p];
        }
        return null;
    }

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
                get_alt_name(dev));
            DfuFuncDescriptor desc = new DfuFuncDescriptor(dev);
        }
        DfuDevice dev = devs.get(0);
        dev.open();
        dev.claim_and_set();
        Dfu.setIdle(dev);
        dev.close();
        LibUsb.freeDeviceList(devices, true);
        LibUsb.exit(null);
    }
}
