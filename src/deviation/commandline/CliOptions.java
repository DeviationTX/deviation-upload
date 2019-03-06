package deviation.commandline;

public class CliOptions {

  private boolean programOptions = false;

  private boolean txId;
  private String txIdValue;
  private boolean altSettings;
  private String altSettingsValue;
  private boolean anInterface;
  private String interfaceValue;
  private boolean list;
  private boolean send;
  private boolean dfu;
  private String dfuValue;
  private boolean bin;
  private String binValue;
  private boolean address;
  private String addressValue;
  private boolean length;
  private String lengthValue;
  private boolean fetch;
  private String fetchValue;
  private boolean invert;
  private String invertValue;
  private boolean reset;

  public boolean hasProgramOptions() {
    return programOptions;
  }

  public boolean hasTxId() {
    return txId;
  }

  public String getTxIdValue() {
    return txIdValue;
  }

  public boolean hasAltSettings() {
    return altSettings;
  }

  public String getAltSettingsValue() {
    return altSettingsValue;
  }

  public boolean hasInterface() {
    return anInterface;
  }

  public String getInterfaceValue() {
    return interfaceValue;
  }

  public boolean hasList() {
    return list;
  }

  public boolean hasSend() {
    return send;
  }

  public boolean hasDfu() {
    return dfu;
  }

  public String getDfuValue() {
    return dfuValue;
  }

  public boolean hasBin() {
    return bin;
  }

  public String getBinValue() {
    return binValue;
  }

  public boolean hasAddress() {
    return address;
  }

  public String getAddressValue() {
    return addressValue;
  }

  public boolean hasReset() {
    return reset;
  }

  public boolean hasLength() {
    return length;
  }

  public String getLengthValue() {
    return lengthValue;
  }

  public boolean hasFetch() {
    return fetch;
  }

  public String getFetchValue() {
    return fetchValue;
  }

  public boolean hasInvert() {
    return invert;
  }

  public String getInvertValue() {
    return invertValue;
  }

  CliOptions setProgramOptions(boolean programOptions) {
    this.programOptions = programOptions;
    return this;
  }

  CliOptions setTxId(boolean txId) {
    this.txId = txId;
    return this;
  }

  CliOptions setTxIdValue(String txIdValue) {
    this.txIdValue = txIdValue;
    return this;
  }

  CliOptions setAltSettings(boolean altSettings) {
    this.altSettings = altSettings;
    return this;
  }

  CliOptions setAltSettingsValue(String altSettingsValue) {
    this.altSettingsValue = altSettingsValue;
    return this;
  }

  CliOptions setInterface(boolean anInterface) {
    this.anInterface = anInterface;
    return this;
  }

  CliOptions setInterfaceValue(String interfaceValue) {
    this.interfaceValue = interfaceValue;
    return this;
  }

  CliOptions setList(boolean list) {
    this.list = list;
    return this;
  }

  CliOptions setSend(boolean send) {
    this.send = send;
    return this;
  }

  CliOptions setDfu(boolean dfu) {
    this.dfu = dfu;
    return this;
  }

  CliOptions setDfuValue(String dfuValue) {
    this.dfuValue = dfuValue;
    return this;
  }

  CliOptions setBin(boolean bin) {
    this.bin = bin;
    return this;
  }

  CliOptions setBinValue(String binValue) {
    this.binValue = binValue;
    return this;
  }

  CliOptions setAddress(boolean address) {
    this.address = address;
    return this;
  }

  CliOptions setAddressValue(String addressValue) {
    this.addressValue = addressValue;
    return this;
  }

  CliOptions setLength(boolean length) {
    this.length = length;
    return this;
  }

  CliOptions setLengthValue(String lengthValue) {
    this.lengthValue = lengthValue;
    return this;
  }

  CliOptions setFetch(boolean fetch) {
    this.fetch = fetch;
    return this;
  }

  CliOptions setFetchValue(String fetchValue) {
    this.fetchValue = fetchValue;
    return this;
  }

  CliOptions setInvert(boolean invert) {
    this.invert = invert;
    return this;
  }

  CliOptions setInvertValue(String invertValue) {
    this.invertValue = invertValue;
    return this;
  }

  CliOptions setReset(boolean reset) {
    this.reset = reset;
    return this;
  }

}
