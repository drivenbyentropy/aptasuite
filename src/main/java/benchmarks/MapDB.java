package benchmarks;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lib.aptamer.pool.PoolMapDB;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.FilterBuilder;

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

		try {
			PoolMapDB p = new PoolMapDB(pp);
			// p.setMaxTreeMapCapacity(100);

			char[] alphabet = { 'A', 'C', 'G', 'T' };
			char[] sb = { 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
					'A' };
			Random r = new Random();
			int current = p.size();
			int total = 100000000;
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
			System.out.printf("Iterated over %s items out of %s in %s seconds", count, p.size(),
					((System.currentTimeMillis() - tStart) / 1000.0));

			p.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
	
	public static void testWithGeneratedDataset(){
		
		System.out.println("Hola Aptamero!");
		Path pp = Paths.get("/home/matrix/temp/aptasuite");
		PoolMapDB p = null;
		try {
			p = new PoolMapDB(pp);
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
		
		

	}

}
