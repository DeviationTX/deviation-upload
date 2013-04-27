import java.io.*;
import java.util.*;
import javax.usb.*;
import javax.usb.util.*;

public class Dfu
{
    public static final byte DFU_DETACH    = 0;
    public static final byte DFU_DNLOAD    = 1;
    public static final byte DFU_UPLOAD    = 2;
    public static final byte DFU_GETSTATUS = 3;
    public static final byte DFU_CLRSTATUS = 4;
    public static final byte DFU_GETSTATE  = 5;
    public static final byte DFU_ABORT     = 6;

    private static short wTransaction = 0;

    public static class DfuDevice
    {
        private UsbDevice dev;
        private UsbInterface intf;
        public DfuDevice(UsbDevice _device, UsbInterface _intf) {
            dev = _device;
            intf = _intf;
        }
        public UsbDevice device() { return dev; }
        public UsbInterface Interface() { return intf; }
        public byte bInterfaceNumber() { return intf.getUsbInterfaceDescriptor().bInterfaceNumber();}
        public byte bAlternateSetting() { return intf.getUsbInterfaceDescriptor().bAlternateSetting();}
        public byte iInterface() { return intf.getUsbInterfaceDescriptor().iInterface();}
        public byte bConfigurationValue() { return intf.getUsbConfiguration().getUsbConfigurationDescriptor().bConfigurationValue();}
        public int idVendor() { return 0xffff & (int)dev.getUsbDeviceDescriptor().idVendor(); }
        public int idProduct() { return 0xffff & (int)dev.getUsbDeviceDescriptor().idProduct(); }
        public boolean DFU_IFF_DFU() { return intf.getUsbInterfaceDescriptor().bInterfaceProtocol() == 2;}
    };

