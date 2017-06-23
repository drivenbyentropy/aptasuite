package lib.aptacluster;

import java.util.Map.Entry;
import org.eclipse.collections.api.list.primitive.MutableIntList;

import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.compile.KolobokeMap;

/**
 * @author Jan Hoinka
 * This class implements a specialized and space efficient map
 * for storing the hashing information resulting from a LSH run.
 * It uses only primitives avoiding expensive overhead.
 */
@KolobokeMap
public abstract class Buckets {
    
	public static Buckets withExpectedSize(int expectedSize) {
        return new KolobokeBuckets(expectedSize);
    }

	public abstract void justPut(int key, MutableIntList value);

	public abstract boolean justRemove(int key);
	
	public abstract MutableIntList get(int key);
	
	public abstract boolean contains(int key);

	public abstract void clear();

	public abstract int size();
	
	public abstract HashObjSet<Entry<Integer, MutableIntList>> entrySet();

}
