package deviation;

import java.util.*;

import de.ailis.usb4java.libusb.*;

/* Note: This class is not functioning properly because usb4java does not properly return extra() */
public class DfuFuncDescriptor
    {
        public static final byte USB_DT_DFU =  0x21;
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
                //byte resbuf[] = find_descriptor(extra, USB_DT_DFU, 0);
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
