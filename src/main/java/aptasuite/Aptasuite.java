package aptasuite;

/**
 * @author Jan Hoinka
 * The main class of the aptasuite implementation. This class controls the 
 * program flow for the command line interface as well as the graphical
 * user interface.
 */
public class Aptasuite {

	public static void main(String[] args) {

		// case command line interface
		//TODO: change this later so we can do java -jar --parse --cluster --motif --config path/to/file
		if (args.length == 1){
			
			CLI cli = new CLI(args[0]);
		
		}
		else{
			// TODO implement the gui handling
		}
	}

}
