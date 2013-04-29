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

    public static final byte DFUSE_SET_ADDRESS    = 0;
    public static final byte DFUSE_ERASE_PAGE     = 1;
    public static final byte DFUSE_MASS_ERASE     = 2;
    public static final byte DFUSE_READ_UNPROTECT = 3;

    public static byte dfu_timeout   = 0;
    public static final int DFU_TIMEOUT = 5000;

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

    public static int detach(DfuDevice dev, short timeout) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(6);
        return LibUsb.controlTransfer( dev.Handle(),
            /* bmRequestType */ (LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE),
            /* bRequest      */ DFU_DETACH,
            /* wValue        */ timeout,
            /* wIndex        */ dev.bInterfaceNumber(),
            /* Data          */ buffer,
                                dfu_timeout );
    }

    public static int dfuseSpecialCommand(DfuDevice dev, int address, int command) {
        byte [] buf = new byte[5];
        int ret;
        DfuStatus status;

        if (command == DFUSE_ERASE_PAGE) {
            Sector sector = dev.Memory().find(address);
            if (sector == null || ! sector.erasable()) {
                System.out.format("Error: Page at 0x%08x can not be erased%n", address);
                return -1;
            }
            System.out.format("Erasing page size %i at address 0x%08x, page "
                           + "starting at 0x%08x%n", sector.size(), address,
                           address & ~(sector.size() - 1));
            buf[0] = 0x41;  // Erase command
        } else if (command == DFUSE_SET_ADDRESS) {
            System.out.format("  Setting address pointer to 0x%08x%n", address);
                buf[0] = 0x21;  /* Set Address Pointer command */
        } else {
            System.out.format("Error: Non-supported special command %d%n", command);
            return -1;
        }
        buf[1] = (byte)(address & 0xff);
        buf[2] = (byte)((address >> 8) & 0xff);
        buf[3] = (byte)((address >> 16) & 0xff);
        buf[4] = (byte)((address >> 24) & 0xff);

        ret = dfuseDownload(dev, buf, 0);
        if (ret < 0) {
            System.out.println("Error during special command download");
            return -1;
        }
        // 1st getStatus
        status = getStatus(dev);
        if (status.bState != DfuStatus.STATE_DFU_DOWNLOAD_BUSY) {
            System.out.println("Error: Wrong state after command download");
            return -1;
        }
        // wait while command is executed
        System.out.format("   Poll timeout %i ms%n", status.bwPollTimeout);
        try {
            Thread.sleep(status.bwPollTimeout);
        } catch (InterruptedException e) {} //Don't care if we're interrupted
        // 2nd getStatus
        status = getStatus(dev);
        if (status.bStatus != DfuStatus.DFU_STATUS_OK) {
            System.out.format("Error during second get_status%n"
                   + "state(%u) = %s, status(%u) = %s%n",
                   status.bState, status.stateToString(),
                   status.bStatus, status.statusToString());
            return -1;
        }
        try {
            Thread.sleep(status.bwPollTimeout);
        } catch (InterruptedException e) {} //Don't care if we're interrupted

        ret = abort(dev);
        if (ret < 0) {
            System.out.println("Error sending dfu abort request");
            return -1;
        }

        // 3rd getStatus
        status =getStatus(dev);
        if (status.bState != DfuStatus.STATE_DFU_IDLE) {
                System.out.println("Error: Failed to enter idle state on abort%n");
                return -1;
        }
        try {
            Thread.sleep(status.bwPollTimeout);
        } catch (InterruptedException e) {} //Don't care if we're interrupted
        return 0;
    }

    public static int dfuseUpload(DfuDevice dev, byte [] data, int transaction)
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(data);
        int status = LibUsb.controlTransfer(dev.Handle(),
                 /* bmRequestType */     LibUsb.ENDPOINT_IN |
                                         LibUsb.REQUEST_TYPE_CLASS |
                                         LibUsb.RECIPIENT_INTERFACE,
                 /* bRequest      */     DFU_UPLOAD,
                 /* wValue        */     transaction,
                 /* wIndex        */     dev.bInterfaceNumber(),
                 /* Data          */     buffer,
                                         DFU_TIMEOUT);
        if (status < 0) {
            System.out.format("dfuseUpload: libusb_control_msg returned %d\n", status);
        }               
        return status;  
    }
    public static int dfuseDownload(DfuDevice dev, byte [] data, int transaction)
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);

        int status = LibUsb.controlTransfer(dev.Handle(),
                 /* bmRequestType */     LibUsb.ENDPOINT_OUT |
                                         LibUsb.REQUEST_TYPE_CLASS |
                                         LibUsb.RECIPIENT_INTERFACE,
                 /* bRequest      */     DFU_DNLOAD,
                 /* wValue        */     transaction,
                 /* wIndex        */     dev.bInterfaceNumber(),
                 /* Data          */     buffer,
                                         DFU_TIMEOUT);
        buffer.get(data);
        if (status < 0) {
            System.out.format("dfuseDownload: libusb_control_msg returned %d\n", status);
        }
        return status;
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
    public static byte [] FetchFromDevice(DfuDevice dev, int address, int requested_length)
    {
        int xfer_size = 1024;
        int total_bytes = 0;
        int transaction = 2;
        int ret;

        if (dfuseSpecialCommand(dev, address, DFUSE_SET_ADDRESS) != 0) {
            return null;
        }
        /* Boot loader decides the start address, unknown to us */
        /* Use a short length to lower risk of running out of bounds */

        System.out.format("bytes_per_hash=%d\n", xfer_size);
        System.out.println("Starting upload");

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        while (true) {
                int rc, write_rc;

                /* last chunk can be smaller than original xfer_size */
                if (requested_length - total_bytes < xfer_size)
                        xfer_size = requested_length - total_bytes;

                byte [] buf = new byte[xfer_size];
                rc = dfuseUpload(dev, buf, transaction++);
                total_bytes += rc;
                data.write(buf, 0, rc);
                if (rc < 0) {
                        ret = rc;
                        break;
                }
                if (rc < xfer_size || total_bytes >= requested_length) {
                        /* last block, return successfully */
                        ret = total_bytes;
                        break;
                }
        }
        return data.toByteArray();
    }
}
