package deviation.filesystem;

import deviation.Transmitter;

public class FSStatus {
	public static final int MEDIA_FS = 1;
	public static final int ROOT_FS  = 2;
	
	private Transmitter tx;
	private boolean formattedRoot;
	private boolean formattedMedia;

	public FSStatus(Transmitter tx, boolean formattedRoot, boolean formattedMedia) {
		this.tx = tx;
		this.formattedRoot = formattedRoot;
		this.formattedMedia = formattedMedia;
	}
	public boolean isFormatted() {
		return (tx != null) && formattedRoot && (! tx.hasMediaFS() || formattedMedia);
	}
	public boolean isRootFormatted() {
		return (tx != null) && formattedRoot;
	}
	public boolean isMediaFormatted() {
		return (tx !=null) && (! tx.hasMediaFS() || formattedMedia);
	}
	public boolean hasMediaFS() {
		return (tx != null) && tx.hasMediaFS() && formattedMedia;
	}
	public static FSStatus unformatted() {
		return new FSStatus(null, false, false);
	}
}
