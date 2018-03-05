package lib.aptacluster;


import com.koloboke.collect.impl.AbstractEntry;
import com.koloboke.collect.impl.AbstractSetView;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import com.koloboke.collect.impl.CommonObjCollectionOps;
import java.util.ConcurrentModificationException;
import java.util.function.Consumer;
import com.koloboke.collect.Equivalence;
import com.koloboke.collect.hash.HashConfig;
import com.koloboke.collect.impl.hash.HashConfigWrapper;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.impl.IntArrays;
import com.koloboke.collect.impl.InternalObjCollectionOps;
import com.koloboke.collect.impl.hash.LHash;
import com.koloboke.collect.impl.hash.LHashCapacities;
import java.util.Map;
import com.koloboke.collect.impl.Maths;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import java.util.NoSuchElementException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.koloboke.collect.ObjCollection;
import com.koloboke.collect.ObjCursor;
import com.koloboke.collect.ObjIterator;
import com.koloboke.collect.set.ObjSet;
import java.util.function.Predicate;
import com.koloboke.collect.impl.Primitives;
import java.util.Random;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ThreadLocalRandom;

public class Buckets {

	public static Buckets withExpectedSize(int expectedSize) {
        return new Buckets(expectedSize);
    }
	
	Buckets(int expectedSize) {
        this.init(DEFAULT_CONFIG_WRAPPER, expectedSize);
    }
	
    static void verifyConfig(HashConfig config) {
        if ((config.getGrowthFactor()) != 2.0) {
            throw new IllegalArgumentException(((((((config + " passed, HashConfig for a hashtable\n") + "implementation with linear probing must have growthFactor of 2.0.\n") + "A Koloboke Compile-generated hashtable implementation could have\n") + "a different growth factor, if the implemented type is annotated with\n") + "@com.koloboke.compile.hash.algo.openaddressing.QuadraticProbing or\n") + "@com.koloboke.compile.hash.algo.openaddressing.DoubleHashing"));
        } 
    }

    @Nonnull
    public final HashConfig hashConfig() {
        return configWrapper().config();
    }

    int freeValue;

    MutableIntList[] values;

    int[] set;

    public final boolean isEmpty() {
        return (size()) == 0;
    }

    private HashConfigWrapper configWrapper;

    int size;

    private int maxSize;

    private int modCount = 0;

    public int capacity() {
        return set.length;
    }

    public final double currentLoad() {
        return ((double) (size())) / ((double) (capacity()));
    }

    final void init(HashConfigWrapper configWrapper, int size, int freeValue) {
        this.freeValue = freeValue;
        init(configWrapper, size);
    }

    boolean nullableValueEquals(@Nullable
    MutableIntList a, @Nullable
    MutableIntList b) {
        return (a == b) || ((a != null) && (valueEquals(a, b)));
    }

    public final HashConfigWrapper configWrapper() {
        return configWrapper;
    }

    boolean valueEquals(@Nonnull
    MutableIntList a, @Nullable
    MutableIntList b) {
        return a.equals(b);
    }

    public final int size() {
        return size;
    }

    int nullableValueHashCode(@Nullable
    MutableIntList value) {
        return value != null ? valueHashCode(value) : 0;
    }

    public final int modCount() {
        return modCount;
    }

    int valueHashCode(@Nonnull
    MutableIntList value) {
        return value.hashCode();
    }

    final void incrementModCount() {
        (modCount)++;
    }

    @Nonnull
    public Equivalence<MutableIntList> valueEquivalence() {
        return Equivalence.defaultEquality();
    }

    public boolean containsEntry(int key, Object value) {
        int index = index(key);
        if (index >= 0) {
            return nullableValueEquals(values[index], ((MutableIntList) (value)));
        } else {
            return false;
        }
    }

    public boolean contains(int key) {
        return (index(key)) >= 0;
    }

    int index(int key) {
        int free;
        if (key != (free = freeValue)) {
            int[] keys = set;
            int capacityMask;
            int index;
            int cur;
            if ((cur = keys[(index = (LHash.SeparateKVIntKeyMixing.mix(key)) & (capacityMask = (keys.length) - 1))]) == key) {
                return index;
            } else {
                if (cur == free) {
                    return -1;
                } else {
                    while (true) {
                        if ((cur = keys[(index = (index - 1) & capacityMask)]) == key) {
                            return index;
                        } else if (cur == free) {
                            return -1;
                        } 
                    }
                }
            }
        } else {
            return -1;
        }
    }

    final void init(HashConfigWrapper configWrapper, int size) {
        Buckets.verifyConfig(configWrapper.config());
        Buckets.this.configWrapper = configWrapper;
        Buckets.this.size = 0;
        internalInit(targetCapacity(size));
    }

    private void internalInit(int capacity) {
        assert Maths.isPowerOf2(capacity);
        maxSize = maxSize(capacity);
        allocateArrays(capacity);
    }

    private int maxSize(int capacity) {
        return !(isMaxCapacity(capacity)) ? configWrapper.maxSize(capacity) : capacity - 1;
    }

    public MutableIntList get(int key) {
        int index = index(key);
        if (index >= 0) {
            return values[index];
        } else {
            return null;
        }
    }

    private int findNewFreeOrRemoved() {
        int free = Buckets.this.freeValue;
        Random random = ThreadLocalRandom.current();
        int newFree;
        {
            do {
                newFree = ((int) (random.nextInt()));
            } while ((newFree == free) || ((index(newFree)) >= 0) );
        }
        return newFree;
    }

