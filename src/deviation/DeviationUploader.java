package deviation;

import de.ailis.usb4java.libusb.DeviceList;
import de.ailis.usb4java.libusb.LibUsb;
import deviation.DfuMemory.SegmentParser;
import deviation.commandline.CliOptions;
import deviation.commandline.CommandLineHandler;
import deviation.filesystem.FSUtils;
import deviation.gui.DeviationUploadGUI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
public class DeviationUploader
{
    public static byte[] applyEncryption(DfuFile.ImageElement elem, TxInfo info)
    {
        long address = elem.address();
        byte[] data = elem.data();
        if (address < 0x08005000L && address + data.length > 0x08005000L) {
            int offset = (int)(0x08005000L - address);
            byte[] txcode = info.encodeId();
            if (txcode != null) {
                System.out.println("Encrypting txid for " + TxInfo.typeToString(info.type()));
                for (byte b : txcode) {
                    data[offset++] = b;
                }
            }
        }
        return data;
    }
    public static boolean findDeviceByAddress(DfuDevice dev, int address, Integer vid, Integer pid, Integer alt)
    {
        if ((vid == null || vid == dev.idVendor()) && (pid == null || pid == dev.idProduct())) {
        	if (dev.SelectInterfaceByAddr(address, alt) != null) {
                return true;
            }
        }
        return false;
    }

