package deviation;

public class Sector {
        private long start;
        private long end;
        private long size;
        private int count;
        private boolean readable;
        private boolean erasable;
        private boolean writable;
        public Sector(long start, long end, long size, int count, boolean readable, boolean erasable, boolean writable) {
            this.start = start;
            this.end   = end;
            this.size  = size;
            this.count = count;
            this.readable = readable;
            this.erasable = erasable;
            this.writable = writable;
        }
        public long start() { return start; }
        public long end()   { return end; }
        public long size()  { return size; }
        public int count()  { return count; }
        public boolean erasable() { return erasable; }
        public boolean writable() { return writable; }
        public boolean readable() { return readable; }
};
