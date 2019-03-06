package deviation.commandline;

import deviation.DeviationVersion;
import org.apache.commons.cli.*;

import java.io.File;

public class CommandLineHandler {

  private static final String VERBOSE = "v";
  private static final String EXTRA_VERBOSE = "vv";
  private static final String TXID = "txid";
  private static final String ALT_SETTING = "alt-setting";
  private static final String INTERFACE = "interface";
  private static final String LIST = "list";
  private static final String SEND = "send";
  private static final String DFU = "dfu";
  private static final String BIN = "bin";
  private static final String ADDRESS = "address";
  private static final String LENGTH = "length";
  private static final String FETCH = "fetch";
  private static final String INVERT = "invert";
  private static final String RESET = "reset";
  private static final String OVERWRITE = "overwrite";
  private static final String VERSION = "version";
  private static final String HELP = "help";

  private final DeviationVersion ver = new DeviationVersion();
  private final Options optionsHelp = new Options();
  private final Options options = new Options();
  private final OptionGroup groupCmd = new OptionGroup();
  private final OptionGroup groupFile = new OptionGroup();

  public CommandLineHandler() {
    configureCommandLineOptions();
  }

  public CliOptions handleCmdLine(String[] args) {
    if (handleHelpAndVersionOptionOrDie(args)) {
      System.exit(0);
    };
    CommandLine commandLine = handleRegularOptionsOrDie(args);
    CliOptions cliOptions = new CliOptions()
        .setTxId(commandLine.hasOption(TXID))
        .setTxIdValue(commandLine.getOptionValue(TXID))
        .setAltSettings(commandLine.hasOption(ALT_SETTING))
        .setAltSettingsValue(commandLine.getOptionValue(ALT_SETTING))
        .setInterface(commandLine.hasOption(INTERFACE))
        .setInterfaceValue(commandLine.getOptionValue(INTERFACE))
        .setList(commandLine.hasOption(LIST))
        .setSend(commandLine.hasOption(SEND))
        .setDfu(commandLine.hasOption(DFU))
        .setDfuValue(commandLine.getOptionValue(DFU))
        .setBin(commandLine.hasOption(BIN))
        .setBinValue(commandLine.getOptionValue(BIN))
        .setFetch(commandLine.hasOption(FETCH))
        .setFetchValue(commandLine.getOptionValue(FETCH))
        .setInvert(commandLine.hasOption(INVERT))
        .setInvertValue(commandLine.getOptionValue(INVERT))
        .setAddress(commandLine.hasOption(ADDRESS))
        .setAddressValue(commandLine.getOptionValue(ADDRESS))
        .setLength(commandLine.hasOption(LENGTH))
        .setLengthValue(commandLine.getOptionValue(LENGTH))
        .setReset(commandLine.hasOption(RESET));
    cliOptions.setProgramOptions(
        cliOptions.hasTxId()
            || cliOptions.hasAltSettings()
            || cliOptions.hasInterface()
            || cliOptions.hasList()
            || cliOptions.hasSend()
            || cliOptions.hasDfu()
            || cliOptions.hasBin()
            || cliOptions.hasFetch()
            || cliOptions.hasInvert()
            || cliOptions.hasAddress()
            || cliOptions.hasLength()
            || cliOptions.hasReset()
    );
    return cliOptions;
  }

