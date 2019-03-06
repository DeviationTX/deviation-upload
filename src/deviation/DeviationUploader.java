package deviation;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import de.ailis.usb4java.libusb.*;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.util.*;
import de.waldheinz.fs.*;
import deviation.DfuMemory.SegmentParser;
import deviation.filesystem.FSUtils;
import deviation.filesystem.FileDisk2;
import deviation.filesystem.DevoFS.DevoFSFileSystem;
import deviation.gui.DeviationUploadGUI;

import org.apache.commons.cli.*;
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

    public static CommandLine handleCmdline(String[] args)
    {
        DeviationVersion ver = new DeviationVersion();
        Options optionsHelp = new Options();
        Options options = new Options();
        OptionGroup groupCmd = new OptionGroup();
        OptionGroup groupFile = new OptionGroup();
        optionsHelp.addOption(Option.builder("h")
        		                           .longOpt("help")
                                           .desc("Show help message")
                                           .build());
        optionsHelp.addOption(Option.builder("V")
        		                           .longOpt("version")
                                           .desc("Show help message")
                                           .build());

        groupCmd.addOption(Option.builder("s")
        		                        .longOpt("send")
                                        .desc("send file to transmitter")
                                        .build());
        groupCmd.addOption(Option.builder("f")
        		                        .longOpt("fetch")
                                        .desc("fetch file from transmitter")
                                        .build());
        groupCmd.addOption(Option.builder("l")
        		                        .longOpt("list")
                                        .desc("list transmitter interfaces")
                                        .build());
        //groupCmd.setRequired(true);
        options.addOptionGroup(groupCmd);
        groupFile.addOption(Option.builder("d")
        		                .longOpt("dfu")
                                .argName( "file" )
                                .hasArg()
                                .desc(  "specify Dfu file to send" )
                                .build());
        groupFile.addOption(Option.builder("b")
        		                .longOpt("bin")
                                .argName( "file" )
                                .hasArg()
                                .desc(  "specify bin file to send/receive" )
                                .build());
        options.addOptionGroup(groupFile);
        options.addOption(Option.builder("a")
        		                .longOpt("address")
                                .argName( "address" )
                                .hasArg()
                                .desc(  "specify address to send/receive from" )
                                .build());
        options.addOption(Option.builder()
        		                .longOpt("overwrite")
                                .desc(  "overwrite local files (only relevant with -fetch")
                                .build());
        options.addOption(Option.builder()
        		                .longOpt("length")
                                .argName( "bytes" )
                                .hasArg()
                                .desc(  "specify number of bytes to transfer" )
                                .build());
        options.addOption(Option.builder()
        		                .longOpt("txid")
                                .argName( "id" )
                                .hasArg()
                                .desc(  "specify the tx id as vendorid:productid" )
                                .build());
        options.addOption(Option.builder()
        		                .longOpt("alt-setting")
                                .argName( "id" )
                                .hasArg()
                                .desc(  "specify the alt-setting for this transfer" )
                                .build());
        options.addOption(Option.builder()
        						.longOpt("interface")
        						.argName( "interface" )
        						.hasArg()
        						.desc(  "manuallyoverride interface detection" )
        						.build());
        options.addOption(Option.builder()
        		                .longOpt("force-txtype")
                                .argName( "txType" )
                                .hasArg()
                                .desc(  "force the encryption to be to a specific transmitter type (very risky)" )
                                .build());
        options.addOption(Option.builder()
        		                .longOpt("ignore-dfu-check")
                                .desc(  "ignore Tx model checks")
                                .build());
        options.addOption(Option.builder()
        						.longOpt("invert")
        						.desc(   "invert data during bin read/write")
        						.build());
        options.addOption(Option.builder("h")
        		                .longOpt("help")
                                .desc("Show help message")
                                .build());
        options.addOption(Option.builder("V")
        		                .longOpt("version")
                                .desc("Show help message")
                                .build());
        options.addOption(Option.builder("r")
                .longOpt("reset")
                .desc("Reset after any other options have been perfomed")
                .build());

        try {
            //Handle help and version info here
            CommandLine cl = new DefaultParser().parse(optionsHelp, args, true);
            if (cl.getOptions().length != 0) {
                if (cl.hasOption("help")) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp(ver.name(), options);
                }
                if (cl.hasOption("version")) {
                    System.out.println(ver.name() + ": " + ver.version());
                }
                System.exit(0);
            }
            //No handle all other options
            cl = new DefaultParser().parse(options, args);
            if (groupCmd.getSelected() == null && ! cl.hasOption("reset")) {
            	System.err.println("Must specify at leats one of: -s -f -l -r -h");
            	System.exit(1);
            }
            String file = null;
            if (cl.hasOption("dfu")) {
                file = cl.getOptionValue("dfu");
            } else if (cl.hasOption("bin")) {
                file = cl.getOptionValue("bin");
            }
            if (file != null) {
                if (cl.hasOption("fetch") && ! cl.hasOption("overwrite") && new File(file).isFile()) {
                    System.err.println("File '" + file + "' already exists.");
                    System.exit(1);
                }
                if (cl.hasOption("send") && ! new File(file).isFile()) {
                    System.err.println("File '" + file + "' does not exist.");
                    System.exit(1);
                }
                if (! cl.hasOption("address")) {
                    if ((cl.hasOption("send") && cl.hasOption("bin")) || cl.hasOption("fetch")) {
                        System.err.println("Must specify -address");
                        System.exit(1);
                    }
                }
            } else if(cl.hasOption("send") || cl.hasOption("fetch")) {
                System.err.println("No file specified");
                System.exit(1);
            }
            for (String opt : new String[] {"address", "length"}) {
                if (cl.hasOption(opt)) {
                    try {
                        Long.decode(cl.getOptionValue(opt));
                    } catch (NumberFormatException ex) {
                        System.err.println("Must specify a valid numerical value to -" + opt);
                        System.exit(1);
                    }
                }
            }
       
            return cl;
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
        return null;
    }

    public static void main(String[] args)
    {
        if (args.length == 0) {
        	//DnDFrame.main(null);
            DeviationUploadGUI.main(null);
            while(true) {}
        }
        TransmitterList.init();
        CommandLine cl = handleCmdline(args);
        Integer vendorId = null;
        Integer productId = null;
        Integer altSetting = null;
        if (cl.hasOption("txid")) {
            String[] id = cl.getOptionValue("txid").split(":");
            vendorId = Integer.parseInt(id[0], 16);
            productId = Integer.parseInt(id[1], 16);
        }
        if (cl.hasOption("alt-setting")) {
            altSetting = Integer.parseInt(cl.getOptionValue("alt-setting"));
        }

        DeviceList devices = new DeviceList();
        LibUsb.init(null);
        LibUsb.getDeviceList(null, devices);
        List<DfuDevice> devs = Dfu.findDevices(devices);
        Integer iface = null;
        if (cl.hasOption("interface")) {
        	iface = Integer.parseInt(cl.getOptionValue("interface"));
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
        if (cl.hasOption("list")) {
        	listDevices(devs);
        }
        if (cl.hasOption("send")) {
            if (cl.hasOption("dfu")) {
                sendDfuToDevice(devs.get(0), cl.getOptionValue("dfu"), null);
            } else {
                int address = Long.decode(cl.getOptionValue("address")).intValue();
                sendBinToDevice(devs.get(0), cl.getOptionValue("bin"), address, vendorId, productId, altSetting, iface, cl.hasOption("invert"));
            }
        }
        if (cl.hasOption("fetch")) {
        	String addrStr = cl.getOptionValue("address");
            int address = addrStr == null ? 0 : Long.decode(addrStr).intValue();
        	String lenStr = cl.getOptionValue("length");
            int length = lenStr == null ? 0 : Integer.decode(lenStr);
            readBinFromDevice(devs.get(0), cl.getOptionValue("bin"), address, length, vendorId, productId, altSetting, iface, cl.hasOption("invert"));
        }
        if (cl.hasOption("reset")) {
        	resetDevices(devs);
        }
        LibUsb.freeDeviceList(devices, true);
        LibUsb.exit(null);
    }
}
