package deviation;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import de.ailis.usb4java.libusb.*;

import static java.util.logging.Level.FINEST;

public final class Dfu
{

    private static final Logger LOG = Logger.getLogger(Dfu.class.getName());

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
                    if (LOG.isLoggable(FINEST)) {
                        LOG.finest(String.format("%04x:%04x -> %02x/%02x %02x/%02x",
                                          desc.idVendor() & 0xffff,
                                          desc.idProduct() & 0xffff,
                                          intf.bInterfaceNumber(),
                                          intf.bAlternateSetting(),
                                          intf.bInterfaceClass(),
                                          intf.bInterfaceSubClass()));
                    }
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
    public static DfuDevice findFirstDevice(DeviceList usb_devices)
    {
    	List<DfuDevice> devices = findDevices(usb_devices);
    	return devices.size() > 0 ? devices.get(0) : null;
    }

    public static int detach(DfuDevice dev, int timeout) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(6);
        return LibUsb.controlTransfer( dev.Handle(),
            /* bmRequestType */ (LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE),
            /* bRequest      */ DFU_DETACH,
            /* wValue        */ timeout,
            /* wIndex        */ dev.bInterfaceNumber(),
            /* Data          */ buffer,
                                dfu_timeout );
    }

    public static int dfuseSpecialCommand(DfuDevice dev, long address, int command) {
        byte [] buf = new byte[5];
        int ret;
        DfuStatus status;

        if (command == DFUSE_ERASE_PAGE) {
            Sector sector = dev.Memory().find(address);
            if (sector == null || ! sector.erasable()) {
                LOG.severe(String.format("Error: Page at 0x%x can not be erased", address));
                return -1;
            }
            LOG.severe(String.format("Erasing page size %d at address 0x%x, page "
                           + "starting at 0x%x", sector.size(), address,
                           address & ~(sector.size() - 1)));
            buf[0] = 0x41;  // Erase command
        } else if (command == DFUSE_SET_ADDRESS) {
            LOG.fine(String.format("Setting address pointer to 0x%x", address));
            buf[0] = 0x21;  /* Set Address Pointer command */
        } else {
            LOG.severe(String.format("Error: Non-supported special command %d", command));
            return -1;
        }
        buf[1] = (byte)(address & 0xff);
        buf[2] = (byte)((address >> 8) & 0xff);
        buf[3] = (byte)((address >> 16) & 0xff);
        buf[4] = (byte)((address >> 24) & 0xff);

        ret = dfuseDownload(dev, buf, 0);
        if (ret < 0) {
            LOG.severe("Error during special command download");
            return -1;
        }
        // 1st getStatus
        status = getStatus(dev);
        if (status.bState != DfuStatus.STATE_DFU_DOWNLOAD_BUSY) {
            LOG.severe("Error: Wrong state after command download");
            return -1;
        }
        // wait while command is executed
        LOG.fine(String.format("Poll timeout %d ms", status.bwPollTimeout));
        try {
            Thread.sleep(status.bwPollTimeout);
        } catch (InterruptedException e) {} //Don't care if we're interrupted
        // 2nd getStatus
        status = getStatus(dev);
        if (status.bStatus != DfuStatus.DFU_STATUS_OK) {
            LOG.severe(String.format("Error during second get_status"
                   + " -- state(%d) = %s, status(%d) = %s",
                   status.bState, status.stateToString(),
                   status.bStatus, status.statusToString()));
            return -1;
        }
        try {
            Thread.sleep(status.bwPollTimeout);
        } catch (InterruptedException e) {} //Don't care if we're interrupted

        ret = abort(dev);
        if (ret < 0) {
            LOG.severe("Error sending dfu abort request");
            return -1;
        }

        // 3rd getStatus
        status =getStatus(dev);
        if (status.bState != DfuStatus.STATE_DFU_IDLE) {
                LOG.severe("Error: Failed to enter idle state on abort");
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
            LOG.info(String.format("dfuseUpload: libusb_control_transfer returned %d", bytes_received));
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
            LOG.info(String.format("dfuseDownload: libusb_control_transfer returned %d", bytes_sent));
            return bytes_sent;
        }
        return bytes_sent;
    }
    //Send To Device
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
            LOG.info("Transitioning to dfuMANIFEST state");
        }

        if (status.bStatus != DfuStatus.DFU_STATUS_OK) {
                LOG.severe(String.format("Error: state(%d) = %s, status(%d) = %s",
                    status.bState, status.stateToString(),
                    status.bStatus, status.statusToString()));
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
            LOG.fine(String.format("Determining device status: state=%s status=%d",
                          status.stateToString(), status.bStatus));

            switch (status.bState) {
                case DfuStatus.STATE_APP_IDLE:
                case DfuStatus.STATE_APP_DETACH:
                    LOG.severe("Device still in Runtime Mode!");
                    return -1;
                case DfuStatus.STATE_DFU_ERROR:
                    LOG.info("dfuERROR, clearing status");
                    if (clearStatus(dev) < 0) {
                        LOG.severe("error clear_status");
                        return -1;
                    }
                    break;
                case DfuStatus.STATE_DFU_DOWNLOAD_IDLE:
                case DfuStatus.STATE_DFU_UPLOAD_IDLE:
                    LOG.fine("aborting previous incomplete transfer");
                    if (abort(dev) < 0) {
                        LOG.severe("can't send DFU_ABORT");
                        return -1;
                    }
                    break;
                case DfuStatus.STATE_DFU_IDLE:
                    LOG.fine("dfuIDLE, continuing");
                    done = true;
                    break;
            }
        }
        if (DfuStatus.DFU_STATUS_OK != status.bStatus ) {
            LOG.warning(String.format("DFU Status: '%s'", status.statusToString()));
            // Clear our status & try again.
            clearStatus(dev);
            getStatus(dev);
            if (DfuStatus.DFU_STATUS_OK != status.bStatus) {
                LOG.severe(String.format("Error: %d", status.bStatus));
                return -1;
            }
        }
        return 0;
    }
    public static byte [] fetchFromDevice(DfuDevice dev, long address, int requested_length)
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

        LOG.finer(String.format("bytes_per_hash=%d", xfer_size));
        LOG.info("Starting device read");

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
    public static int sendToDevice(DfuDevice dev, long address, byte[] data,  Progress progress)
    {
        int xfer_size = 1024;
        // ensure the entire data rangeis writeable
        long sector_address = address;
        setIdle(dev);
        while (true) {
            Sector sector = dev.Memory().find(sector_address);
            LOG.fine(String.format("%d: %d (%d)", sector_address, sector == null ? -1 : sector.end(), address + data.length));
            if (sector == null || ! sector.writable()) {
                LOG.severe(String.format("Error: No sector found that can be written at address 0x%08x", sector_address));
                return -1;
            }
            if (sector.end() + 1>= address + data.length) {
                break;
            }
            sector_address = sector.end() + 1;
        }

        // erase and write
        sector_address = address;
        int transaction = 2;
        if (dfuseSpecialCommand(dev, sector_address, DFUSE_SET_ADDRESS) != 0) {
            LOG.severe(String.format("Error: Write failed to set address: 0x%x", sector_address));
            return -1;
        }
        while (true) {
            Sector sector = dev.Memory().find(sector_address);
            long sector_size = sector.size();
            long sector_index = (sector_address - sector.start()) / sector_size;
            long sector_start = sector.start() + sector_index * sector_size;
            if (progress != null) {
                progress.update((int)(data.length > sector_size ? sector_size : data.length));
                if (progress.cancelled()) {
                    LOG.severe(String.format("Cancelled at address 0x%x", sector_address));
                    return -1;
                }
            }
            if (sector.erasable()) {
                LOG.info(String.format("Erasing page: 0x%x", sector_address));
                if (dfuseSpecialCommand(dev, sector_address, DFUSE_ERASE_PAGE) != 0) {
                    LOG.severe(String.format("Error: Write failed to erase address: 0x%x", sector_address));
                    return -1;
                }
            }
            if (dfuseSpecialCommand(dev, sector_address, DFUSE_SET_ADDRESS) != 0) {
                LOG.severe(String.format("Error: Write failed to set address: 0x%x", sector_address));
                return -1;
            }
            transaction = 2;

            if (address + data.length - sector_address < sector_size) {
                //Remaining data is less than the sector size
                sector_size = address + data.length - sector_address;
            }
            int xfer = xfer_size;
            while (sector_address < sector_start + sector_size) {
                if (xfer_size > sector_start + sector_size - sector_address) {
                    //Need to transfer less than the xfer_size
                    xfer  = (int)(sector_start + sector_size - sector_address);
                    if (dfuseSpecialCommand(dev, sector_address, DFUSE_SET_ADDRESS) != 0) {
                        LOG.severe(String.format("Error: Write failed to set address: 0x%x", sector_address));
                        return -1;
                    }
                    transaction = 2;
                }
                int offset = (int)(sector_address - address);
                //LOG.info(String.format("Writing array at 0x%x (length: %d) dest: %x", sector_address, xfer, (transaction-2)*xfer + address));
                byte[] buf = Arrays.copyOfRange(data, offset, offset + xfer);
                
                if (progress != null && progress.cancelled()) {
                    LOG.severe(String.format("Cancelled at address 0x%x", sector_address));
                    return -1;
                }
                //address will be ((wBlockNum – 2) × wTransferSize) + Addres_Pointer
                if (dfuseDownloadChunk(dev, buf, transaction) <= 0) {
                    LOG.severe(String.format("Error: Write failed to write address : 0x%x", sector_address));
                    return -1;
                }
                sector_address += xfer;
                transaction++;
            }
            if (sector_address >= address + data.length) {
                return 0;
            }
            if (xfer != xfer_size) {
                LOG.severe(String.format("Error: xfer_size %d is not a multiple of the sector size: %d",
                                  xfer_size, sector.size()));
                return -1;
            }
        }
    }
    public static int resetSTM32(DfuDevice dev) {
    	DfuStatus status = new DfuStatus(null);
      LOG.info("Resetting STM32, starting firmware at address 0x08000000...");
    	int set_ret = dfuseSpecialCommand(dev, 0x08000000, DFUSE_SET_ADDRESS);
    	if( set_ret < 0 ) {
          LOG.severe("Error: Unable to set start address for resetting");
    		return -1;
    	}

    	int dret = dfuseDownload(dev, new byte[0], 2);

    	if( dret < 0 ) {
          LOG.severe(String.format("Error: Unable to initiate zero-length download"));
    		return -1;
    	}
    	status = getStatus(dev);

    	if( status.bState != DfuStatus.STATE_DFU_MANIFEST) {
        LOG.severe("Error: Expected STM32 to be in dfuMANIFEST state after get-status command!");
    	} else {
          LOG.fine("Successfully reset STM32");
    	}
    	return 0;
    }
}
