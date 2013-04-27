class DfuStatus {
    public static final byte  DFU_STATUS_OK = 0;
    public static final byte  DFU_STATUS_errTARGET = 1;
    public static final byte  DFU_STATUS_errFILE = 2;
    public static final byte  DFU_STATUS_errWRITE = 3;
    public static final byte  DFU_STATUS_errERASE = 4;
    public static final byte  DFU_STATUS_errCHECK_ERASED = 5;
    public static final byte  DFU_STATUS_errPROG = 6;
    public static final byte  DFU_STATUS_errVERIFY = 7;
    public static final byte  DFU_STATUS_errADDRESS = 8;
    public static final byte  DFU_STATUS_errNOTDONE = 9;
    public static final byte  DFU_STATUS_errFIRMWARE = 10;
    public static final byte  DFU_STATUS_errVENDOR = 11;
    public static final byte  DFU_STATUS_errUSBR = 12;
    public static final byte  DFU_STATUS_errPOR = 13;
    public static final byte  DFU_STATUS_errUNKNOWN = 14;
    public static final byte  DFU_STATUS_ERROR_UNKNOWN = 14;
    public static final byte  DFU_STATUS_errSTALLEDPKT = 15;

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
    public static String StatusToString(int status)
    {
        switch(status) {
            case DFU_STATUS_OK:        return "No error condition is present";
	    case DFU_STATUS_errTARGET: return "File is not targeted for use by this device";
	    case DFU_STATUS_errFILE:   return "File is for this device but fails some vendor-specific test";
	    case DFU_STATUS_errWRITE:  return "Device is unable to write memory";
	    case DFU_STATUS_errERASE:  return "Memory erase function failed";
	    case DFU_STATUS_errCHECK_ERASED: return "Memory erase check failed";
	    case DFU_STATUS_errPROG:   return "Program memory function failed";
	    case DFU_STATUS_errVERIFY: return "Programmed memory failed verification";
	    case DFU_STATUS_errADDRESS: return "Cannot program memory due to received address that is out of range";
	    case DFU_STATUS_errNOTDONE: return "Received DFU_DNLOAD with wLength = 0, but device does not think that it has all data yet";
	    case DFU_STATUS_errFIRMWARE: return "Device's firmware is corrupt. It cannot return to run-time (non-DFU) operations";
	    case DFU_STATUS_errVENDOR: return "iString indicates a vendor specific error";
	    case DFU_STATUS_errUSBR:   return "Device detected unexpected USB reset signalling";
	    case DFU_STATUS_errPOR:    return "Device detected unexpected power on reset";
	    case DFU_STATUS_errUNKNOWN: return "Something went wrong, but the device does not know what it was";
	    case DFU_STATUS_errSTALLEDPKT: return "Device stalled an unexpected request";
        }
        return "Unknown Status";
    }
}
