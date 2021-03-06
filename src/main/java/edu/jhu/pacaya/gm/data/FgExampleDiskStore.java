package edu.jhu.pacaya.gm.data;

import java.io.File;
import java.io.IOException;

import edu.jhu.pacaya.util.cache.CachedFastDiskStore;

/**
 * A disk-backed mutable collection of instances for a graphical model.
 * 
 * @author mgormley
 * 
 */
public class FgExampleDiskStore implements FgExampleStore {

    private CachedFastDiskStore<Integer, LFgExample> examples;

    public FgExampleDiskStore() {
        this(new File("."), true, -1);
    }

    public FgExampleDiskStore(File cacheDir, boolean gzipped, int maxEntriesInMemory) {
        try {
            File cachePath = File.createTempFile("cache", ".binary.gz", cacheDir);
            this.examples = new CachedFastDiskStore<Integer, LFgExample>(cachePath, gzipped, maxEntriesInMemory);
            // TODO: cachePath.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Adds an example. */
    public synchronized void add(LFgExample example) {
        examples.put(examples.size(), example);
    }

    /** Gets the i'th example. */
    public synchronized LFgExample get(int i) {
        return examples.get(i);
    }

    /** Gets the number of examples. */
    public synchronized int size() {
        return examples.size();
    }

    // In an old version of this class, we used the following iterator. 
    // However there was no way to ensure its thread safety. The iterator
    // in the abstract base class however, relies on get(i) which is thread 
    // safe.
    //
    //    public Iterator<FgExample> iterator() {
    //        return examples.valueIterator();
    //    }

}
