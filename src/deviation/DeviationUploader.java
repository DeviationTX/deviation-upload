import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

import de.ailis.usb4java.libusb.*;

public class DeviationUploader
{
    public static void main(String[] args)
    {
        try {
            byte[] data = IOUtil.readFile("devo8.dfu");
            DfuFile f = new DfuFile(data);
            return;
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
            //throw new RuntimeException(e);
        }
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
        int idx = 0;
        for (DfuDevice dev : devs) {
            if (dev.open() != 0) {
                System.out.println("Error: Unable to open device");
                break;
            }
            dev.claim_and_set();
            Dfu.setIdle(dev);
            byte [] data = Dfu.FetchFromDevice(dev, 0x08000400, 0x40);
            DeviationInfo info = new DeviationInfo(data);
            System.out.format("%s : %x %x %x%n", info.model(), info.id1(), info.id2(), info.id3());
/*
            try{
                File file = new File("output" + Integer.toString(idx) + ".txt");
                FileOutputStream fop = new FileOutputStream(file);
                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }
                fop.write(data);
                fop.flush();
                fop.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
*/
            dev.close();
            idx++;
            break;
        }
        LibUsb.freeDeviceList(devices, true);
        LibUsb.exit(null);
    }
}