  private void configureCommandLineOptions() {
    optionsHelp.addOption(Option.builder("h")
        .longOpt(HELP)
        .desc("show help message")
        .build());
    optionsHelp.addOption(Option.builder("V")
        .longOpt(VERSION)
        .desc("show help message")
        .build());

    groupCmd.addOption(Option.builder("s")
        .longOpt(SEND)
        .desc("send file to transmitter")
        .build());
    groupCmd.addOption(Option.builder("f")
        .longOpt(FETCH)
        .desc("fetch file from transmitter")
        .build());
    groupCmd.addOption(Option.builder("l")
        .longOpt(LIST)
        .desc("list transmitter interfaces")
        .build());
    options.addOptionGroup(groupCmd);
    groupFile.addOption(Option.builder("d")
        .longOpt(DFU)
        .argName("file")
        .hasArg()
        .desc("specify Dfu file to send")
        .build());
    groupFile.addOption(Option.builder("b")
        .longOpt(BIN)
        .argName("file")
        .hasArg()
        .desc("specify bin file to send/receive")
        .build());
    options.addOptionGroup(groupFile);
    options.addOption(Option.builder("a")
        .longOpt(ADDRESS)
        .argName(ADDRESS)
        .hasArg()
        .desc("specify address to send/receive from. an address has to be formatted like <0x12345678>")
        .build());
    options.addOption(Option.builder()
        .longOpt(OVERWRITE)
        .desc("overwrite local files (only relevant with -fetch")
        .build());
    options.addOption(Option.builder()
        .longOpt(LENGTH)
        .argName("bytes")
        .hasArg()
        .desc("specify number of bytes to transfer")
        .build());
    options.addOption(Option.builder()
        .longOpt(TXID)
        .argName("id")
        .hasArg()
        .desc("specify the tx id as <vendorid:productid>")
        .build());
    options.addOption(Option.builder()
        .longOpt(ALT_SETTING)
        .argName("id")
        .hasArg()
        .desc("specify the alt-setting for this transfer")
        .build());
    options.addOption(Option.builder()
        .longOpt(INTERFACE)
        .argName(INTERFACE)
        .hasArg()
        .desc("manually override interface detection")
        .build());
    options.addOption(Option.builder()
        .longOpt("force-txtype")
        .argName("txType")
        .hasArg()
        .desc("force the encryption to be to a specific transmitter type (very risky)")
        .build());
    options.addOption(Option.builder()
        .longOpt("ignore-dfu-check")
        .desc("ignore Tx model checks")
        .build());
    options.addOption(Option.builder()
        .longOpt(INVERT)
        .desc("invert data during bin read/write")
        .build());
    options.addOption(Option.builder("h")
        .longOpt(HELP)
        .desc("show help message")
        .build());
    options.addOption(Option.builder("V")
        .longOpt(VERSION)
        .desc("show program version message")
        .build());
    options.addOption(Option.builder("r")
        .longOpt(RESET)
        .desc("reset after any other options have been perfomed")
        .build());
    options.addOption(Option.builder(VERBOSE)
            .longOpt("verbose")
            .desc("verbose console logging")
            .build());
    options.addOption(Option.builder(EXTRA_VERBOSE)
            .longOpt("extra-verbose")
            .desc("extra verbose console logging")
            .build());
  }

  private boolean handleHelpAndVersionOptionOrDie(String[] args) {
    CommandLine cl = null;
    try {
      cl = new DefaultParser().parse(optionsHelp, args, true);
    } catch (UnrecognizedOptionException e) {
      System.err.println(e.getMessage() + "\nTry command line argument -h for help.");
      System.exit(1);
    } catch (ParseException e) {
      throw new RuntimeException("Could not parse 'optionsHelp'. Aborting.", e);
    }
    if (cl.getOptions().length != 0) {
      if (cl.hasOption(HELP)) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(ver.name(), options);
      }
      if (cl.hasOption(VERSION)) {
        System.out.println(ver.name() + ": " + ver.version());
      }
      return true;
    }
    return false;
  }

  private CommandLine handleRegularOptionsOrDie(String[] args) {
    CommandLine cl = null;
    try {
      cl = new DefaultParser().parse(options, args);
    } catch (UnrecognizedOptionException e) {
      System.err.println(e.getMessage() + "\nTry command line argument -h for help.");
      System.exit(1);
    } catch (ParseException e) {
      throw new RuntimeException("Could not parse 'options'. Aborting.", e);
    }

    String file = null;
    if (cl.hasOption(DFU)) {
      file = cl.getOptionValue(DFU);
    } else if (cl.hasOption(BIN)) {
      file = cl.getOptionValue(BIN);
    }
    if (file != null) {
      if (cl.hasOption(FETCH) && !cl.hasOption(OVERWRITE) && new File(file).isFile()) {
        System.err.println("File '" + file + "' already exists.");
        System.exit(1);
      }
      if (cl.hasOption(SEND) && !new File(file).isFile()) {
        System.err.println("File '" + file + "' does not exist.");
        System.exit(1);
      }
      if (!cl.hasOption(ADDRESS)) {
        if ((cl.hasOption(SEND) && cl.hasOption(BIN)) || cl.hasOption(FETCH)) {
          System.err.println("Must specify -address");
          System.exit(1);
        }
      }
    } else if (cl.hasOption(SEND) || cl.hasOption(FETCH)) {
      System.err.println("No file specified");
      System.exit(1);
    }
    for (String opt : new String[]{ADDRESS, LENGTH}) {
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
  }

}