    int changeFree() {
        int mc = modCount();
        int newFree = findNewFreeOrRemoved();
        incrementModCount();
        mc++;
        IntArrays.replaceAll(set, freeValue, newFree);
        Buckets.this.freeValue = newFree;
        if (mc != (modCount()))
            throw new ConcurrentModificationException();
        
        return newFree;
    }

    int insert(int key, MutableIntList value) {
        int free;
        if (key == (free = freeValue)) {
            free = changeFree();
        } 
        int[] keys = set;
        int capacityMask;
        int index;
        int cur;
        keyAbsent : if ((cur = keys[(index = (LHash.SeparateKVIntKeyMixing.mix(key)) & (capacityMask = (keys.length) - 1))]) != free) {
            if (cur == key) {
                return index;
            } else {
                while (true) {
                    if ((cur = keys[(index = (index - 1) & capacityMask)]) == free) {
                        break keyAbsent;
                    } else if (cur == key) {
                        return index;
                    } 
                }
            }
        } 
        incrementModCount();
        keys[index] = key;
        values[index] = value;
        postInsertHook();
        return -1;
    }

    final void initForRehash(int newCapacity) {
        (modCount)++;
        internalInit(newCapacity);
    }

    private void _MutableSeparateKVIntLHashSO_allocateArrays(int capacity) {
        set = new int[capacity];
        if ((freeValue) != 0)
            Arrays.fill(set, freeValue);
        
    }

    private void _MutableLHash_clear() {
        (modCount)++;
        size = 0;
    }

    private void _MutableSeparateKVIntLHashSO_clear() {
        _MutableLHash_clear();
        Arrays.fill(set, freeValue);
    }

