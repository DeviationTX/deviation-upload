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
    public static DfuDevice find_device(List<DfuDevice> devs, int vid, int pid, int alt)
    {
        for (DfuDevice dev : devs) {
            if (vid == dev.idVendor() && pid == dev.idProduct() && alt == dev.bAlternateSetting()) {
                return dev;
            }
        }
        return null;
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

        DfuFile file;
        try {
            byte[] fname = IOUtil.readFile("devo8.dfu");
            file = new DfuFile(fname);
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
            throw new RuntimeException(e);
        }
        for (DfuFile.ImageElement elem : file.imageElements()) {
            DfuDevice dev = find_device(devs, file.idVendor(), file.idProduct(), elem.altSetting());
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
            byte [] txInfo = Dfu.FetchFromDevice(dev, 0x08000400, 0x40);
            DeviationInfo info = new DeviationInfo(txInfo);
            byte [] data = applyEncryption(elem, info);
            dev.close();

            try{
                File f = new File("output.txt");
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
        LibUsb.freeDeviceList(devices, true);
        LibUsb.exit(null);
    }
}
