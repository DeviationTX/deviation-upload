package deviation;
import java.io.*;
import java.util.*;

public class DeviationVersion
{
    private String name = "Unknown";
    private String version = "Unknown";
    public DeviationVersion() {
        Properties p = new Properties();
        try {
            InputStream is = DeviationUploader.class.getResourceAsStream("/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("product.version", "Unknown");
                name    = p.getProperty("product.name", "Unknown");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String name() { return name; }
    public String version() { return version; }
}