    public static List<DfuDevice> dfu_find_devices(UsbHub hub)
    {
        List<DfuDevice> devices= new ArrayList<DfuDevice>();
        for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices())
        {
            if (device.isUsbHub())
            {
                devices.addAll(dfu_find_devices((UsbHub)device));
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
                        if (intf.bInterfaceClass() == (byte)0xfe && intf.bInterfaceSubClass() == 0x01) {
                           devices.add(new DfuDevice(device, uif2));
                        }
                    }
                }
            //if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
            }
        }
        return devices;
    }

    public static void dfu_detach(DfuDevice dev, short wTimeout) {
        UsbDevice device = dev.device();
        short wInterface = dev.bInterfaceNumber();
        UsbControlIrp irp = device.createUsbControlIrp(
                            (byte)(UsbConst.ENDPOINT_DIRECTION_OUT
                                 | UsbConst.REQUESTTYPE_TYPE_CLASS
                                 | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                            DFU_DETACH,
                            wTimeout,
                            wInterface);
        irp.setData(null, 0, 0);
        try {
            device.syncSubmit(irp);
        } catch (UsbException ex) {
            System.out.format("Failed to detach%n");
        }
    }

    public static int dfu_download(DfuDevice dev, int length, byte data[])
    {
        UsbDevice device = dev.device();
        short wInterface = dev.bInterfaceNumber();
        UsbControlIrp irp = device.createUsbControlIrp(
                            (byte)(UsbConst.ENDPOINT_DIRECTION_OUT
                                 | UsbConst.REQUESTTYPE_TYPE_CLASS
                                 | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                            DFU_DNLOAD,
                            wTransaction,
                            wInterface);
        irp.setData(data, 0, length);
        wTransaction++;
        try {
            device.syncSubmit(irp);
        } catch (UsbException ex) {
            System.out.format("Failed to download%n");
            return 0;
        }
        return 1;
    }
    public static byte[] dfu_upload(DfuDevice dev, int length)
    {
        UsbDevice device = dev.device();
        short wInterface = dev.bInterfaceNumber();
        UsbControlIrp irp = device.createUsbControlIrp(
                            (byte)(UsbConst.ENDPOINT_DIRECTION_IN
                                 | UsbConst.REQUESTTYPE_TYPE_CLASS
                                 | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                            DFU_UPLOAD,
                            wTransaction,
                            wInterface);
        irp.setLength(length);
        wTransaction++;
        try {
            device.syncSubmit(irp);
        } catch (UsbException ex) {
            System.out.format("Failed to upload%n");
            return null;
        }
        return irp.getData();
    }

    public static DfuStatus dfu_get_status(DfuDevice dev) {
        UsbDevice device = dev.device();
        short wInterface = dev.bInterfaceNumber();
        UsbControlIrp irp = device.createUsbControlIrp(
                            (byte)(UsbConst.ENDPOINT_DIRECTION_IN
                                 | UsbConst.REQUESTTYPE_TYPE_CLASS
                                 | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                            DFU_GETSTATUS,
                            (short)0,
                            wInterface);
        irp.setLength(6);
        try {
            device.syncSubmit(irp);
            if (irp.getActualLength() == 6) {
                return new DfuStatus(irp.getData());
            }
        } catch (UsbException ex) {
            System.out.format("Failed to detach%n");
        }
        return new DfuStatus(null);
    }

    public static void dfu_clear_status(DfuDevice dev) {
        UsbDevice device = dev.device();
        short wInterface = dev.bInterfaceNumber();
        UsbControlIrp irp = device.createUsbControlIrp(
                            (byte)(UsbConst.ENDPOINT_DIRECTION_OUT
                                 | UsbConst.REQUESTTYPE_TYPE_CLASS
                                 | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                            DFU_CLRSTATUS,
                            (short)0,
                            wInterface);
        irp.setData(null, 0, 0);
        try {
            device.syncSubmit(irp);
        } catch (UsbException ex) {
            System.out.format("Failed to clear status%n");
        }
    }

    public static int dfu_get_state(DfuDevice dev) {
        UsbDevice device = dev.device();
        short wInterface = dev.bInterfaceNumber();
        UsbControlIrp irp = device.createUsbControlIrp(
                            (byte)(UsbConst.ENDPOINT_DIRECTION_IN
                                 | UsbConst.REQUESTTYPE_TYPE_CLASS
                                 | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                            DFU_GETSTATE,
                            (short)0,
                            wInterface);
        irp.setLength(1);
        try {
            device.syncSubmit(irp);
            if (irp.getActualLength() == 1) {
                return irp.getData()[0];
            }
        } catch (UsbException ex) {
            System.out.format("Failed to get state%n");
        }
        return -1;
    }

    public static void dfu_abort(DfuDevice dev) {
        UsbDevice device = dev.device();
        short wInterface = dev.bInterfaceNumber();
        UsbControlIrp irp = device.createUsbControlIrp(
                            (byte)(UsbConst.ENDPOINT_DIRECTION_OUT
                                 | UsbConst.REQUESTTYPE_TYPE_CLASS
                                 | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE),
                            DFU_ABORT,
                            (short)0,
                            wInterface);
        irp.setData(null, 0, 0);
        try {
            device.syncSubmit(irp);
        } catch (UsbException ex) {
            System.out.format("Failed to abort%n");
        }
    }

    public static void main(String[] args) throws UsbException
    {
        UsbServices services = UsbHostManager.getUsbServices();
        UsbHub rootHub = services.getRootUsbHub();
        List<DfuDevice> devs = dfu_find_devices(rootHub);
        for(DfuDevice dev : devs) {
            String str;
            String str1;
            try {
                System.out.format("iInterface: %d%n", dev.iInterface());
                str = dev.device().getString(dev.iInterface());
            } catch (UsbException e) {
                str = "USB Exception: " + e;
            } catch (UsbDisconnectedException e) {
                str = "USB Disconnect: " + e;
            } catch (java.io.UnsupportedEncodingException e) {
                str = "Bad encoding: " + e;
            }
            try {
                str1 = dev.Interface().getUsbConfiguration().getConfigurationString();
            } catch (UsbException e) {
                str1 = "USB Exception: " + e;
            } catch (UsbDisconnectedException e) {
                str1 = "USB Disconnect: " + e;
            } catch (java.io.UnsupportedEncodingException e) {
                str1 = "Bad encoding: " + e;
            }
            System.out.format("Found: %s [%04x:%04x] cfg=%d, intf=%d, alt=%d, name='%s' '%d/%d'%n",
                dev.DFU_IFF_DFU() ? "DFU" : "Runtime",
                dev.idVendor(),
                dev.idProduct(),
                dev.bConfigurationValue(),
                dev.bInterfaceNumber(),
                dev.bAlternateSetting(),
                str, dev.Interface().getUsbConfiguration().getUsbConfigurationDescriptor().wTotalLength(), dev.Interface().getUsbConfiguration().getUsbConfigurationDescriptor().bLength());
        }
    }
}
