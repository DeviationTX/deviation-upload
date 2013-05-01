import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

import de.ailis.usb4java.libusb.*;

public class DeviationUploader
{
    public static byte[] applyEncryption(DfuFile.ImageElement elem, DeviationInfo info)
    {
        long address = elem.address();
        byte[] data = elem.data();
        if (address < 0x08005000L && address + data.length > 0x08005000L) {
            int offset = (int)(0x08005000L - address);
            byte[] txcode = info.encodeId();
            for (byte b : txcode) {
                data[offset++] = b;
            }
        }
        return data;
    }
    public static DfuDevice findDeviceByAddress(List<DfuDevice> devs, int address, Integer vid, Integer pid, Integer alt)
    {
        for (DfuDevice dev : devs) {
            if ((vid == null || vid == dev.idVendor())
                && (pid == null || pid == dev.idProduct())
                && (alt == null || alt == dev.bAlternateSetting()))
            {
                if (dev.Memory().find(address) != null) {
                    return dev;
                }
            }
        }
        return null;
    }

    public static void sendDfuToDevice(List<DfuDevice> devs, String fname)
    {
        DfuFile file;
        try {
            byte[] fdata = IOUtil.readFile(fname);
            file = new DfuFile(fdata);
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        for (DfuFile.ImageElement elem : file.imageElements()) {
            DfuDevice dev = findDeviceByAddress(devs, (int)elem.address(), file.idVendor(), file.idProduct(), elem.altSetting());
            if (dev == null) {
                System.out.format("Error: Did not find matching device for VID:0x%x PID:0x%x alt:%d%n",
                    file.idVendor(), file.idProduct(), elem.altSetting());
                continue;
            }
            if (dev.open() != 0) {
                System.out.println("Error: Unable to open device");
                break;
            }
            dev.claim_and_set();
            Dfu.setIdle(dev);
            byte [] txInfo = Dfu.fetchFromDevice(dev, 0x08000400, 0x40);
            DeviationInfo info = new DeviationInfo(txInfo);
            byte [] data = applyEncryption(elem, info);
            //Write data
            dev.close();
        }
    }

    public static void sendBinToDevice(List<DfuDevice> devs, String fname, int address, Integer vid, Integer pid, Integer alt)
    {
        byte[] data;
        try {
            data = IOUtil.readFile(fname);
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        DfuDevice dev = findDeviceByAddress(devs, address, vid, pid, alt);
        if (dev == null) {
            System.out.format("Error: Did not find matching device for VID:0x%x PID:0x%x alt:%d%n",
                    vid, pid, alt);
            return;
        }
        if (dev.open() != 0) {
            System.out.println("Error: Unable to open device");
            return;
        }
        dev.claim_and_set();
        Dfu.setIdle(dev);
        byte [] txInfo = Dfu.fetchFromDevice(dev, 0x08000400, 0x40);
        DeviationInfo info = new DeviationInfo(txInfo);
        DfuFile.ImageElement elem = new DfuFile.ImageElement("Binary", dev.bAlternateSetting(), address, data);
        data = applyEncryption(elem, info);
        Dfu.sendToDevice(dev, address, data);
        dev.close();
    }

    public static void readBinFromDevice(List<DfuDevice> devs, String fname, int address, Integer length, Integer vid, Integer pid, Integer alt)
    {
        DfuDevice dev = findDeviceByAddress(devs, address, vid, pid, alt);
        if (dev == null) {
            System.out.format("Error: Did not find matching device for VID:0x%x PID:0x%x alt:%d%n",
                    vid, pid, alt);
            return;
        }
        if (length == null) {
            length = dev.Memory().contiguousSize(address);
        }
        if (dev.open() != 0) {
            System.out.println("Error: Unable to open device");
            return;
        }
        dev.claim_and_set();
        Dfu.setIdle(dev);
        
        byte [] data = Dfu.fetchFromDevice(dev, address, length);
        dev.close();

        try{
            File f = new File(fname);
            FileOutputStream fop = new FileOutputStream(f);
            // if file doesnt exists, then create it
            if (!f.exists()) {
                f.createNewFile();
            }
            fop.write(data);
            fop.flush();
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                dev.Memory().name());
            //DfuFuncDescriptor desc = new DfuFuncDescriptor(dev);
        }
        //sendDfuToDevice(devs, "devo8.dfu");
        sendBinToDevice(devs, "file.toTx", 0x2000, null, null, null);
        readBinFromDevice(devs, "file.fromTx", 0x2000, 0x1000, null, null, null);
        LibUsb.freeDeviceList(devices, true);
        LibUsb.exit(null);
    }
}
