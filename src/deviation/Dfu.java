import java.io.*;
import java.util.*;
import javax.usb.*;
import javax.usb.util.*;

public class Dfu
{

    public static UsbDevice findDevice(UsbHub hub, short vendorId, short productId)
    {
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            if (device.isUsbHub())
            {
                device = findDevice((UsbHub) device, vendorId, productId);
                if (device != null) return device;
                continue;
            }
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            for (UsbConfiguration cfg : (List<UsbConfiguration>)device.getUsbConfigurations()) {
                UsbConfigurationDescriptor cfg_desc = cfg.getUsbConfigurationDescriptor();
                for (UsbInterface uif : (List<UsbInterface>) cfg.getUsbInterfaces()) {
                    for (UsbInterface uif2 : (List<UsbInterface>) uif.getSettings()) {
                        UsbInterfaceDescriptor intf = uif2.getUsbInterfaceDescriptor();
                        System.out.format("%04x:%04x -> %02x/%02x %02x/%02x%n",
                                          desc.idVendor() & 0xffff,
                                          desc.idProduct() & 0xffff,
                                          intf.bInterfaceNumber(),
                                          intf.bAlternateSetting(),
                                          intf.bInterfaceClass(),
                                          intf.bInterfaceSubClass());
                        if (intf.bInterfaceClass() ==0xfe && intf.bInterfaceSubClass() == 1) {
                           return device;
                        }
                    }
                }
            //if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
            }
        }
        return null;
    }
    public static void main(String[] args) throws UsbException
    {
        UsbServices services = UsbHostManager.getUsbServices();
        UsbHub rootHub = services.getRootUsbHub();
        findDevice(rootHub, (short)0, (short)0);
    }
}
