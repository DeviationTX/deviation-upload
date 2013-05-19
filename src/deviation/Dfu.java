package deviation;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

import de.ailis.usb4java.libusb.*;
import deviation.DevoFat.FatStatus;

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

    //private static short wTransaction = 0;

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
            DfuDevice dev = new DfuDevice(device);
            for (Interface uif : cfg.iface()) {
                for (InterfaceDescriptor intf : uif.altsetting()) {
                    /*
                    System.out.format("%04x:%04x -> %02x/%02x %02x/%02x%n",
                                      desc.idVendor() & 0xffff,
                                      desc.idProduct() & 0xffff,
                                      intf.bInterfaceNumber(),
                                      intf.bAlternateSetting(),
                                      intf.bInterfaceClass(),
                                      intf.bInterfaceSubClass());
                    */
                    if (intf.bInterfaceClass() == (byte)0xfe && intf.bInterfaceSubClass() == 0x01) {
                       dev.AddInterface(intf, cfg);
                    }
                }
            }
            LibUsb.freeConfigDescriptor(cfg);
            if (! dev.Interfaces().isEmpty()) {
                devices.add(dev);
            }
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
                System.out.format("Error: Page at 0x%x can not be erased%n", address);
                return -1;
            }
            System.out.format("Erasing page size %d at address 0x%x, page "
                           + "starting at 0x%x%n", sector.size(), address,
                           address & ~(sector.size() - 1));
            buf[0] = 0x41;  // Erase command
        } else if (command == DFUSE_SET_ADDRESS) {
            System.out.format("  Setting address pointer to 0x%x%n", address);
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
        System.out.format("   Poll timeout %d ms%n", status.bwPollTimeout);
        try {
            Thread.sleep(status.bwPollTimeout);
        } catch (InterruptedException e) {} //Don't care if we're interrupted
        // 2nd getStatus
        status = getStatus(dev);
        if (status.bStatus != DfuStatus.DFU_STATUS_OK) {
            System.out.format("Error during second get_status%n"
                   + "state(%d) = %s, status(%d) = %s%n",
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
        int bytes_received = LibUsb.controlTransfer(dev.Handle(),
                 /* bmRequestType */     LibUsb.ENDPOINT_IN |
                                         LibUsb.REQUEST_TYPE_CLASS |
                                         LibUsb.RECIPIENT_INTERFACE,
                 /* bRequest      */     DFU_UPLOAD,
                 /* wValue        */     transaction,
                 /* wIndex        */     dev.bInterfaceNumber(),
                 /* Data          */     buffer,
                                         DFU_TIMEOUT);
        buffer.get(data);
        if (bytes_received < 0) {
            System.out.format("dfuseUpload: libusb_control_msg returned %d\n", bytes_received);
        }               
        return bytes_received;
    }

    public static int dfuseDownload(DfuDevice dev, byte [] data, int transaction)
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(data);

        int bytes_sent = LibUsb.controlTransfer(dev.Handle(),
                 /* bmRequestType */     LibUsb.ENDPOINT_OUT |
                                         LibUsb.REQUEST_TYPE_CLASS |
                                         LibUsb.RECIPIENT_INTERFACE,
                 /* bRequest      */     DFU_DNLOAD,
                 /* wValue        */     transaction,
                 /* wIndex        */     dev.bInterfaceNumber(),
                 /* Data          */     buffer,
                                         DFU_TIMEOUT);
        if (bytes_sent < 0) {
            System.out.format("dfuseDownload: libusb_control_msg returned %d\n", bytes_sent);
            return bytes_sent;
        }
        return bytes_sent;
    }
    public static int dfuseDownloadChunk(DfuDevice dev, byte [] data, int transaction)
    {
        int bytes_sent = dfuseDownload(dev, data, transaction);
        DfuStatus status;
        do {
                status = getStatus(dev);
                try {
                    Thread.sleep(status.bwPollTimeout);
                } catch (InterruptedException e) {} //Don't care if we're interrupted
        } while (status.bState != DfuStatus.STATE_DFU_DOWNLOAD_IDLE &&
                 status.bState != DfuStatus.STATE_DFU_ERROR &&
                 status.bState != DfuStatus.STATE_DFU_MANIFEST);

        if (status.bState == DfuStatus.STATE_DFU_MANIFEST) {
            System.out.println("Transitioning to dfuMANIFEST state");
        }

        if (status.bStatus != DfuStatus.DFU_STATUS_OK) {
                System.out.format("Error: state(%d) = %s, status(%d) = %s%n",
                    status.bState, status.stateToString(),
                    status.bStatus, status.statusToString());
                return -1;
        }

        return bytes_sent;
    }

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
    public static byte [] fetchFromDevice(DfuDevice dev, int address, int requested_length)
    {
        int xfer_size = 1024;
        int total_bytes = 0;
        int transaction = 2;

        setIdle(dev);
        if (dfuseSpecialCommand(dev, address, DFUSE_SET_ADDRESS) != 0) {
            return null;
        }
        /* Boot loader decides the start address, unknown to us */
        /* Use a short length to lower risk of running out of bounds */

        System.out.format("bytes_per_hash=%d\n", xfer_size);
        System.out.println("Starting upload");

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        while (true) {
                int rc;

                /* last chunk can be smaller than original xfer_size */
                if (requested_length - total_bytes < xfer_size)
                        xfer_size = requested_length - total_bytes;

                byte [] buf = new byte[xfer_size];
                rc = dfuseUpload(dev, buf, transaction++);
                total_bytes += rc;
                data.write(buf, 0, rc);
                if (rc < 0) {
                        //ret = rc;
                        break;
                }
                if (rc < xfer_size || total_bytes >= requested_length) {
                        /* last block, return successfully */
                        //ret = total_bytes;
                        break;
                }
        }
        return data.toByteArray();
    }
    public static int sendToDevice(DfuDevice dev, int address, byte[] data,  Progress progress)
    {
        int xfer_size = 1024;
        // ensure the entire data rangeis writeable
        int sector_address = address;
        setIdle(dev);
        while (true) {
            Sector sector = dev.Memory().find(sector_address);
            if (sector == null || ! sector.writable()) {
                System.out.format("Error: No sector found that can be written at address %d%n", sector_address);
                return -1;
            }
            if (sector.end() >= address + data.length) {
                break;
            }
            sector_address = sector.end() + 1;
        }

        // erase and write
        sector_address = address;
        int transaction = 2;
        if (dfuseSpecialCommand(dev, sector_address, DFUSE_SET_ADDRESS) != 0) {
            System.out.format("Error: Write failed to set address: 0x%x%n", sector_address);
            return -1;
        }
        while (true) {
            Sector sector = dev.Memory().find(sector_address);
            for (int i = 0; i < sector.count(); i++) {
                if (progress != null) {
                    progress.update(1.0 * (sector_address - address) / data.length);
                    if (progress.cancelled()) {
                        System.out.format("Cancelled at address 0x%x%n", sector_address);
                        return -1;
                    }
                }
                if (sector.erasable()) {
                    System.out.format("Erasing page: 0x%x%n", sector_address);
                    if (dfuseSpecialCommand(dev, sector_address, DFUSE_ERASE_PAGE) != 0) {
                        System.out.format("Error: Write failed to erase address: 0x%x%n", sector_address);
                        return -1;
                    }
                }
                if (dfuseSpecialCommand(dev, sector_address, DFUSE_SET_ADDRESS) != 0) {
                    System.out.format("Error: Write failed to set address: 0x%x%n", sector_address);
                    return -1;
                }
                transaction = 2;

                int sector_size = sector.size();
                if (address + data.length - sector_address < sector_size) {
                    //Remaining data is less than the sector size
                    sector_size = address + data.length - sector_address;
                }
                int xfer = xfer_size;
                int sector_start = sector.start() + i * sector.size();
                while (sector_address < sector_start + sector_size) {
                    if (xfer_size > sector_start + sector_size - sector_address) {
                        //Need to transfer less than the xfer_size
                        xfer  = sector_start + sector_size - sector_address;
                        if (dfuseSpecialCommand(dev, sector_address, DFUSE_SET_ADDRESS) != 0) {
                            System.out.format("Error: Write failed to set address: 0x%x%n", sector_address);
                            return -1;
                        }
                        transaction = 2;
                    }
                    int offset = sector_address - address;
                    //System.out.format("Writing array at 0x%x (length: %d) dest: %x%n", sector_address, xfer, (transaction-2)*xfer + address);
                    byte[] buf = Arrays.copyOfRange(data, offset, offset + xfer);
                    
                    if (progress != null && progress.cancelled()) {
                        System.out.format("Cancelled at address 0x%x%n", sector_address);
                        return -1;
                    }
                    //address will be ((wBlockNum – 2) × wTransferSize) + Addres_Pointer
                    if (dfuseDownloadChunk(dev, buf, transaction) <= 0) {
                        System.out.format("Error: Write failed to write address : 0x%x%n", sector_address);
                        return -1;
                    }
                    sector_address += xfer;
                    transaction++;
                }
                if (sector_address >= address + data.length) {
                    return 0;
                }
                if (xfer != xfer_size) {
                    System.out.format("Error: xfer_size %d is not a multiple of the sector size: %d%n",
                                      xfer_size, sector.size());
                    return -1;
                }
            }
        }
    }
}
