/**
 * 
 */
package utilities;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Jan Hoinka
 * A series of helper functions to provide uniform behavior
 * when dealing with files regardless of the OS or filesystem.
 */
public class FileUtilities {
	
	
	/**
	 * Returns a List of paths sorted in ascending order based on the file ID
	 * for all elements in the provided stream
	 * 
	 * E.g. data0042.mapdb will be sorted based on integer 42
	 * @param directoryStream
	 * @return
	 */
	public static List<Path> getSortedPaths( DirectoryStream<Path> directoryStream ){
		
		// Temp class to sort these elements together
		class TempContainer {
			
			public Path poolDataPath;
			
			// extracted from the path 
			private Integer fileID;
			
			public TempContainer(Path poolDataPath) {
				
				this.poolDataPath = poolDataPath;
				this.fileID = extractFileID(poolDataPath);
				
			}
			
			// Extracts the integer reprentation of the data filennames
			// E.g. data0042.mapdb returns 42 
			private Integer extractFileID(Path path) {
				
				return Integer.parseInt(path.getFileName().toString().replaceAll("[^0-9]", ""));
				
			}
			
		}
		
		// Store the paths 
		List<TempContainer> sorted_paths = new ArrayList<TempContainer>();

			for (Path file : directoryStream) {
				
				sorted_paths.add( new TempContainer(file) );
				
			}
			
		
		// Sort them
		sorted_paths.sort( (o1,o2) -> o1.fileID.compareTo(o2.fileID) );
		
		// Return only the path		
		return sorted_paths.stream().map( tc -> tc.poolDataPath ).collect(Collectors.toList());
		
	}

}