    public boolean shrink() {
        int newCapacity = targetCapacity(size);
        if (newCapacity < (capacity())) {
            rehash(newCapacity);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings(value = "unchecked")
    void allocateArrays(int capacity) {
        _MutableSeparateKVIntLHashSO_allocateArrays(capacity);
        values = ((MutableIntList[]) (new MutableIntList[capacity]));
    }

    private void _MutableLHashSeparateKVIntObjMapSO_clear() {
        _MutableSeparateKVIntLHashSO_clear();
        Arrays.fill(values, null);
    }

    final void postRemoveHook() {
        (size)--;
    }

    final void postInsertHook() {
        if ((++(size)) > (maxSize)) {
            int capacity = capacity();
            if (!(isMaxCapacity(capacity))) {
                rehash((capacity << 1));
            } 
        } 
    }

    boolean doubleSizedArrays() {
        return false;
    }

    private int targetCapacity(int size) {
        return LHashCapacities.capacity(configWrapper, size, doubleSizedArrays());
    }

    private boolean isMaxCapacity(int capacity) {
        return LHashCapacities.isMaxCapacity(capacity, doubleSizedArrays());
    }

    @Nonnull
    public HashObjSet<Map.Entry<Integer, MutableIntList>> entrySet() {
        return new Buckets.EntryView();
    }

    @SuppressFBWarnings(value = "EC_UNRELATED_TYPES_USING_POINTER_EQUALITY")
    @Override
    public String toString() {
        if (Buckets.this.isEmpty())
            return "{}";
        
        StringBuilder sb = new StringBuilder();
        int elementCount = 0;
        int mc = modCount();
        int free = freeValue;
        int[] keys = set;
        MutableIntList[] vals = values;
        for (int i = (keys.length) - 1; i >= 0; i--) {
            int key;
            if ((key = keys[i]) != free) {
                sb.append(' ');
                sb.append(key);
                sb.append('=');
                Object val = vals[i];
                sb.append((val != ((Object) (Buckets.this)) ? val : "(this Map)"));
                sb.append(',');
                if ((++elementCount) == 8) {
                    int expectedLength = (sb.length()) * ((size()) / 8);
                    sb.ensureCapacity((expectedLength + (expectedLength / 2)));
                } 
            } 
        }
        if (mc != (modCount()))
            throw new ConcurrentModificationException();
        
        sb.setCharAt(0, '{');
        sb.setCharAt(((sb.length()) - 1), '}');
        return sb.toString();
    }

    void rehash(int newCapacity) {
        int mc = modCount();
        int free = freeValue;
        int[] keys = set;
        MutableIntList[] vals = values;
        initForRehash(newCapacity);
        mc++;
        int[] newKeys = set;
        int capacityMask = (newKeys.length) - 1;
        MutableIntList[] newVals = values;
        for (int i = (keys.length) - 1; i >= 0; i--) {
            int key;
            if ((key = keys[i]) != free) {
                int index;
                if ((newKeys[(index = (LHash.SeparateKVIntKeyMixing.mix(key)) & capacityMask)]) != free) {
                    while (true) {
                        if ((newKeys[(index = (index - 1) & capacityMask)]) == free) {
                            break;
                        } 
                    }
                } 
                newKeys[index] = key;
                newVals[index] = vals[i];
            } 
        }
        if (mc != (modCount()))
            throw new ConcurrentModificationException();
        
    }

    public void justPut(int key, MutableIntList value) {
        int index = insert(key, value);
        if (index < 0) {
            return ;
        } else {
            values[index] = value;
            return ;
        }
    }

    public void clear() {
        doClear();
    }

    private void doClear() {
        int mc = (modCount()) + 1;
        _MutableLHashSeparateKVIntObjMapSO_clear();
        if (mc != (modCount()))
            throw new ConcurrentModificationException();
        
    }

    public boolean justRemove(int key) {
        int free;
        if (key != (free = freeValue)) {
            int[] keys = set;
            int capacityMask = (keys.length) - 1;
            int index;
            int cur;
            keyPresent : if ((cur = keys[(index = (LHash.SeparateKVIntKeyMixing.mix(key)) & capacityMask)]) != key) {
                if (cur == free) {
                    return false;
                } else {
                    while (true) {
                        if ((cur = keys[(index = (index - 1) & capacityMask)]) == key) {
                            break keyPresent;
                        } else if (cur == free) {
                            return false;
                        } 
                    }
                }
            } 
            MutableIntList[] vals = values;
            incrementModCount();
            int indexToRemove = index;
            int indexToShift = indexToRemove;
            int shiftDistance = 1;
            while (true) {
                indexToShift = (indexToShift - 1) & capacityMask;
                int keyToShift;
                if ((keyToShift = keys[indexToShift]) == free) {
                    break;
                } 
                if ((((LHash.SeparateKVIntKeyMixing.mix(keyToShift)) - indexToShift) & capacityMask) >= shiftDistance) {
                    keys[indexToRemove] = keyToShift;
                    vals[indexToRemove] = vals[indexToShift];
                    indexToRemove = indexToShift;
                    shiftDistance = 1;
                } else {
                    shiftDistance++;
                    if (indexToShift == (1 + index)) {
                        throw new ConcurrentModificationException();
                    } 
                }
            }
            keys[indexToRemove] = free;
            vals[indexToRemove] = null;
            postRemoveHook();
            return true;
        } else {
            return false;
        }
    }

    public boolean remove(int key, Object value) {
        int free;
        if (key != (free = freeValue)) {
            int[] keys = set;
            int capacityMask = (keys.length) - 1;
            int index;
            int cur;
            keyPresent : if ((cur = keys[(index = (LHash.SeparateKVIntKeyMixing.mix(key)) & capacityMask)]) != key) {
                if (cur == free) {
                    return false;
                } else {
                    while (true) {
                        if ((cur = keys[(index = (index - 1) & capacityMask)]) == key) {
                            break keyPresent;
                        } else if (cur == free) {
                            return false;
                        } 
                    }
                }
            } 
            MutableIntList[] vals = values;
            if (nullableValueEquals(vals[index], ((MutableIntList) (value)))) {
                incrementModCount();
                int indexToRemove = index;
                int indexToShift = indexToRemove;
                int shiftDistance = 1;
                while (true) {
                    indexToShift = (indexToShift - 1) & capacityMask;
                    int keyToShift;
                    if ((keyToShift = keys[indexToShift]) == free) {
                        break;
                    } 
                    if ((((LHash.SeparateKVIntKeyMixing.mix(keyToShift)) - indexToShift) & capacityMask) >= shiftDistance) {
                        keys[indexToRemove] = keyToShift;
                        vals[indexToRemove] = vals[indexToShift];
                        indexToRemove = indexToShift;
                        shiftDistance = 1;
                    } else {
                        shiftDistance++;
                        if (indexToShift == (1 + index)) {
                            throw new ConcurrentModificationException();
                        } 
                    }
                }
                keys[indexToRemove] = free;
                vals[indexToRemove] = null;
                postRemoveHook();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    void closeDelayedRemoved(int firstDelayedRemoved, int delayedRemoved) {
        int free = freeValue;
        int[] keys = set;
        MutableIntList[] vals = values;
        int capacityMask = (keys.length) - 1;
        for (int i = firstDelayedRemoved; i >= 0; i--) {
            if ((keys[i]) == delayedRemoved) {
                int indexToRemove = i;
                int indexToShift = indexToRemove;
                int shiftDistance = 1;
                while (true) {
                    indexToShift = (indexToShift - 1) & capacityMask;
                    int keyToShift;
                    if ((keyToShift = keys[indexToShift]) == free) {
                        break;
                    } 
                    if ((keyToShift != delayedRemoved) && ((((LHash.SeparateKVIntKeyMixing.mix(keyToShift)) - indexToShift) & capacityMask) >= shiftDistance)) {
                        keys[indexToRemove] = keyToShift;
                        vals[indexToRemove] = vals[indexToShift];
                        indexToRemove = indexToShift;
                        shiftDistance = 1;
                    } else {
                        shiftDistance++;
                        if (indexToShift == (1 + i)) {
                            throw new ConcurrentModificationException();
                        } 
                    }
                }
                keys[indexToRemove] = free;
                vals[indexToRemove] = null;
                postRemoveHook();
            } 
        }
    }

    class EntryView extends AbstractSetView<Map.Entry<Integer, MutableIntList>> implements HashObjSet<Map.Entry<Integer, MutableIntList>> , InternalObjCollectionOps<Map.Entry<Integer, MutableIntList>> {
        @Nonnull
        @Override
        public Equivalence<Map.Entry<Integer, MutableIntList>> equivalence() {
            return Equivalence.entryEquivalence(Equivalence.<Integer>defaultEquality(), valueEquivalence());
        }

        @Nonnull
        @Override
        public HashConfig hashConfig() {
            return Buckets.this.hashConfig();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public double currentLoad() {
            return Buckets.this.currentLoad();
        }

        @Override
        @SuppressWarnings(value = "unchecked")
        public boolean contains(Object o) {
            try {
                Map.Entry<Integer, MutableIntList> e = ((Map.Entry<Integer, MutableIntList>) (o));
                return containsEntry(e.getKey(), e.getValue());
            } catch (NullPointerException e) {
                return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        @Nonnull
        public final Object[] toArray() {
            int size = size();
            Object[] result = new Object[size];
            if (size == 0)
                return result;
            
            int resultIndex = 0;
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    result[(resultIndex++)] = new Buckets.MutableEntry(mc, i, key, vals[i]);
                } 
            }
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            return result;
        }

        @Override
        @SuppressWarnings(value = "unchecked")
        @Nonnull
        public final <T>  T[] toArray(@Nonnull
        T[] a) {
            int size = size();
            if ((a.length) < size) {
                Class<?> elementType = a.getClass().getComponentType();
                a = ((T[]) (Array.newInstance(elementType, size)));
            } 
            if (size == 0) {
                if ((a.length) > 0)
                    a[0] = null;
                
                return a;
            } 
            int resultIndex = 0;
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    a[(resultIndex++)] = ((T) (new Buckets.MutableEntry(mc, i, key, vals[i])));
                } 
            }
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            if ((a.length) > resultIndex)
                a[resultIndex] = null;
            
            return a;
        }

        @Override
        public final void forEach(@Nonnull
        Consumer<? super Map.Entry<Integer, MutableIntList>> action) {
            if (action == null)
                throw new NullPointerException();
            
            if (Buckets.EntryView.this.isEmpty())
                return ;
            
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    action.accept(new Buckets.MutableEntry(mc, i, key, vals[i]));
                } 
            }
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
        }

        @Override
        public boolean forEachWhile(@Nonnull
        Predicate<? super Map.Entry<Integer, MutableIntList>> predicate) {
            if (predicate == null)
                throw new NullPointerException();
            
            if (Buckets.EntryView.this.isEmpty())
                return true;
            
            boolean terminated = false;
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    if (!(predicate.test(new Buckets.MutableEntry(mc, i, key, vals[i])))) {
                        terminated = true;
                        break;
                    } 
                } 
            }
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            return !terminated;
        }

        @Override
        @Nonnull
        public ObjIterator<Map.Entry<Integer, MutableIntList>> iterator() {
            int mc = modCount();
            return new Buckets.NoRemovedEntryIterator(mc);
        }

        @Nonnull
        @Override
        public ObjCursor<Map.Entry<Integer, MutableIntList>> cursor() {
            int mc = modCount();
            return new Buckets.NoRemovedEntryCursor(mc);
        }

        @Override
        public final boolean containsAll(@Nonnull
        Collection<?> c) {
            return CommonObjCollectionOps.containsAll(Buckets.EntryView.this, c);
        }

        @Override
        public final boolean allContainingIn(ObjCollection<?> c) {
            if (Buckets.EntryView.this.isEmpty())
                return true;
            
            boolean containsAll = true;
            Buckets.ReusableEntry e = new Buckets.ReusableEntry();
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    if (!(c.contains(e.with(key, vals[i])))) {
                        containsAll = false;
                        break;
                    } 
                } 
            }
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            return containsAll;
        }

        @Override
        public boolean reverseRemoveAllFrom(ObjSet<?> s) {
            if ((Buckets.EntryView.this.isEmpty()) || (s.isEmpty()))
                return false;
            
            boolean changed = false;
            Buckets.ReusableEntry e = new Buckets.ReusableEntry();
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    changed |= s.remove(e.with(key, vals[i]));
                } 
            }
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            return changed;
        }

        @Override
        public final boolean reverseAddAllTo(ObjCollection<? super Map.Entry<Integer, MutableIntList>> c) {
            if (Buckets.EntryView.this.isEmpty())
                return false;
            
            boolean changed = false;
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    changed |= c.add(new Buckets.MutableEntry(mc, i, key, vals[i]));
                } 
            }
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            return changed;
        }

        public int hashCode() {
            return Buckets.this.hashCode();
        }

        @SuppressFBWarnings(value = "EC_UNRELATED_TYPES_USING_POINTER_EQUALITY")
        @Override
        public String toString() {
            if (Buckets.EntryView.this.isEmpty())
                return "[]";
            
            StringBuilder sb = new StringBuilder();
            int elementCount = 0;
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    sb.append(' ');
                    sb.append(key);
                    sb.append('=');
                    Object val = vals[i];
                    sb.append((val != ((Object) (Buckets.EntryView.this)) ? val : "(this Collection)"));
                    sb.append(',');
                    if ((++elementCount) == 8) {
                        int expectedLength = (sb.length()) * ((size()) / 8);
                        sb.ensureCapacity((expectedLength + (expectedLength / 2)));
                    } 
                } 
            }
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            sb.setCharAt(0, '[');
            sb.setCharAt(((sb.length()) - 1), ']');
            return sb.toString();
        }

        @Override
        public boolean shrink() {
            return Buckets.this.shrink();
        }

        @Override
        @SuppressWarnings(value = "unchecked")
        public boolean remove(Object o) {
            try {
                Map.Entry<Integer, MutableIntList> e = ((Map.Entry<Integer, MutableIntList>) (o));
                int key = e.getKey();
                MutableIntList value = e.getValue();
                return Buckets.this.remove(key, value);
            } catch (NullPointerException e) {
                return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public final boolean removeIf(@Nonnull
        Predicate<? super Map.Entry<Integer, MutableIntList>> filter) {
            if (filter == null)
                throw new NullPointerException();
            
            if (Buckets.EntryView.this.isEmpty())
                return false;
            
            boolean changed = false;
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            int capacityMask = (keys.length) - 1;
            int firstDelayedRemoved = -1;
            int delayedRemoved = 0;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    if (filter.test(new Buckets.MutableEntry(mc, i, key, vals[i]))) {
                        incrementModCount();
                        mc++;
                        closeDeletion : if (firstDelayedRemoved < 0) {
                            int indexToRemove = i;
                            int indexToShift = indexToRemove;
                            int shiftDistance = 1;
                            while (true) {
                                indexToShift = (indexToShift - 1) & capacityMask;
                                int keyToShift;
                                if ((keyToShift = keys[indexToShift]) == free) {
                                    break;
                                } 
                                if ((((LHash.SeparateKVIntKeyMixing.mix(keyToShift)) - indexToShift) & capacityMask) >= shiftDistance) {
                                    if (indexToShift > indexToRemove) {
                                        firstDelayedRemoved = i;
                                        delayedRemoved = key;
                                        keys[indexToRemove] = key;
                                        break closeDeletion;
                                    } 
                                    if (indexToRemove == i) {
                                        i++;
                                    } 
                                    keys[indexToRemove] = keyToShift;
                                    vals[indexToRemove] = vals[indexToShift];
                                    indexToRemove = indexToShift;
                                    shiftDistance = 1;
                                } else {
                                    shiftDistance++;
                                    if (indexToShift == (1 + i)) {
                                        throw new ConcurrentModificationException();
                                    } 
                                }
                            }
                            keys[indexToRemove] = free;
                            vals[indexToRemove] = null;
                            postRemoveHook();
                        } else {
                            keys[i] = delayedRemoved;
                        }
                        changed = true;
                    } 
                } 
            }
            if (firstDelayedRemoved >= 0) {
                closeDelayedRemoved(firstDelayedRemoved, delayedRemoved);
            } 
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            return changed;
        }

        @SuppressWarnings(value = "unchecked")
        @Override
        public final boolean removeAll(@Nonnull
        Collection<?> c) {
            if (c instanceof InternalObjCollectionOps) {
                InternalObjCollectionOps c2 = ((InternalObjCollectionOps) (c));
                if ((equivalence().equals(c2.equivalence())) && ((c2.size()) < (Buckets.EntryView.this.size()))) {
                    c2.reverseRemoveAllFrom(Buckets.EntryView.this);
                } 
            } 
            if ((Buckets.EntryView.this) == ((Object) (c)))
                throw new IllegalArgumentException();
            
            if ((Buckets.EntryView.this.isEmpty()) || (c.isEmpty()))
                return false;
            
            boolean changed = false;
            Buckets.ReusableEntry e = new Buckets.ReusableEntry();
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            int capacityMask = (keys.length) - 1;
            int firstDelayedRemoved = -1;
            int delayedRemoved = 0;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    if (c.contains(e.with(key, vals[i]))) {
                        incrementModCount();
                        mc++;
                        closeDeletion : if (firstDelayedRemoved < 0) {
                            int indexToRemove = i;
                            int indexToShift = indexToRemove;
                            int shiftDistance = 1;
                            while (true) {
                                indexToShift = (indexToShift - 1) & capacityMask;
                                int keyToShift;
                                if ((keyToShift = keys[indexToShift]) == free) {
                                    break;
                                } 
                                if ((((LHash.SeparateKVIntKeyMixing.mix(keyToShift)) - indexToShift) & capacityMask) >= shiftDistance) {
                                    if (indexToShift > indexToRemove) {
                                        firstDelayedRemoved = i;
                                        delayedRemoved = key;
                                        keys[indexToRemove] = key;
                                        break closeDeletion;
                                    } 
                                    if (indexToRemove == i) {
                                        i++;
                                    } 
                                    keys[indexToRemove] = keyToShift;
                                    vals[indexToRemove] = vals[indexToShift];
                                    indexToRemove = indexToShift;
                                    shiftDistance = 1;
                                } else {
                                    shiftDistance++;
                                    if (indexToShift == (1 + i)) {
                                        throw new ConcurrentModificationException();
                                    } 
                                }
                            }
                            keys[indexToRemove] = free;
                            vals[indexToRemove] = null;
                            postRemoveHook();
                        } else {
                            keys[i] = delayedRemoved;
                        }
                        changed = true;
                    } 
                } 
            }
            if (firstDelayedRemoved >= 0) {
                closeDelayedRemoved(firstDelayedRemoved, delayedRemoved);
            } 
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            return changed;
        }

        @Override
        public final boolean retainAll(@Nonnull
        Collection<?> c) {
            if ((Buckets.EntryView.this) == ((Object) (c)))
                throw new IllegalArgumentException();
            
            if (Buckets.EntryView.this.isEmpty())
                return false;
            
            if (c.isEmpty()) {
                clear();
                return true;
            } 
            boolean changed = false;
            Buckets.ReusableEntry e = new Buckets.ReusableEntry();
            int mc = modCount();
            int free = freeValue;
            int[] keys = set;
            int capacityMask = (keys.length) - 1;
            int firstDelayedRemoved = -1;
            int delayedRemoved = 0;
            MutableIntList[] vals = values;
            for (int i = (keys.length) - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    if (!(c.contains(e.with(key, vals[i])))) {
                        incrementModCount();
                        mc++;
                        closeDeletion : if (firstDelayedRemoved < 0) {
                            int indexToRemove = i;
                            int indexToShift = indexToRemove;
                            int shiftDistance = 1;
                            while (true) {
                                indexToShift = (indexToShift - 1) & capacityMask;
                                int keyToShift;
                                if ((keyToShift = keys[indexToShift]) == free) {
                                    break;
                                } 
                                if ((((LHash.SeparateKVIntKeyMixing.mix(keyToShift)) - indexToShift) & capacityMask) >= shiftDistance) {
                                    if (indexToShift > indexToRemove) {
                                        firstDelayedRemoved = i;
                                        delayedRemoved = key;
                                        keys[indexToRemove] = key;
                                        break closeDeletion;
                                    } 
                                    if (indexToRemove == i) {
                                        i++;
                                    } 
                                    keys[indexToRemove] = keyToShift;
                                    vals[indexToRemove] = vals[indexToShift];
                                    indexToRemove = indexToShift;
                                    shiftDistance = 1;
                                } else {
                                    shiftDistance++;
                                    if (indexToShift == (1 + i)) {
                                        throw new ConcurrentModificationException();
                                    } 
                                }
                            }
                            keys[indexToRemove] = free;
                            vals[indexToRemove] = null;
                            postRemoveHook();
                        } else {
                            keys[i] = delayedRemoved;
                        }
                        changed = true;
                    } 
                } 
            }
            if (firstDelayedRemoved >= 0) {
                closeDelayedRemoved(firstDelayedRemoved, delayedRemoved);
            } 
            if (mc != (modCount()))
                throw new ConcurrentModificationException();
            
            return changed;
        }

        @Override
        public void clear() {
            Buckets.this.doClear();
        }
    }

    abstract class IntObjEntry extends AbstractEntry<Integer, MutableIntList> {
        abstract int key();

        @Override
        public final Integer getKey() {
            return key();
        }

        abstract MutableIntList value();

        @Override
        public final MutableIntList getValue() {
            return value();
        }

        @SuppressWarnings(value = "unchecked")
        @Override
        public boolean equals(Object o) {
            Map.Entry e2;
            int k2;
            MutableIntList v2;
            try {
                e2 = ((Map.Entry) (o));
                k2 = ((Integer) (e2.getKey()));
                v2 = ((MutableIntList) (e2.getValue()));
                return ((key()) == k2) && (nullableValueEquals(v2, value()));
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return (Primitives.hashCode(key())) ^ (nullableValueHashCode(value()));
        }
    }

    class MutableEntry extends Buckets.IntObjEntry {
        final int modCount;

        private final int index;

        final int key;

        private MutableIntList value;

        MutableEntry(int modCount, int index, int key, MutableIntList value) {
            this.modCount = modCount;
            this.index = index;
            this.key = key;
            Buckets.MutableEntry.this.value = value;
        }

        @Override
        public int key() {
            return key;
        }

        @Override
        public MutableIntList value() {
            return value;
        }

        @Override
        public MutableIntList setValue(MutableIntList newValue) {
            if ((modCount) != (modCount()))
                throw new IllegalStateException();
            
            MutableIntList oldValue = value;
            MutableIntList unwrappedNewValue = newValue;
            value = unwrappedNewValue;
            updateValueInTable(unwrappedNewValue);
            return oldValue;
        }

        void updateValueInTable(MutableIntList newValue) {
            values[index] = newValue;
        }
    }

    class ReusableEntry extends Buckets.IntObjEntry {
        private int key;

        private MutableIntList value;

        Buckets.ReusableEntry with(int key, MutableIntList value) {
            Buckets.ReusableEntry.this.key = key;
            Buckets.ReusableEntry.this.value = value;
            return Buckets.ReusableEntry.this;
        }

        @Override
        public int key() {
            return key;
        }

        @Override
        public MutableIntList value() {
            return value;
        }
    }

    class NoRemovedEntryIterator implements ObjIterator<Map.Entry<Integer, MutableIntList>> {
        int[] keys;

        MutableIntList[] vals;

        final int free;

        final int capacityMask;

        int expectedModCount;

        class MutableEntry2 extends Buckets.MutableEntry {
            MutableEntry2(int modCount, int index, int key, MutableIntList value) {
                super(modCount, index, key, value);
            }

            @Override
            void updateValueInTable(MutableIntList newValue) {
                if ((vals) == (values)) {
                    vals[index] = newValue;
                } else {
                    justPut(key, newValue);
                    if ((Buckets.NoRemovedEntryIterator.MutableEntry2.this.modCount) != (modCount())) {
                        throw new IllegalStateException();
                    } 
                }
            }
        }

        int index = -1;

        int nextIndex;

        Buckets.MutableEntry next;

        NoRemovedEntryIterator(int mc) {
            expectedModCount = mc;
            int[] keys = Buckets.NoRemovedEntryIterator.this.keys = set;
            capacityMask = (keys.length) - 1;
            MutableIntList[] vals = Buckets.NoRemovedEntryIterator.this.vals = values;
            int free = this.free = freeValue;
            int nextI = keys.length;
            while ((--nextI) >= 0) {
                int key;
                if ((key = keys[nextI]) != free) {
                    next = new Buckets.NoRemovedEntryIterator.MutableEntry2(mc, nextI, key, vals[nextI]);
                    break;
                } 
            }
            nextIndex = nextI;
        }

        @Override
        public void forEachRemaining(@Nonnull
        Consumer<? super Map.Entry<Integer, MutableIntList>> action) {
            if (action == null)
                throw new NullPointerException();
            
            int mc = expectedModCount;
            int[] keys = Buckets.NoRemovedEntryIterator.this.keys;
            MutableIntList[] vals = Buckets.NoRemovedEntryIterator.this.vals;
            int free = Buckets.NoRemovedEntryIterator.this.free;
            int nextI = nextIndex;
            for (int i = nextI; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    action.accept(new Buckets.NoRemovedEntryIterator.MutableEntry2(mc, i, key, vals[i]));
                } 
            }
            if ((nextI != (nextIndex)) || (mc != (modCount()))) {
                throw new ConcurrentModificationException();
            } 
            index = nextIndex = -1;
        }

        @Override
        public boolean hasNext() {
            return (nextIndex) >= 0;
        }

        @Override
        public Map.Entry<Integer, MutableIntList> next() {
            int mc;
            if ((mc = expectedModCount) == (modCount())) {
                int nextI;
                if ((nextI = nextIndex) >= 0) {
                    index = nextI;
                    int[] keys = Buckets.NoRemovedEntryIterator.this.keys;
                    int free = Buckets.NoRemovedEntryIterator.this.free;
                    Buckets.MutableEntry prev = next;
                    while ((--nextI) >= 0) {
                        int key;
                        if ((key = keys[nextI]) != free) {
                            next = new Buckets.NoRemovedEntryIterator.MutableEntry2(mc, nextI, key, vals[nextI]);
                            break;
                        } 
                    }
                    nextIndex = nextI;
                    return prev;
                } else {
                    throw new NoSuchElementException();
                }
            } else {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void remove() {
            int index;
            if ((index = Buckets.NoRemovedEntryIterator.this.index) >= 0) {
                if (((expectedModCount)++) == (modCount())) {
                    Buckets.NoRemovedEntryIterator.this.index = -1;
                    int[] keys = Buckets.NoRemovedEntryIterator.this.keys;
                    MutableIntList[] vals = Buckets.NoRemovedEntryIterator.this.vals;
                    if (keys == (set)) {
                        int capacityMask = Buckets.NoRemovedEntryIterator.this.capacityMask;
                        incrementModCount();
                        int indexToRemove = index;
                        int indexToShift = indexToRemove;
                        int shiftDistance = 1;
                        while (true) {
                            indexToShift = (indexToShift - 1) & capacityMask;
                            int keyToShift;
                            if ((keyToShift = keys[indexToShift]) == (free)) {
                                break;
                            } 
                            if ((((LHash.SeparateKVIntKeyMixing.mix(keyToShift)) - indexToShift) & capacityMask) >= shiftDistance) {
                                if ((Buckets.NoRemovedEntryIterator.this.keys) == keys) {
                                    if (indexToShift > indexToRemove) {
                                        int slotsToCopy;
                                        if ((slotsToCopy = (nextIndex) + 1) > 0) {
                                            Buckets.NoRemovedEntryIterator.this.keys = Arrays.copyOf(keys, slotsToCopy);
                                            Buckets.NoRemovedEntryIterator.this.vals = Arrays.copyOf(vals, slotsToCopy);
                                            if (indexToRemove < slotsToCopy) {
                                                Buckets.NoRemovedEntryIterator.this.keys[indexToRemove] = free;
                                                Buckets.NoRemovedEntryIterator.this.vals[indexToRemove] = null;
                                            } 
                                        } 
                                    } else if (indexToRemove == index) {
                                        Buckets.NoRemovedEntryIterator.this.nextIndex = index;
                                        if (indexToShift < (index - 1)) {
                                            Buckets.NoRemovedEntryIterator.this.next = new Buckets.NoRemovedEntryIterator.MutableEntry2(modCount(), indexToShift, keyToShift, vals[indexToShift]);
                                        } 
                                    } 
                                } 
                                keys[indexToRemove] = keyToShift;
                                vals[indexToRemove] = vals[indexToShift];
                                indexToRemove = indexToShift;
                                shiftDistance = 1;
                            } else {
                                shiftDistance++;
                                if (indexToShift == (1 + index)) {
                                    throw new ConcurrentModificationException();
                                } 
                            }
                        }
                        keys[indexToRemove] = free;
                        vals[indexToRemove] = null;
                        postRemoveHook();
                    } else {
                        justRemove(keys[index]);
                        vals[index] = null;
                    }
                } else {
                    throw new ConcurrentModificationException();
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    class NoRemovedEntryCursor implements ObjCursor<Map.Entry<Integer, MutableIntList>> {
        int[] keys;

        MutableIntList[] vals;

        final int free;

        final int capacityMask;

        int expectedModCount;

        class MutableEntry2 extends Buckets.MutableEntry {
            MutableEntry2(int modCount, int index, int key, MutableIntList value) {
                super(modCount, index, key, value);
            }

            @Override
            void updateValueInTable(MutableIntList newValue) {
                if ((vals) == (values)) {
                    vals[index] = newValue;
                } else {
                    justPut(key, newValue);
                    if ((Buckets.NoRemovedEntryCursor.MutableEntry2.this.modCount) != (modCount())) {
                        throw new IllegalStateException();
                    } 
                }
            }
        }

        int index;

        int curKey;

        MutableIntList curValue;

        NoRemovedEntryCursor(int mc) {
            expectedModCount = mc;
            int[] keys = Buckets.NoRemovedEntryCursor.this.keys = set;
            capacityMask = (keys.length) - 1;
            index = keys.length;
            vals = values;
            int free = this.free = freeValue;
            curKey = free;
        }

        @Override
        public void forEachForward(Consumer<? super Map.Entry<Integer, MutableIntList>> action) {
            if (action == null)
                throw new NullPointerException();
            
            int mc = expectedModCount;
            int[] keys = Buckets.NoRemovedEntryCursor.this.keys;
            MutableIntList[] vals = Buckets.NoRemovedEntryCursor.this.vals;
            int free = Buckets.NoRemovedEntryCursor.this.free;
            int index = Buckets.NoRemovedEntryCursor.this.index;
            for (int i = index - 1; i >= 0; i--) {
                int key;
                if ((key = keys[i]) != free) {
                    action.accept(new Buckets.NoRemovedEntryCursor.MutableEntry2(mc, i, key, vals[i]));
                } 
            }
            if ((index != (Buckets.NoRemovedEntryCursor.this.index)) || (mc != (modCount()))) {
                throw new ConcurrentModificationException();
            } 
            Buckets.NoRemovedEntryCursor.this.index = -1;
            curKey = free;
        }

        @Override
        public Map.Entry<Integer, MutableIntList> elem() {
            int curKey;
            if ((curKey = Buckets.NoRemovedEntryCursor.this.curKey) != (free)) {
                return new Buckets.NoRemovedEntryCursor.MutableEntry2(expectedModCount, index, curKey, curValue);
            } else {
                throw new IllegalStateException();
            }
        }

        @Override
        public boolean moveNext() {
            if ((expectedModCount) == (modCount())) {
                int[] keys = Buckets.NoRemovedEntryCursor.this.keys;
                int free = Buckets.NoRemovedEntryCursor.this.free;
                for (int i = (index) - 1; i >= 0; i--) {
                    int key;
                    if ((key = keys[i]) != free) {
                        index = i;
                        curKey = key;
                        curValue = vals[i];
                        return true;
                    } 
                }
                curKey = free;
                index = -1;
                return false;
            } else {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public void remove() {
            int curKey;
            int free;
            if ((curKey = Buckets.NoRemovedEntryCursor.this.curKey) != (free = Buckets.NoRemovedEntryCursor.this.free)) {
                if (((expectedModCount)++) == (modCount())) {
                    Buckets.NoRemovedEntryCursor.this.curKey = free;
                    int index = Buckets.NoRemovedEntryCursor.this.index;
                    int[] keys = Buckets.NoRemovedEntryCursor.this.keys;
                    MutableIntList[] vals = Buckets.NoRemovedEntryCursor.this.vals;
                    if (keys == (set)) {
                        int capacityMask = Buckets.NoRemovedEntryCursor.this.capacityMask;
                        incrementModCount();
                        int indexToRemove = index;
                        int indexToShift = indexToRemove;
                        int shiftDistance = 1;
                        while (true) {
                            indexToShift = (indexToShift - 1) & capacityMask;
                            int keyToShift;
                            if ((keyToShift = keys[indexToShift]) == free) {
                                break;
                            } 
                            if ((((LHash.SeparateKVIntKeyMixing.mix(keyToShift)) - indexToShift) & capacityMask) >= shiftDistance) {
                                if ((Buckets.NoRemovedEntryCursor.this.keys) == keys) {
                                    if (indexToShift > indexToRemove) {
                                        int slotsToCopy;
                                        if ((slotsToCopy = index) > 0) {
                                            Buckets.NoRemovedEntryCursor.this.keys = Arrays.copyOf(keys, slotsToCopy);
                                            Buckets.NoRemovedEntryCursor.this.vals = Arrays.copyOf(vals, slotsToCopy);
                                            if (indexToRemove < slotsToCopy) {
                                                Buckets.NoRemovedEntryCursor.this.keys[indexToRemove] = free;
                                                Buckets.NoRemovedEntryCursor.this.vals[indexToRemove] = null;
                                            } 
                                        } 
                                    } else if (indexToRemove == index) {
                                        Buckets.NoRemovedEntryCursor.this.index = ++index;
                                    } 
                                } 
                                keys[indexToRemove] = keyToShift;
                                vals[indexToRemove] = vals[indexToShift];
                                indexToRemove = indexToShift;
                                shiftDistance = 1;
                            } else {
                                shiftDistance++;
                                if (indexToShift == (1 + index)) {
                                    throw new ConcurrentModificationException();
                                } 
                            }
                        }
                        keys[indexToRemove] = free;
                        vals[indexToRemove] = null;
                        postRemoveHook();
                    } else {
                        justRemove(curKey);
                        vals[index] = null;
                    }
                } else {
                    throw new ConcurrentModificationException();
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    Buckets(HashConfig hashConfig, int expectedSize) {
        this.init(new HashConfigWrapper(hashConfig), expectedSize);
    }

    static class Support {    }

    static final HashConfigWrapper DEFAULT_CONFIG_WRAPPER = new HashConfigWrapper(HashConfig.getDefault());
	
	
}
