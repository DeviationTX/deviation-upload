package deviation;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.yamlbeans.YamlReader;

public final class TransmitterList {

	private static List<Transmitter> transmitters;
	private static final Transmitter UNKNOWN = new Transmitter("Unknown", "", 0, Transmitter.FlashInfo.empty, Transmitter.FlashInfo.empty, null, null);;
	private TransmitterList() {}
	public static void init(URL configFile) {
		transmitters = new ArrayList<Transmitter>();
		transmitters.add(UNKNOWN);
  		try {
			YamlReader reader = new YamlReader(new InputStreamReader(configFile.openStream()));
			reader.getConfig().setPropertyElementType(Transmitter.class, "overrideSectors", Transmitter.SectorOverride.class);
			reader.getConfig().setPropertyElementType(Transmitter.SectorOverride.class, "add", Transmitter.SectorPrivate.class);
			reader.getConfig().setPropertyElementType(Transmitter.class, "matchRules", String.class);
			while(true) {
				Transmitter tx = reader.read(Transmitter.class);
				if (tx == null) {
					break;
				}
				transmitters.add(tx);
			}
		} catch(Exception e) { e.printStackTrace(); }
	}
	public static void init() {
	    init(DeviationUploader.class.getResource("/transmitters.yml"));
	} 
	public static Transmitter UNKNOWN() { return UNKNOWN; }
	public static List<Transmitter> values() { return transmitters; }
	public static Transmitter get(String str) { 
		for (Transmitter tx : transmitters) {
			if (tx.getName().equals(str)) {
				return tx;
			}
		}
		return UNKNOWN;
	}
}
