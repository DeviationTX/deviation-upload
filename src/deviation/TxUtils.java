package deviation;

import deviation.DevoFat.FatStatus;

public class TxUtils {
    public static TxInfo getTxInfo(DfuDevice dev)
    {
        dev.SelectInterface(dev.Interfaces().get(0));
        if (dev.open() != 0) {
            System.out.println("Error: Unable to open device");
            return new TxInfo();
        }
        dev.claim_and_set();
        Dfu.setIdle(dev);
        byte [] txInfoBytes = Dfu.fetchFromDevice(dev, 0x08000400, 0x40);
        TxInfo txInfo = new TxInfo(txInfoBytes);
        dev.close();
        return txInfo;

    }
    public static FatStatus getFatStatus(DfuDevice dev, Transmitter txType) {
    	final int SECTOR_SIZE = 0x1000;
        boolean has_root = false;
        boolean has_media = false;
        FatStatus status = FatStatus.NO_FAT;
        if (txType == Transmitter.DEVO_UNKNOWN) {
            return status;
        }
        
        dev.SelectInterface(dev.Interfaces().get(0));
        if (dev.open() != 0) {
            System.out.println("Error: Unable to open device");
            return status;
        }
        dev.claim_and_set();
        Dfu.setIdle(dev);
        
        if (txType.hasMediaFS()) {
            byte [] fatMediaBytes = Dfu.fetchFromDevice(dev, txType.getMediaSectorOffset()*SECTOR_SIZE, 0x200);
            if (fatMediaBytes[510] == 0x55 && ((int)fatMediaBytes[511] & 0xff) == 0xaa
                    && fatMediaBytes[54] == 0x46 && fatMediaBytes[55] == 0x41) {
                has_media = true;
            }
            //IOUtil.writeFile("fatmedia", fatMediaBytes);
        }
        byte [] fatRootBytes = DevoFat.invert(Dfu.fetchFromDevice(dev, txType.getRootSectorOffset()*SECTOR_SIZE, 0x200));
        //Magic bytes indicating FAT:
        //end of sector (510,511) must be 0x55aa
        //Fat type (54,55,56,57,58) must be FAT16 (we actuallyony check the 1st 2 bytes as sufficient)
        if (fatRootBytes[510] == 0x55 && ((int)fatRootBytes[511] & 0xff) == 0xaa
                && fatRootBytes[54] == 0x46 && fatRootBytes[55] == 0x41) {
            has_root = true;
            if (txType.hasMediaFS()) {
                has_media = true;
            }
        }
        dev.close();
        //IOUtil.writeFile("fatroot", fatRootBytes);
        if (has_media && has_root) {
            status = FatStatus.ROOT_AND_MEDIA_FAT;
        } else if (has_media) {
            status = FatStatus.MEDIA_FAT;
        } else if  (has_root) {
            status = FatStatus.ROOT_FAT;
        }
        return status;
    }
}
