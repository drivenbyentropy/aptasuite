package utilities;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Jan Hoinka
 *
 * Helper functions to serialize and deserialize objects from disk.
 * This class is applicable to all classes implementing the Serializable interface.
 */
public class Serialization {

	/**
	 * Serialize the current instance of the object to disk. This is useful for 
	 * later restoring an existing project from disk. 
	 * @param fileName
	 * @throws IOException
	 */
	public static void serialize(Object obj, String fileName) throws IOException {
		
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(obj);

		fos.close();
	}
	
	/**
	 * Deserialize the a previous instance of the class.
	 * @param fileName the path of the file at which the object is stored
	 * @return and instance of the deserialized Object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object deserialize(String fileName) throws IOException, ClassNotFoundException {

		FileInputStream fis = new FileInputStream(fileName);
		ObjectInputStream ois = new ObjectInputStream(fis);
		Object obj = ois.readObject();
		ois.close();
		
		return obj;
		
	}	
	
}
