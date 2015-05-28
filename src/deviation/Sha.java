package deviation;

import java.security.*;
public class Sha {
    public static String hash(String method, byte data[]) {
    	try {
    		MessageDigest md = MessageDigest.getInstance(method);
    		md.update(data);
    		return bytesToHex(md.digest());
    	} catch (Exception e) { System.out.println(e); }
    	return null;
    }
    public static String hash256(byte data[]) {
    	return hash("SHA-256", data);
    }
    public static String md5(byte data[]) {
    	return hash("MD5", data);
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }
}