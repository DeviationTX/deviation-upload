import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

import de.ailis.usb4java.libusb.*;

public final class Dfu
{
    public static final byte DFU_DETACH    = 0;
    public static final byte DFU_DNLOAD    = 1;
    public static final byte DFU_UPLOAD    = 2;
    public static final byte DFU_GETSTATUS = 3;
    public static final byte DFU_CLRSTATUS = 4;
    public static final byte DFU_GETSTATE  = 5;
    public static final byte DFU_ABORT     = 6;

    public static byte dfu_timeout   = 0;

    private static short wTransaction = 0;

    public static List<DfuDevice> findInterfaces(Device device)
    {
        List<DfuDevice> devices= new ArrayList<DfuDevice>();
        DeviceDescriptor desc = new DeviceDescriptor();
        if (LibUsb.getDeviceDescriptor(device, desc) != 0) {
            return devices;
        }
        for (int cfg_idx = 0; cfg_idx < desc.bNumConfigurations(); cfg_idx++) {
            ConfigDescriptor cfg = new ConfigDescriptor();
            int rc = LibUsb.getConfigDescriptor(device, cfg_idx, cfg);
            if (rc != 0) {
                continue;
            }
            for (Interface uif : cfg.iface()) {
                for (InterfaceDescriptor intf : uif.altsetting()) {
                    System.out.format("%04x:%04x -> %02x/%02x %02x/%02x%n",
                                      desc.idVendor() & 0xffff,
                                      desc.idProduct() & 0xffff,
                                      intf.bInterfaceNumber(),
                                      intf.bAlternateSetting(),
                                      intf.bInterfaceClass(),
                                      intf.bInterfaceSubClass());
                    if (intf.bInterfaceClass() == (byte)0xfe && intf.bInterfaceSubClass() == 0x01) {
                       devices.add(new DfuDevice(device, intf, cfg));
                    }
                }
            }
            LibUsb.freeConfigDescriptor(cfg);
            //if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device;
        }
        return devices;
    }

    public static List<DfuDevice> findDevices(DeviceList usb_devices)
    {
        List<DfuDevice> devices= new ArrayList<DfuDevice>();
        for (Device device : usb_devices)
        {
            devices.addAll(findInterfaces(device));
        }
        return devices;
    }

    public static int dfu_detach(DfuDevice dev, short timeout) {
        return LibUsb.controlTransfer( dev.Handle(),
            /* bmRequestType */ (LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE),
            /* bRequest      */ DFU_DETACH,
            /* wValue        */ timeout,
            /* wIndex        */ dev.bInterfaceNumber(),
            /* Data          */ null,
                                dfu_timeout );
    }
/*
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
*/
    public static DfuStatus getStatus(DfuDevice dev) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(6);
        int result = LibUsb.controlTransfer( dev.Handle(),
          /* bmRequestType */ LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE,
          /* bRequest      */ DFU_GETSTATUS,
          /* wValue        */ 0,
          /* wIndex        */ dev.bInterfaceNumber(),
          /* Data          */ buffer,
                              dfu_timeout );

        if( 6 == result ) {
            return new DfuStatus(buffer);
        }

        return new DfuStatus(null);
    }
    public static int clearStatus(DfuDevice dev) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(0);
        return LibUsb.controlTransfer( dev.Handle(),
        /* bmRequestType */ LibUsb.ENDPOINT_OUT| LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE,
        /* bRequest      */ DFU_CLRSTATUS,
        /* wValue        */ 0,
        /* wIndex        */ dev.bInterfaceNumber(),
        /* Data          */ buffer,
                            dfu_timeout );

    }

    public static int getState(DfuDevice dev) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1);
        int result = LibUsb.controlTransfer( dev.Handle(),
          /* bmRequestType */ LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE,
          /* bRequest      */ DFU_GETSTATE,
          /* wValue        */ 0,
          /* wIndex        */ dev.bInterfaceNumber(),
          /* Data          */ buffer,
                              dfu_timeout );

        /* Return the error if there is one. */
        if( result < 1 ) {
            return result;
        }

        /* Return the state. */
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return data[0];
    }

    public static int abort(DfuDevice dev) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(0);
        return LibUsb.controlTransfer( dev.Handle(),
        /* bmRequestType */ LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE,
        /* bRequest      */ DFU_ABORT,
        /* wValue        */ 0,
        /* wIndex        */ dev.bInterfaceNumber(),
        /* Data          */ buffer,
                            dfu_timeout );
    }

    public static int setIdle(DfuDevice dev)
    {
        boolean done = false;
        DfuStatus status = new DfuStatus(null);
        while (! done) {
            status = getStatus(dev);
            System.out.format("Determining device status: statue=%s status=%d%n",
                          status.stateToString(), status.bStatus);

            switch (status.bState) {
                case DfuStatus.STATE_APP_IDLE:
                case DfuStatus.STATE_APP_DETACH:
                    System.out.println("Device still in Runtime Mode!");
                    return -1;
                case DfuStatus.STATE_DFU_ERROR:
                    System.out.println("dfuERROR, clearing status");
                    if (clearStatus(dev) < 0) {
                        System.out.println("error clear_status");
                        return -1;
                    }
                    break;
                case DfuStatus.STATE_DFU_DOWNLOAD_IDLE:
                case DfuStatus.STATE_DFU_UPLOAD_IDLE:
                    System.out.println("aborting previous incomplete transfer");
                    if (abort(dev) < 0) {
                        System.out.println("can't send DFU_ABORT");
                        return -1;
                    }
                    break;
                case DfuStatus.STATE_DFU_IDLE:
                    System.out.println("dfuIDLE, continuing");
                    done = true;
                    break;
            }
        }
        if (DfuStatus.DFU_STATUS_OK != status.bStatus ) {
            System.out.format("WARNING: DFU Status: '%s'%n",
                    status.statusToString());
            // Clear our status & try again.
            clearStatus(dev);
            getStatus(dev);
            if (DfuStatus.DFU_STATUS_OK != status.bStatus) {
                System.out.format("Error: %d\n", status.bStatus);
                return -1;
            }
        }
        return 0;
    }
}
