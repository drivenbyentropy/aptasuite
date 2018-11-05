/**
 * 
 */
package lib.aptamer.datastructures;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.serializer.GroupSerializer;
import org.mapdb.serializer.SerializerCompressionWrapper;

import utilities.AptaLogger;

/**
 * @author Jan Hoinka
 * MapDB implementation of the GenericStorage interface
 */
public class MapDBGenericStorage<T, U> implements GenericStorage<T, U> {

	private BTreeMap<T, U> dbmap = null;
	
	private DB db_structure = null;
	
	private Path db_path = null;
	
	private String db_name = null;
	
	private Path full_qualifier = null;
	
	/**
	 * @param p
	 * @param map_name
	 * @param key_serializer
	 * @param value_serializer
	 * @param new_db
	 * @param exit_on_error
	 */
	public MapDBGenericStorage(Path p, String db_name, GroupSerializer key_serializer, GroupSerializer value_serializer){

		this.db_name = db_name;
		this.db_path = p;
		this.full_qualifier = Paths.get(p.toString(), db_name);

		AptaLogger.log(Level.INFO, this.getClass(), "Creating Database " + full_qualifier.toString());
		
		try {
			db_structure = DBMaker
					.fileDB(full_qualifier.toFile())
					.fileMmapEnableIfSupported() // Only enable mmap on supported platforms
					.fileMmapPreclearDisable() // Make mmap file faster
					.concurrencyScale(8) // TODO: Number of threads make this a parameter?
					.executorEnable()
					.transactionEnable()
					.make();
			
		}
		//If this fails, we need to make sure that all file handles have been closed
		catch(Exception e) {

			if (db_structure != null) {  
				db_structure.close(); 
			}

			AptaLogger.log(Level.WARNING, this.getClass(), "Could not complete DBMaker call. Error:");
			AptaLogger.log(Level.WARNING, this.getClass(), e);
			
			throw(e);
		}

		dbmap = db_structure.treeMap("map")
				.valuesOutsideNodesEnable()
				.keySerializer(key_serializer)
				.valueSerializer(new SerializerCompressionWrapper(value_serializer))
				.createOrOpen();
	}
	
	@Override
	public U get(T key) {

		return dbmap.get(key);
		
	}

	@Override
	public U put(T key, U value) {
		
		U rvalue = dbmap.put(key, value);
		db_structure.commit();
		
		return rvalue;
		
	}

	@Override
	public U remove(T key) {
		
		U rvalue = dbmap.remove(key);
		db_structure.commit();
		
		return rvalue;
		
	}

	@Override
	public Boolean containsKey(T key) {
		
		return dbmap.containsKey(key);
		
	}

	@Override
	public void close() {
		
		// Commit any remaining transactions
		db_structure.commit();

		// now close the remaining handles
		dbmap.close();
		db_structure.close();
		
	}

}
