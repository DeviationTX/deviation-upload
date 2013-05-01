import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

import de.ailis.usb4java.libusb.*;
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
            System.out.println("Encrypting txid for " + TxInfo.typeToString(info.type()));
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
            TxInfo info = new TxInfo(txInfo);
            if (! info.matchType(TxInfo.getTypeFromString(elem.name()))) {
                System.out.format("Error: Dfu Tx type '%s' does not match transmitter type '%s'%n",
                                  TxInfo.typeToString(info.type()),
                                  TxInfo.typeToString(TxInfo.getTypeFromString(elem.name())));
                break;
            }
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
        TxInfo info = new TxInfo(txInfo);
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

    public static CommandLine handleCmdline(String[] args)
    {
        DeviationVersion ver = new DeviationVersion();
        Options optionsHelp = new Options();
        Options options = new Options();
        OptionGroup groupCmd = new OptionGroup();
        OptionGroup groupFile = new OptionGroup();
        optionsHelp.addOption(OptionBuilder.withLongOpt("help")
                                           .withDescription("Show help message")
                                           .create("h"));
        optionsHelp.addOption(OptionBuilder.withLongOpt("version")
                                           .withDescription("Show help message")
                                           .create("V"));

        groupCmd.addOption(OptionBuilder.withLongOpt("send")
                                        .withDescription("send file to transmitter")
                                        .create("s"));
        groupCmd.addOption(OptionBuilder.withLongOpt("fetch")
                                        .withDescription("fetch file from transmitter")
                                        .create("f"));
        groupCmd.addOption(OptionBuilder.withLongOpt("list")
                                        .withDescription("list transmitter interfaces")
                                        .create("l"));
        groupCmd.setRequired(true);
        options.addOptionGroup(groupCmd);
        groupFile.addOption(OptionBuilder.withLongOpt("dfu")
                                .withArgName( "file" )
                                .hasArg()
                                .withDescription(  "specify Dfu file to send" )
                                .create( "d" ));
        groupFile.addOption(OptionBuilder.withLongOpt("bin")
                                .withArgName( "file" )
                                .hasArg()
                                .withDescription(  "specify bin file to send/receive" )
                                .create( "b" ));
        options.addOptionGroup(groupFile);
        options.addOption(OptionBuilder.withLongOpt("address")
                                .withArgName( "address" )
                                .hasArg()
                                .withDescription(  "specify address to send/receive from" )
                                .create( "a" ));
        options.addOption(OptionBuilder.withLongOpt("overwrite")
                                .withDescription(  "overwrite local files (only relevant with -fetch")
                                .create());
        options.addOption(OptionBuilder.withLongOpt("length")
                                .withArgName( "bytes" )
                                .hasArg()
                                .withDescription(  "specify number of bytes to transfer" )
                                .create());
        options.addOption(OptionBuilder.withLongOpt("txid")
                                .withArgName( "id" )
                                .hasArg()
                                .withDescription(  "specify the tx id as vendorid:productid" )
                                .create());
        options.addOption(OptionBuilder.withLongOpt("alt-setting")
                                .withArgName( "id" )
                                .hasArg()
                                .withDescription(  "specify the alt-setting for this transfer" )
                                .create());
        options.addOption(OptionBuilder.withLongOpt("force-txtype")
                                .withArgName( "txType" )
                                .hasArg()
                                .withDescription(  "force the encryption to be to a specific transmitter type (very risky)" )
                                .create());
        options.addOption(OptionBuilder.withLongOpt("ignore-dfu-check")
                                .withDescription(  "ignore Tx model checks")
                                .create());
        options.addOption(OptionBuilder.withLongOpt("help")
                                       .withDescription("Show help message")
                                       .create("h"));
        options.addOption(OptionBuilder.withLongOpt("version")
                                       .withDescription("Show help message")
                                       .create("V"));

        try {
            //Handle help and version info here
            CommandLine cl = new DefaultParser().parse(optionsHelp, args, true);
            if (cl.getOptions().length != 0) {
                if (cl.hasOption("help")) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp( ver.name(), options );
                }
                if (cl.hasOption("version")) {
                    System.out.println(ver.name() + ": " + ver.version());
                }
                System.exit(0);
            }
            //No handle all other options
            cl = new DefaultParser().parse(options, args);
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
        if (cl.hasOption("send")) {
            if (cl.hasOption("dfu")) {
                sendDfuToDevice(devs, cl.getOptionValue("dfu"));
            } else {
                int address = Long.decode(cl.getOptionValue("address")).intValue();
                sendBinToDevice(devs, cl.getOptionValue("bin"), address, vendorId, productId, altSetting);
            }
        }
        if (cl.hasOption("fetch")) {
            int address = Long.decode(cl.getOptionValue("address")).intValue();
            int length = Integer.decode(cl.getOptionValue("length"));
            readBinFromDevice(devs, cl.getOptionValue("bin"), address, length, vendorId, productId, altSetting);
        }
        LibUsb.freeDeviceList(devices, true);
        LibUsb.exit(null);
    }
}
