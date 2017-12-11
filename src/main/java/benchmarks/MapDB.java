package benchmarks;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.mapdb.Atomic;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;

import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.MapDBAptamerPool;
import lib.structure.rnafold.MFEData;
import lib.structure.rnafold.RNAFoldAPI;
import utilities.Configuration;
import utilities.Index;

public class MapDB {

	public static void generateDataset() {

		char[] alphabet = { 'A', 'C', 'G', 'T' };
		char[] sb = { 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
				'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
				'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
				'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
				'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A' };
		Random r = new Random();
		int current = 0;
		int total = 100000000;

		while (current < total) {
			for (int i = 0; i < 100; i++) {
				sb[i] = alphabet[r.nextInt(4)];
			}

			for (int x = 0; x < r.nextInt(150); x++) {
				System.out.println(new String(sb));
			}
			current++;
		}
	}
	
	public static void testBTreeMap() {

		// TODO Auto-generated method stub
		System.out.println("Hola Aptamero!");

		Path pp = Paths.get("/home/matrix/temp/aptasuite");

		try{

			DB db = DBMaker
				    .fileDB(Paths.get("/home/hoinkaj/aptamers/data_pan/tmp/aptasuite/btreemap.mapdb").toFile())
				    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
				    .concurrencyScale(8) // TODO: Number of threads make this a parameter?
				    .executorEnable()
				    .make();

			BTreeMap<Integer, byte[]> dbmap = db.treeMap("map")
					.valuesOutsideNodesEnable()
					.keySerializer(Serializer.INTEGER)
					.valueSerializer(new SerializerCompressionWrapper(Serializer.BYTE_ARRAY))
			        .create();
			
			char[] alphabet = { 'A', 'C', 'G', 'T' };
			char[] sb = { 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A' };
			Random r = new Random();
			int current = 0;
			int total = 500000000;
			long tStart = System.currentTimeMillis();
			long tStartTotal = System.currentTimeMillis();
	
			while (current < total) {
				if (current % 100000 == 0) {
					System.out.printf("%s / %s \t insert:%s \t total:%s \n", current, total,
							((System.currentTimeMillis() - tStart) / 1000.0),
							((System.currentTimeMillis() - tStartTotal) / 1000.0));
					tStart = System.currentTimeMillis();
				}
				for (int i = 0; i < 100; i++) {
					sb[i] = alphabet[r.nextInt(4)];
				}
				dbmap.put(current, sb.toString().getBytes());
				// bf.add(new String(sb));
				current++;
			}
	
			System.out.println("Iterating Dataset");
			tStart = System.currentTimeMillis();
			int count = 0;
			for (Entry<Integer, byte[]> item : dbmap.getEntries()) {
				item.getValue();
				item.getKey();
				count++;
			}
			System.out.printf("Iterated over %s items in %s seconds\n", count,
					((System.currentTimeMillis() - tStart) / 1000.0));
	
			System.out.println("Iterating Dataset");
			tStart = System.currentTimeMillis();
			ArrayList<Integer> perm = new ArrayList<Integer>();
			for (int x =0; x<count; x++){ perm.add(x); }
			Collections.shuffle(perm);
			count = 0;
			for (Integer x : perm) {
				byte[] item = dbmap.get(x);
				count++;
			}
			System.out.printf("Iterated over %s items in %s seconds\n", count,
					((System.currentTimeMillis() - tStart) / 1000.0));
			
			
			int mb = 1024 * 1024;
			// Getting the runtime reference from system
			Runtime runtime = Runtime.getRuntime();
			System.out.println("##### Heap utilization statistics [MB] #####");
			// Print used memory
			System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);
			// Print free memory
			System.out.println("Free Memory:" + runtime.freeMemory() / mb);
			// Print total available memory
			System.out.println("Total Memory:" + runtime.totalMemory() / mb);
			// Print Maximum available memory
			System.out.println("Max Memory:" + runtime.maxMemory() / mb);
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}	

	public static void testRandomSequences() {

		// TODO Auto-generated method stub
		System.out.println("Hola Aptamero!");

		Path pp = Paths.get("/home/matrix/temp/aptasuite");

		Configuration.setConfiguration("/run/media/matrix/a9278623-5051-410b-88db-ae475a62a6ba/eclipse/aptasuite/src/main/resources/current_configuration.properties");
		
		
		//Experiment e = new Experiment(Configuration.getParameters().getString("Experiment.projectPath"));
		try{
			AptamerPool p = new MapDBAptamerPool(pp, true);
			
			char[] alphabet = { 'A', 'C', 'G', 'T' };
			char[] sb = { 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A' };
			Random r = new Random();
			int current = p.size();
			int total = 75000000;
			long tStart = System.currentTimeMillis();
			long tStartTotal = System.currentTimeMillis();
	
			while (current < total) {
				if (current % 100000 == 0) {
					System.out.printf("%s / %s \t insert:%s \t total:%s \n", current, total,
							((System.currentTimeMillis() - tStart) / 1000.0),
							((System.currentTimeMillis() - tStartTotal) / 1000.0));
					tStart = System.currentTimeMillis();
				}
				for (int i = 0; i < 100; i++) {
					sb[i] = alphabet[r.nextInt(4)];
				}
				p.registerAptamer(new String(sb), 0 ,0);
				// bf.add(new String(sb));
				current++;
			}
	
			System.out.println("Iterating Dataset");
			tStart = System.currentTimeMillis();
			int count = 0;
			for ( Entry<byte[], Integer> item : p.iterator() ) {
				item.getValue();
				item.getKey();
				count++;
			}
			System.out.printf("Iterated over %s items out of %s in %s seconds\n", count, p.size(),
					((System.currentTimeMillis() - tStart) / 1000.0));
	
			
			System.out.println("Iterating And Finding Values in Dataset");
			tStart = System.currentTimeMillis();
			count = 0;
			for (Entry<byte[], Integer> item : p.iterator()) {
				p.getIdentifier(item.getKey());
				count++;
				if (count % 100000 == 0){
					System.out.print(count + "   " + p.getIdentifier(item.getKey()) + "\r");
				}
			}
			System.out.printf("\nIterated and found %s items out of %s in %s seconds", count, p.size(),
					((System.currentTimeMillis() - tStart) / 1000.0));
	
			
			
			//p.clear();
			p.close();
	
			int mb = 1024 * 1024;
			// Getting the runtime reference from system
			Runtime runtime = Runtime.getRuntime();
			System.out.println("##### Heap utilization statistics [MB] #####");
			// Print used memory
			System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);
			// Print free memory
			System.out.println("Free Memory:" + runtime.freeMemory() / mb);
			// Print total available memory
			System.out.println("Total Memory:" + runtime.totalMemory() / mb);
			// Print Maximum available memory
			System.out.println("Max Memory:" + runtime.maxMemory() / mb);
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	public static void generatePairedEndDataset() {

		// TODO Auto-generated method stub
		System.out.println("Hola Aptamero!");

		
		
		try {

			FileOutputStream forwards = new FileOutputStream("/run/media/matrix/a9278623-5051-410b-88db-ae475a62a6ba/5rounds_flankingbarcodes_forward.fasta.gz");
			FileOutputStream reverses = new FileOutputStream("/run/media/matrix/a9278623-5051-410b-88db-ae475a62a6ba/5rounds_flankingbarcodes_reverse.fasta.gz");
			
			Writer forward = new OutputStreamWriter(new GZIPOutputStream(forwards), "UTF-8");
			Writer reverse = new OutputStreamWriter(new GZIPOutputStream(reverses), "UTF-8");
			
			String[] barcodes5 = new String[] {"ATGCGT","GACGAC","GGTACC","TCGTAG","CCATGG"};
			String[] barcodes3 = new String[] {"TAGCCA","ATCGAT","AATCAA","ATCGTA","GGTTAA"};
			String primer5 = "AGTGATGCTAGCTAGCTTGGATCGACTG";
			String primer3 = "TTAGCATCGGGATCTATACGGATCGGTAGCCGT";
			
			char[] alphabet = { 'A', 'C', 'G', 'T' };
			char[] sb = { 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A'};
			char[] qualities = {'\'','!','"','#','$','%','&','(',')','*','+',',','-','.','/','0','1','2','3','4','5','6','7','8','9',':',';','<','=','>','?','@','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','[','\\',']','^','_','`','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','{','|','}','~'};
			Random r = new Random();
			int current = 0;
			int total = 25000000;
			double error_rate = 0.01;

			while (current < total) {
				if (current % 100000 == 0){
					System.out.println(current);
				}
				
				//create randomized region
				for (int i = 0; i < sb.length; i++) {
					sb[i] = alphabet[r.nextInt(4)];
				}
				
				//asseble sequence
				StringBuilder sbs = new StringBuilder();
				int bc_index = r.nextInt(5);
				
				//random letter before
				int total_random_letters = 10;
				int num_letters_befor = r.nextInt(4);
				for (int x=0; x<num_letters_befor; x++){
					sbs.append(alphabet[r.nextInt(4)]);
				}
				
				sbs.append(barcodes5[bc_index]);
				sbs.append(primer5);
				sbs.append(sb);
				sbs.append(primer3);
				sbs.append(barcodes3[bc_index]);
				
				//remaining random letters after
				for (int x=0; x<total_random_letters-num_letters_befor; x++){
					sbs.append(alphabet[r.nextInt(4)]);
				}
				
				current++;
				
				//split into forward and reverse a 75nts each
				String fr = sbs.toString().substring(0,75);
				String rrt = sbs.toString().substring(sbs.toString().length()-75);
				StringBuilder rr = new StringBuilder();
				
				//create reverse read
				for (int x=0; x<rrt.length(); x++){
					if (rrt.charAt(x) == 'A'){
						rr.append("T");
					}
					else if (rrt.charAt(x) == 'C'){
						rr.append("G");
					}
					else if (rrt.charAt(x) == 'G'){
						rr.append("C");
					}
					else if (rrt.charAt(x) == 'T'){
						rr.append("A");
					}
				}
				
				// add errors accodring to error rate
				StringBuilder fr_error = new StringBuilder();
				StringBuilder rr_error = new StringBuilder();
				
				for (int x=0; x<fr.length(); x++){
					if (r.nextDouble() <= error_rate){
						fr_error.append(alphabet[r.nextInt(4)]);
					}
					else {
						fr_error.append(fr.charAt(x));
					}
					
					if (r.nextDouble() <= error_rate){
						rr_error.append(alphabet[r.nextInt(4)]);
					}
					else {
						rr_error.append(rr.toString().charAt(x));
					}
				}
				
				//create quality scores
				StringBuilder qs = new StringBuilder();
				for (int x=0; x<sbs.toString().length(); x++){
					qs.append(qualities[r.nextInt(qualities.length)]);
				}
				
				String qsf = qs.toString().substring(0,75);
				String qsr = new StringBuilder( qs.toString().substring(sbs.toString().length()-75) ).reverse().toString();
				
				//write to files
				forward.write("@SEQ_" + current + "\n");
				forward.write(fr_error.toString() + "\n");
				forward.write("+" + "\n");
				forward.write(qsf.toString() + "\n");
				
				reverse.write("@SEQ_" + current + "\n");
				reverse.write(rr_error.reverse().toString() + "\n");
				reverse.write("+" + "\n");
				reverse.write(qsr.toString() + "\n");
			}

			forward.close();
			reverse.close();
			forwards.close();
			reverses.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}	
	
	public static void testWithGeneratedDataset(){
		
		System.out.println("Hola Aptamero!");
		Path pp = Paths.get("/home/matrix/temp/aptasuite");
		MapDBAptamerPool p = null;
		try {
			p = new MapDBAptamerPool(pp, true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try(
				GZIPInputStream gzip = new GZIPInputStream(new FileInputStream("/run/media/matrix/a9278623-5051-410b-88db-ae475a62a6ba/100_mio_sequences_with_counts.txt.gz"));
				BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
			) {
		    for(String line; (line = br.readLine()) != null; ) {
		        line = line.trim();
		        
		        p.registerAptamer(line, 0, 1);
		    }
		    // line is not visible here.
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	static int iteratedDataSize = 0;
	static int dataSize = 10;
	static int currentIteratorPosition;
	static ArrayList<Integer> data = new ArrayList<Integer>();
	static {
		for(int x=0; x<10;data.add(++x));
	}
	static Iterator<Integer> dataIterator;
	static {
		dataIterator = data.iterator();
	}
	static int stepSize;
	static {
		BigInteger bi = BigInteger.valueOf(2*dataSize);
		stepSize = bi.nextProbablePrime().intValue();
		
		// Set the initial position of the iterator
		currentIteratorPosition = stepSize % dataSize;
		for(int x=0; x<currentIteratorPosition; ++x, dataIterator.next() );
	}
	
	public static Integer getNextItem() {
		
		Integer item = null;
		
		// Do we have more items to return?
		if (iteratedDataSize != dataSize) {
			
			// Get the item to return
			item = dataIterator.next();
			
			// Compute next position
			int new_iterator_position = (currentIteratorPosition + stepSize) % dataSize;
			
//			System.out.println(String.format("%s %s", currentIteratorPosition , new_iterator_position  ));
			
			if( currentIteratorPosition < new_iterator_position ) { // Do we have to circle around?
				
				for (int x=currentIteratorPosition; x<new_iterator_position-1; ++x, dataIterator.next() );
								
			} else {
				
//				System.out.println("else");
				
				// Reset iterator
				dataIterator = data.iterator();
				for (int x=0; x<new_iterator_position; ++x, dataIterator.next() );
				
			}
			
			// Update parameters
			iteratedDataSize++;
			currentIteratorPosition = new_iterator_position;
		}
		
		
		return item;
		
	}
	
	
	public static void main(String[] args) {

//		Test t = new MyStest();
//		
//		for (int x : t.bla) {
//			System.out.println(x);
//		}
//	
//		t = new MyTestTwo();
//		for (int x : t.bla) {
//			System.out.println(x);
//		}
	
		
	}

}
