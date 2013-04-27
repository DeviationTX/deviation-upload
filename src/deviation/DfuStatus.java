class DfuStatus {
    public static final byte  DFU_STATUS_ERROR_UNKNOWN = 0x0e;
    public static final byte  STATE_DFU_ERROR = 0x0a;
    public byte bStatus;
    public int bwPollTimeout;
    public byte bState;
    public int  iString;

    public DfuStatus(byte data[]) {
        if (data == null) {
            bStatus       = DFU_STATUS_ERROR_UNKNOWN;
            bwPollTimeout = 0;
            bState        = STATE_DFU_ERROR;
            iString       = 0;
        } else {
            bStatus       = data[0];
            bwPollTimeout = ((0xff & data[3]) << 16) |
                                ((0xff & data[2]) << 8)  |
                                (0xff & data[1]);
            bState        = data[4];
            iString       = data[5];
        }
    }
}
