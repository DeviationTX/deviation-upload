package deviation;
import java.util.prefs.*;

public class UploaderPreferences {
	private static Preferences prefs = null;
    public UploaderPreferences() {
    	if (prefs == null) {
    	    prefs = Preferences.userNodeForPackage(deviation.DeviationUploader.class);
    	}
    }
    public void put(String key, String value) {
    	prefs.put(key, value);
    }
    public String get(String key, String defaultValue) {
    	return prefs.get(key,  defaultValue);
    }
    public String get(String key) {
    	return get(key, "");
    }
}
