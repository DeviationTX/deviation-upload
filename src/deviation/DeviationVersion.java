import java.io.*;
import java.util.*;

public class DeviationVersion
{
    private String name;
    private String version;
    public DeviationVersion() {
        Properties p = new Properties();
        try {
            InputStream is = DeviationUploader.class.getResourceAsStream("pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("product.version", "Unknown");
                name    = p.getProperty("product.name", "Unknown");
            }
        } catch (Exception e) {
            name = null;
            version = null;
        }
    }
    public String name() { return name; }
    public String version() { return version; }
}
