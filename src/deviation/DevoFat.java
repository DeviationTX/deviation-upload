package deviation;

public class DevoFat {
    public enum FatStatus {NO_FAT, ROOT_FAT, MEDIA_FAT, ROOT_AND_MEDIA_FAT};

    public static byte[] reverse(byte[] data) {
        int i;
        for(i = 0; i < data.length; i++) {
            int j = ~data[i]; 
            data[i] = (byte)(j&0xff);
        }
        return data;
    }

}
