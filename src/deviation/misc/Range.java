package deviation.misc;

import java.util.List;

public class Range {
	final long start;
	final long end;
	Range(long start, long end) {
		this.start = start;
		this.end = end;
	}
	public long start() { return start; }
	public long end() { return end; }
	public long size() { return end + 1 - start; }
	boolean contains(long value) {
		return value >= start && value <= end;
	}
	public static int getIndex(List<Range> ranges, long value) {
		for (int i = 0; i < ranges.size(); i++) {
			if (ranges.get(i).contains(value)) {
				return i;
			}
		}
		return -1;
	}
	public static void createSequentialRanges(List<Range> ranges, long start, long size, int count) {
		for (int i = 0; i < count; i++) {
			ranges.add(new Range(start, start + size - 1));
			start += size;
		}
	}
}