    public static void sendDfuToDevice(DfuDevice dev, String fname, Progress progress)
    {
        DfuFile file;
        try {
            file = new DfuFile(fname);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        sendDfuToDevice(dev, file, progress);
    }
    public static void sendDfuToDevice(DfuDevice dev, DfuFile file, Progress progress)
    {
        for (DfuFile.ImageElement elem : file.imageElements()) {
        	if (progress != null && progress.cancelled()) {
        		return;
        	}
            if (! findDeviceByAddress(dev, (int)elem.address(), file.idVendor(), file.idProduct(), elem.altSetting())) {
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
            TxInfo info = dev.getTxInfo();
            if (! info.matchModel(TxInfo.getModelFromString(elem.name()))) {
                System.out.format("Error: Dfu Tx type '%s' does not match transmitter type '%s'%n",
                                  TxInfo.typeToString(TxInfo.getModelFromString(elem.name())),
                                  TxInfo.typeToString(info.type()));
                if (TxInfo.typeToString(info.type()) == "Unknown") {
                	System.out.format("\tTransmitter ID: '%s'\n", new String(info.getIdentifier()));
                }
                break;
            }
            byte [] data = applyEncryption(elem, info);
            //Write data
            Dfu.sendToDevice(dev, (int)elem.address(), data, progress);
            dev.close();
        }
    }
    
    public static void sendBinToDevice(DfuDevice dev, String fname, int address, Integer vid, Integer pid, Integer alt, Integer iface, boolean invert)
    {
        byte[] data;
        try {
            data = IOUtil.readFile(fname);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (invert) {
        	data = FSUtils.invert(data);
        }
        if (iface != null) {
    		dev.SelectInterface(dev.Interfaces().get(1));        	
        } else {
        	if (! findDeviceByAddress(dev, address, vid, pid, alt)) {
        		System.out.format("Error: Did not find matching device for VID:0x%x PID:0x%x alt:%d%n",
        				vid, pid, alt);
        		return;
        	}
        }
        if (dev.open() != 0) {
            System.out.println("Error: Unable to open device");
            return;
        }
        dev.claim_and_set();
        Dfu.setIdle(dev);
        TxInfo info = dev.getTxInfo();
        DfuFile.ImageElement elem = new DfuFile.ImageElement("Binary", dev.bAlternateSetting(), address, data);
        data = applyEncryption(elem, info);
        Dfu.sendToDevice(dev, address, data, null);
        dev.close();
    }

    public static void readBinFromDevice(DfuDevice dev, String fname, int address, Integer length, Integer vid, Integer pid, Integer alt, Integer iface, boolean invert)
    {
    	if (iface != null) {
    		dev.SelectInterface(dev.Interfaces().get(1));
    	} else {
    		if (! findDeviceByAddress(dev, address, vid, pid, alt)) {
    			System.out.format("Error: Did not find matching device for VID:0x%x PID:0x%x alt:%d%n",
    					vid, pid, alt);
    			return;
    		}
    	}
        if (length == 0) {
            length = (int)dev.Memory().contiguousSize(address);
        }
        if (dev.open() != 0) {
            System.out.println("Error: Unable to open device");
            return;
        }
        dev.claim_and_set();
        Dfu.setIdle(dev);
        
        byte [] data = Dfu.fetchFromDevice(dev, address, length);
        dev.close();
        if (invert) {
        	data = FSUtils.invert(data);
        }
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
    public static void listDevices(List <DfuDevice> devs)
    {
    	System.out.format("Device\t%9s %8s   %8s %7s   %s\n", "Interface", "Start", "End", "Size", "Count");
    	for (DfuDevice dev: devs) {
    		int i = 0;
    		System.out.format("%s\n", dev.getTxInfo().type().getName());
    		for (DfuInterface iface: dev.Interfaces()) {
    			for (SegmentParser segment: iface.Memory().segments()) {
    				for (Sector sector: segment.sectors()) {
    					System.out.format("\t\t%d %08x   %08x %7d   %d\n",i, sector.start(), sector.end(), sector.size(), sector.count());
    				}
    			}
    			i++;
    		}
    	}
    }
    public static void resetDevices(List <DfuDevice> devs)
    {
    	for (DfuDevice dev: devs) {
    		if (dev.open() != 0) {
    			System.out.println("Error: Unable to open device");
    			break;
    		}
    		dev.claim_and_set();
    		Dfu.setIdle(dev);
    		Dfu.resetSTM32(dev);
    		dev.close();
    	}
    }

    public static void main(String[] args)
    {
        CommandLineHandler cliHandler = new CommandLineHandler();
        CliOptions cliOptions = cliHandler.handleCmdLine(args);

        if (!cliOptions.hasProgramOptions()) {
            DeviationUploadGUI.main(null);
            while(true) {}
        }

        TransmitterList.init();

        Integer vendorId = null;
        Integer productId = null;
        if (cliOptions.hasTxId()) {
            String[] id = cliOptions.getTxIdValue().split(":");
            vendorId = Integer.parseInt(id[0], 16);
            productId = Integer.parseInt(id[1], 16);
        }

        Integer altSetting = null;
        if (cliOptions.hasAltSettings()) {
            altSetting = Integer.parseInt(cliOptions.getAltSettingsValue());
        }

        DeviceList devices = new DeviceList();
        LibUsb.init(null);
        LibUsb.getDeviceList(null, devices);
        List<DfuDevice> devs = Dfu.findDevices(devices);

        Integer iface = null;
        if (cliOptions.hasInterface()) {
        	iface = Integer.parseInt(cliOptions.getInterfaceValue());
        }

        for(DfuDevice dev : devs) {
        	DfuMemory mem = dev.Memory();
        	
            System.out.format("Found: %s [%05x:%04x] cfg=%d, intf=%d, alt=%d, name='%s'\n",
                dev.DFU_IFF_DFU() ? "DFU" : "Runtime",
                dev.idVendor(),
                dev.idProduct(),
                dev.bConfigurationValue(),
                dev.bInterfaceNumber(),
                dev.bAlternateSetting(),
                mem == null ? dev.GetId() : mem.name());
            //DfuFuncDescriptor desc = new DfuFuncDescriptor(dev);
        	dev.setTxInfo(TxInfo.getTxInfo(dev));
        }

        if (cliOptions.hasList()) {
        	listDevices(devs);
        }

        if (cliOptions.hasSend()) {
            if (cliOptions.hasDfu()) {
                sendDfuToDevice(devs.get(0), cliOptions.getDfuValue(), null);
            } else {
                int address = Long.decode(cliOptions.getAddressValue()).intValue();
                sendBinToDevice(devs.get(0), cliOptions.getBinValue(), address, vendorId, productId, altSetting, iface, cliOptions.hasInvert());
            }
        }

        if (cliOptions.hasFetch()) {
        	String addrStr = cliOptions.getAddressValue();
            int address = addrStr == null ? 0 : Long.decode(addrStr).intValue();
        	String lenStr = cliOptions.getLengthValue();
            int length = lenStr == null ? 0 : Integer.decode(lenStr);
            readBinFromDevice(devs.get(0), cliOptions.getBinValue(), address, length, vendorId, productId, altSetting, iface, cliOptions.hasInvert());
        }

        if (cliOptions.hasReset()) {
        	resetDevices(devs);
        }

        LibUsb.freeDeviceList(devices, true);
        LibUsb.exit(null);
    }
}
