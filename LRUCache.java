package edu.njit.cs602.s2018.assignments.pastassignments;

import java.util.*;

/**
 * Edited by Justin Bullock 3/11/18
 * CS602002 Spring 2018
 *
* */



/**
 * Created by Ravi Varadarajan on 2/20/2018.
 */
public class LRUCache<K,T extends Cacheable<K> > {

    // Stores items for look up
    private final Map<K,Cacheable<K>> itemMap;
    // Stores new/modified items
    private final Persister <K,T> persister;
    // list in LRU order
    private final List<K> lruList;
    // Size of itemMap (cache)
    private int cacheSize;
    private double totalAccesses = 0;
    private double totalFaults = 0;


    /**
     * To be implemented!!
     * Iterator class only for only keys of cached items; order should be in LRU order, most recently used first
     * @param <K>
     */
    private class CacheKeyIterator<K> implements Iterator<K> {
        ListIterator lruIterator = lruList.listIterator();
        /**
        * Sets lruList iterator in mru order
        *
        *
        * */
        public CacheKeyIterator(){
            while(lruIterator.hasNext()){
                lruIterator.next();
            }
        }

        @Override
        public boolean hasNext() {
            if(lruIterator.hasPrevious())
                return true;
            return false;
        }

        @Override
        public K next() {
            return (K)lruIterator.previous();
        }
    }

    /**
     * Constructor
     * @param size initial size of the cache which can change later
     * @param persister persister instance to use for accessing/modifying evicted items
     */
    public LRUCache(int size, Persister<? extends K,? extends T> persister) {
        //initialize HashMap to be used by instances
        itemMap = new HashMap<>(size);
        this.persister = (Persister<K, T>) persister;
        //initialize size of map
        cacheSize = size;
        lruList = new ArrayList<>();
        totalAccesses = 1;
        totalFaults = 0;
    }

    /**
     * Get cache keys
     * @return
     */
    public Iterator<K> getCacheKeys() {
        return new CacheKeyIterator<>();
    }

    /**
     * Get LRU Item from lruList
     * @return
     * */
    public K getLruItem(){
        K lruKey;
        CacheKeyIterator<K> iterator = (CacheKeyIterator<K>) getCacheKeys();
        ArrayList<K> cacheKeys = new ArrayList<>(cacheSize);

        int i =0;
        while(cacheKeys.size()<cacheSize){//add each individual key that's in cache into cacheKeys
            if(iterator.hasNext()){
                K mruKey = iterator.next();
                if(!cacheKeys.contains(mruKey))// used item not in cacheKeys
                    cacheKeys.add(mruKey);
            }
            i++;
        }
        lruKey = cacheKeys.get(cacheKeys.size()-1);
        cacheKeys.clear();
        return lruKey;
    }

    /**
     * Get item with the key (need to get item even if evicted)
     * @param key
     * @return
     */
    public T getItem(K key) {
        totalAccesses+=1;
        if(itemMap.containsKey(key)){//item already in cache
            Cacheable<K> item = itemMap.get(key);
            lruList.add(key);
            return (T) item;
        }else{//item not already in cache
            if(persister.getValue(key) != null){//item in persister
                Cacheable<K> item = persister.getValue(key);
                if(itemMap.size() < cacheSize){//cache is not full
                    itemMap.put(key, item);
                    //update lru
                    lruList.add(key);
                }else{//cache is full
                    //remove lru
                    itemMap.remove(getLruItem());
                    itemMap.put(key, item);
                    //update lru
                    lruList.add(key);
                }
                //update page fault
                totalFaults+=1;
                return (T) item;
            }
            return null;
        }
    }

    /**
     * Add/Modify item with the key
     * @param item item to be put
     */
    public void putItem(T item) {
        totalAccesses+=1;
        persister.persistValue(item);
        if(itemMap.size() < cacheSize){//cache not full
            if(!itemMap.containsKey(item.getKey())){//item not already in cache
                itemMap.put(item.getKey(), item);//add to cache
                totalFaults+=1;

                System.out.println("itemMap DOESNT contains key: "+item);


            }else{
                System.out.println("itemMap DOES contains key: "+item);

            }
            //update lru
            lruList.add(item.getKey());
        }
        else{//cache is full
            if(itemMap.containsKey(item.getKey())){//item already in cache
                //update lru
                lruList.add(item.getKey());
            }else{//item not already in cache
                //remove mru/lru
                itemMap.remove(getLruItem());
                itemMap.put(item.getKey(), item);
                //update lru
                lruList.add(item.getKey());
                totalFaults+=1;
            }
        }
    }

    /**
     * Remove an item with the key
     * @param key
     * @return item removed or null if it does not exist
     */
    public T removeItem(K key) {
        if(itemMap.containsKey(key)){
            itemMap.remove(key);
            return persister.getValue(key);
        }else{
            return null;
        }
    }


    /**
     * Modify the cache size
     * @param newSize
     */
    public void modifySize(int newSize) {
        cacheSize = newSize;
    }


    /**
     * Get fault rate (proportion of accesses (only for retrievals and modifications) not in cache)
     * @return
     */
    public double getFaultRatePercent() {
        return 100*(totalFaults/totalAccesses);
    }

    /**
     * Reset fault rate stats counters
     */
    public void resetFaultRateStats() {
        totalFaults = 0;
        totalAccesses = 0;
    }


    public static void main(String [] args) {
        LRUCache<String,SimpleCacheItem> cache = new LRUCache<>(20, new SimpleFakePersister<>());
        for (int i=0; i < 100; i++) {
            cache.putItem(new SimpleCacheItem("name"+i, (int) (Math.random()*200000)));
            String name = "name" + (int) (Math.random() * i);
            SimpleCacheItem cacheItem = cache.getItem(name);
            if (cacheItem != null) {
                System.out.println("Salary for " + name + "=" + cacheItem.getAnnualSalary());
            }
            cache.putItem(new SimpleCacheItem("name"+ (int) (Math.random()*i), (int) (Math.random()*200000)));
            name = "name" + (int) (Math.random() * i);
            cache.removeItem(name);
            System.out.println("Fault rate percent=" + cache.getFaultRatePercent());
        }
        for (int i=0; i < 30; i++) {//tries to remove 30 random items
            String name = "name" + (int) (Math.random()*i);
            cache.removeItem(name);
        }
        cache.resetFaultRateStats();
        cache.modifySize(50);
        for (int i=0; i < 100; i++) {
            cache.putItem(new SimpleCacheItem("name"+i, (int) (Math.random()*200000)));
            String name = "name" + (int) (Math.random()*i);
            SimpleCacheItem cacheItem = cache.getItem(name);
            if (cacheItem != null) {
                System.out.println("Salary for " + name + "=" + cacheItem.getAnnualSalary());
            }
            cache.putItem(new SimpleCacheItem("name"+ (int) (Math.random()*i), (int) (Math.random()*200000)));
            name = "name" + (int) (Math.random()*i);
            cache.removeItem(name);
            System.out.println("Fault rate percent=" + cache.getFaultRatePercent());
        }
    }
}
