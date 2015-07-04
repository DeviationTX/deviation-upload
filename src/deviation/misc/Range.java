package deviation.misc;

import java.util.List;

public class Range {
	final int start;
	final int end;
	Range(int start, int end) {
		this.start = start;
		this.end = end;
	}
	public int start() { return start; }
	public int end() { return end; }
	public int size() { return end + 1 - start; }
	boolean contains(int value) {
		return value >= start && value <= end;
	}
	public static int getIndex(List<Range> ranges, int value) {
		for (int i = 0; i < ranges.size(); i++) {
			if (ranges.get(i).contains(value)) {
				return i;
			}
		}
		return -1;
	}
	public static void createSequentialRanges(List<Range> ranges, int start, int size, int count) {
		for (int i = 0; i < count; i++) {
			ranges.add(new Range(start, start + size - 1));
			start += size;
		}
	}
}
