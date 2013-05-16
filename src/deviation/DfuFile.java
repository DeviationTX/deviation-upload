package deviation;

import java.io.IOException;
import java.util.*;

public class DfuFile {
    public static class ImageElement {
        private String name;
        private int altSetting;
        private long address;
        private byte[] data;
        public ImageElement(String name, int altSetting, long address, byte[] data) {
            this.name = name;
            this.altSetting = altSetting;
            this.address = address;
            this.data = data;
        }
        public long address() { return address; }
        public byte[] data() { return data; }
        public int altSetting() { return altSetting; }
        public String name() { return name; }
    }
    private int fwVersion;
    private int idProduct;
    private int idVendor;
    private List<ImageElement> imageElements;
    public DfuFile(String fname) throws IOException {
        byte[] data = IOUtil.readFile(fname);
        imageElements = new ArrayList<ImageElement>();

        final byte [] szSignature = new byte[] {'D', 'f', 'u', 'S', 'e'};
        if (! Arrays.equals(Arrays.copyOfRange(data, 0, 5), szSignature)) {
            throw new IllegalArgumentException("DFU does not contain DfuSe signature");
        }
        if (data[5] != 0x01) {
            throw new IllegalArgumentException(String.format("DFU signature '%d' should be '1'", data[5]));
        }
        //big-endian
        //long DFUImageSize = ((long)(0xff & data[9]) << 24)
        //                        | ((0xff & data[8]) << 16)
        //                        | ((0xff & data[7]) <<  8)
        //                        | ((0xff & data[6]) <<  0);
        int bTargets = 0xff & data[10]; //Number of images
        int start = 11;
        for(int i = 0; i < bTargets; i++) {
             start += parseImage(data, start);
        }
        fwVersion = (data[start] & 0xff) + 10 * (data[start+1] & 0xff);
        idProduct = ((data[start+2] & 0xff) << 0)
                  | ((data[start+3] & 0xff) << 8);
        idVendor  = ((data[start+4] & 0xff) << 0)
                  | ((data[start+5] & 0xff) << 8);
        int dfuSpec   = ((data[start+6] & 0xff) << 0)
                      | ((data[start+7] & 0xff) << 8);
        if (dfuSpec != 0x11a) {
            throw new IllegalArgumentException(String.format("DFU specificaton '0x%x' should be '0x11a'", dfuSpec));
        }
        final byte [] ucDfuSignature = new byte[] {'U', 'F', 'D'};
        if (! Arrays.equals(Arrays.copyOfRange(data, start+8, start+11), ucDfuSignature)) {
            throw new IllegalArgumentException("DFU does not contain UFD signature");
        }
        //little-endian
        long dwCRC = ((long)(0xff & data[start+15]) << 24)
                         | ((0xff & data[start+14]) << 16)
                         | ((0xff & data[start+13]) <<  8)
                         | ((0xff & data[start+12]) <<  0);
        long crc = Crc.Crc32(Arrays.copyOfRange(data, 0, data.length-4));
        if (crc != dwCRC) {
            throw new IllegalArgumentException(String.format("DFU CRC '0x%x' does not match calculated value '0x%x'", dwCRC, crc));
        }
    }
    public int parseImage(byte[] data, int offset) {
        final byte[] szSignature = new byte[] {'T', 'a', 'r', 'g', 'e', 't'};
        if (! Arrays.equals(Arrays.copyOfRange(data, offset, offset+6), szSignature)) {
            throw new IllegalArgumentException("DFU does not contain 'Target' signature");
        }
        int bAltSetting = 0xff & data[offset+6];
        String targetName = null;
        if (data[offset+7] != 0 || data[offset+8] != 0 || data[offset+9] != 0 || data[offset+10] != 0) {
            targetName = new String(Arrays.copyOfRange(data, offset+11, offset+266));
        }
        //System.out.println("Name: '" + targetName + "'");
        //long targetSize = ((long)(0xff & data[offset+269]) << 24)
        //                      | ((0xff & data[offset+268]) << 16)
        //                      | ((0xff & data[offset+267]) <<  8)
        //                      | ((0xff & data[offset+266]) <<  0);
        long numElements= ((long)(0xff & data[offset+273]) << 24)
                              | ((0xff & data[offset+272]) << 16)
                              | ((0xff & data[offset+271]) <<  8)
                              | ((0xff & data[offset+270]) <<  0);
        int start = offset + 274;
        for (int i = 0; i < numElements; i++) {
            long address = ((long)(0xff & data[start+3]) << 24)
                               | ((0xff & data[start+2]) << 16)
                               | ((0xff & data[start+1]) <<  8)
                               | ((0xff & data[start+0]) <<  0);
            int length  = ((0xff & data[start+7]) << 24)
                        | ((0xff & data[start+6]) << 16)
                        | ((0xff & data[start+5]) <<  8)
                        | ((0xff & data[start+4]) <<  0);
            //System.out.format("size: %d count: %d 0x%x %d\n", targetSize, numElements, address, length);
            start += 8;
            imageElements.add(new ImageElement(targetName, bAltSetting, address, Arrays.copyOfRange(data, start, start+length)));
            start += length;
        }
        return start - offset;
    }
    public int fwVersion() { return fwVersion;}
    public int idProduct() { return idProduct;}
    public int idVendor()  { return idVendor;}
    public List<ImageElement> imageElements() { return imageElements;}
}

