package benchmarks;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


import lib.aptamer.datastructures.AptamerPool;
import lib.aptamer.datastructures.Experiment;
import lib.aptamer.datastructures.MapDBAptamerPool;
import orestes.bloomfilter.CountingBloomFilter;
import orestes.bloomfilter.FilterBuilder;
import utilities.Configuration;

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

	public static void testRandomSequences() {

		// TODO Auto-generated method stub
		System.out.println("Hola Aptamero!");

		Path pp = Paths.get("/home/matrix/temp/aptasuite");

		Configuration.setConfiguration("/run/media/matrix/a9278623-5051-410b-88db-ae475a62a6ba/eclipse/aptasuite/src/main/resources/current_configuration.properties");
		
		
		//Experiment e = new Experiment(Configuration.getParameters().getString("Experiment.projectPath"));
		try{
			AptamerPool p = new MapDBAptamerPool(pp);
			
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
				p.registerAptamer(new String(sb));
				// bf.add(new String(sb));
				current++;
			}
	
			System.out.println("Iterating Dataset");
			tStart = System.currentTimeMillis();
			int count = 0;
			for (Entry<byte[], Integer> item : p) {
				item.getValue();
				item.getKey();
				count++;
			}
			System.out.printf("Iterated over %s items out of %s in %s seconds\n", count, p.size(),
					((System.currentTimeMillis() - tStart) / 1000.0));
	
			
			System.out.println("Iterating And Finding Values in Dataset");
			tStart = System.currentTimeMillis();
			count = 0;
			for (Entry<byte[], Integer> item : p) {
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

			FileOutputStream forwards = new FileOutputStream("/run/media/matrix/a9278623-5051-410b-88db-ae475a62a6ba/5rounds_forward.fasta.gz");
			FileOutputStream reverses = new FileOutputStream("/run/media/matrix/a9278623-5051-410b-88db-ae475a62a6ba/5rounds_reverse.fasta.gz");
			
			Writer forward = new OutputStreamWriter(new GZIPOutputStream(forwards), "UTF-8");
			Writer reverse = new OutputStreamWriter(new GZIPOutputStream(reverses), "UTF-8");
			
			String[] barcodes = new String[] {"ATGCGT","GACGAC","GGTACC","TCGTAG","CCATGG"};
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
				sbs.append(barcodes[r.nextInt(4)]);
				sbs.append(primer5);
				sbs.append(sb);
				sbs.append(primer3);
				
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
				reverse.write(rr_error.toString() + "\n");
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
			p = new MapDBAptamerPool(pp);
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
		        
		        p.registerAptamer(line);
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

	public static void main(String[] args) {
		
		//generatePairedEndDataset(); 
		testRandomSequences();
		
//		CountingBloomFilter<String> cbf = new FilterBuilder(1000, 0.01).buildCountingBloomFilter();
//		
//		cbf.add("one");
//		cbf.add("one");
//		cbf.add("one");
//		cbf.add("one");
//		cbf.add("one");
//		
//		cbf.add("two");
//		cbf.add("two");
//		cbf.add("two");
//		
//		cbf.add("three");
//		
//		System.out.printf("one %s   two %s   tree %s", cbf.getEstimatedCount("one"),cbf.getEstimatedCount("two"),cbf.getEstimatedCount("three"));
//		

	}

}
