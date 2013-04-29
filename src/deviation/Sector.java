
public class Sector {
        private int start;
        private int end;
        private int size;
        private int count;
        private int flags;
        private boolean readable;
        private boolean erasable;
        private boolean writable;
        public Sector(int start, int end, int size, int count, boolean readable, boolean erasable, boolean writable) {
            this.start = start;
            this.end   = end;
            this.size  = size;
            this.count = count;
            this.flags = flags;
            this.readable = readable;
            this.erasable = erasable;
            this.writable = writable;
        }
        public int start() { return start; }
        public int end()   { return end; }
        public int size()  { return size; }
        public int count()  { return count; }
        public int flags()  { return flags; }
        public boolean erasable() { return erasable; }
        public boolean writable() { return writable; }
};
