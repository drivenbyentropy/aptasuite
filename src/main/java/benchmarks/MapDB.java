package benchmarks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import lib.aptamer.pool.PoolMapDB;

public class MapDB {

	
	public static String compressString(String str){
		if (str == null || str.length() == 0) {
		    return str;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(str.length());
		GZIPOutputStream gzip;
		try {
			gzip = new GZIPOutputStream(out);
			gzip.write(str.getBytes());
			gzip.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		return out.toString(); // I would return compressedBytes instead String
		}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hola Aptamero!");
		
		Path pp = Paths.get("/home/matrix/temp/aptasuite");
		
		try {
			PoolMapDB p = new PoolMapDB(pp);
			
			char[] alphabet = {'A','C','G','T'};
			char[] sb = {'A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'};
	    	Random r = new Random();
	    	int current = p.size();
	    	int total = 10000000;
	    	long tStart = System.currentTimeMillis();
	    	long tStartTotal = System.currentTimeMillis();
	    	
	    	while (current < total)
	    	{
	    		if (current % 100000 == 0)
	    		{
	    			 System.out.printf("%s / %s \t insert:%s \t total:%s \n", current, total, ((System.currentTimeMillis()-tStart) / 1000.0), ((System.currentTimeMillis()-tStartTotal) / 1000.0));
	    			 tStart = System.currentTimeMillis();
	    		}
	    		for (int i = 0; i<100; i++)
	    		{
	    			sb[i] = alphabet[r.nextInt(4)];
	    		}
	    		p.registerAptamer(new String(sb));
	    		current++;
	    	}
			
	    	p.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		String tempfile = "/home/matrix/temp/mapdbtest.db";
////		String tempfile = "mapdbtest.db";
//		File f = null;
//		
//		 try{
//	         // create new file
//	         f = new File(tempfile);
//	         
//	         // tries to delete a non-existing file
//	         f.delete();
//	         
//	      }catch(Exception e){
//	         // if any error occurs
//	         e.printStackTrace();
//	      }
//		
//		DB db = DBMaker
//			    .fileDB(tempfile)
//			    .fileMmapEnableIfSupported() // Only enable mmap on supported platforms
//			    .concurrencyScale(8) // Number of threads?
//			    .executorEnable()
//			    .make();
//
//		HTreeMap<byte[], Integer> dbmap = db.hashMap("map")
//				.keySerializer(Serializer.BYTE_ARRAY)
//				.valueSerializer(Serializer.INTEGER)
//		        .create();
//
//		char[] alphabet = {'A','C','G','T'};
//		char[] sb = {'A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'};
//    	Random r = new Random();
//    	int current = 0;
//    	int total = 10000000;
//    	long tStart = System.currentTimeMillis();
//    	long tStartTotal = System.currentTimeMillis();
//    	while (current < total)
//    	{
//    		if (current % 100000 == 0)
//    		{
//    			 System.out.printf("%s / %s \t insert:%s \t total:%s \t dbsize: %s mb\n", current, total, ((System.currentTimeMillis()-tStart) / 1000.0), ((System.currentTimeMillis()-tStartTotal) / 1000.0), f.length()/1024/1024);
//    			 tStart = System.currentTimeMillis();
//    		}
//    		for (int i = 0; i<100; i++)
//    		{
//    			sb[i] = alphabet[r.nextInt(4)];
//    		}
//    		dbmap.put(new String(sb).getBytes(), current);
////    		map.put(current, current);
//    		current++;
//    	}
//    	
//    	System.out.printf("Inserted a total of %s items in %s seconds\n", total, ((System.currentTimeMillis()-tStartTotal) / 1000.0));
//    	
//    	db.close();
    	
    	int mb = 1024*1024;
		//Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();
		System.out.println("##### Heap utilization statistics [MB] #####");
		//Print used memory
		System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);
		//Print free memory
		System.out.println("Free Memory:" + runtime.freeMemory() / mb);
		//Print total available memory
		System.out.println("Total Memory:" + runtime.totalMemory() / mb);
		//Print Maximum available memory
		System.out.println("Max Memory:" + runtime.maxMemory() / mb);  	
    
	}

}
