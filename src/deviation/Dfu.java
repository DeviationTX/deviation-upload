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

    public static void dfu_detach(UsbDevice device, short wInterface, short wTimeout) {
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

    public static int dfu_download(UsbDevice device, short wInterface, int length, byte data[])
    {
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
    public static byte[] dfu_upload(UsbDevice device, short wInterface, int length)
    {
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

    public static DfuStatus dfu_get_status(UsbDevice device, short wInterface) {
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

    public static void dfu_clear_status(UsbDevice device, short wInterface) {
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

    public static int dfu_get_state(UsbDevice device, short wInterface) {
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

    public static void dfu_abort(UsbDevice device, short wInterface) {
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
        findDevice(rootHub, (short)0, (short)0);
    }
}
