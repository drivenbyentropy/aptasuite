package lib.aptanet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.IntStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang.ArrayUtils;
import org.datavec.api.io.filters.BalancedPathFilter;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.api.util.ndarray.RecordConverter;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.Writable;
import org.datavec.image.loader.BaseImageLoader;
import org.datavec.image.loader.ImageLoader;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.util.InputSplit;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import lib.aptamer.datastructures.Experiment;
import lib.aptanet.input.InputEncoding;
import lib.aptanet.input.SequenceBPPMRecordReader;
import utilities.AptaLogger;
import utilities.Configuration;

public class PlayGround {

	public PlayGround(CommandLine line) {
		
//		BaseImageLoader imageLoader = new NativeImageLoader(10, 10, 3, null); //height, width, channels, imageTransform
//		
//		File image = Paths.get("/home/hoinkaj/aptamers/data_pan/tmp/aptaai/10by10.png").toFile();
//		
//		try {
//			
//			INDArray row = imageLoader.asMatrix(image);
//			
//			List<Writable> ret;
//			
//			ret = RecordConverter.toRecord(row);
//            ret.add(new IntWritable(42));
//			
//			System.out.println(ArrayUtils.toString(Nd4j.shape(row))); 
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		
		
		
//		Random rng = new Random(42);
//	    int numExamples = 6;
//	    int numLabels = 3;
//		
//		
//		ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
//		
//        File mainPath = new File("/home/hoinkaj/aptamers/data_pan/tmp/aptaai/test/");
//        FileSplit fileSplit = new FileSplit(mainPath, NativeImageLoader.ALLOWED_FORMATS, rng);
//        BalancedPathFilter pathFilter = new BalancedPathFilter(rng, labelMaker, numExamples, numLabels, 2);
//
//        /**
//         * Data Setup -> train test split
//         *  - inputSplit = define train and test split
//         **/
//        
//        org.datavec.api.split.InputSplit[] inputSplit = fileSplit.sample(pathFilter, 1.0, 0.0);
//        //InputSplit[] inputSplit = fileSplit.sample(pathFilter, 1.0, 0.0);
//        org.datavec.api.split.InputSplit trainData = inputSplit[0];
//        //org.datavec.api.split.InputSplit testData = inputSplit[1];
//		
//	    ImageRecordReader recordReader = new ImageRecordReader(10, 10, 3, labelMaker);
//	    try {
//			recordReader.initialize(trainData);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		List<Writable> test = recordReader.next(4);
//
//		System.out.println(test);
		
	
		
//		// Create a single array
//		INDArray input1 = Nd4j.rand(new int[]{1, 3, 4, 4}, 'f');
//		INDArray input2 = Nd4j.rand(new int[]{1, 3, 4, 4}, 'f');
//		INDArray input3 = Nd4j.rand(new int[]{1, 3, 4, 4}, 'f');
//		
//		// Create tensor with 3 items
////		INDArray multi = Nd4j.rand(new int[]{3, 3, 4, 4}, 'c'); 
//		INDArray multi = Nd4j.createUninitialized(new int[]{3, 3, 4, 4}, 'c'); 
//		
//		// now put them together
//		INDArray bla = multi.tensorAlongDimension(1, 1,2,3);
//		
//		System.out.println("Multi");
//		System.out.println(multi);
//		
//		System.out.println("BLA");
//		System.out.println(bla);
//		
//		System.out.println("input1");
//		System.out.println(input1);
//		
//		multi.putRow(1, input1);
//		
//		System.out.println("Multi NEW");
//		System.out.println(multi);
//		
//		
//		for (int i : input2.getRow(0).shape()) {
//			
//			System.out.print(i + " ");
//			
//		}
//		System.out.println();
		
		
		
		
		
		
		
		
		
		
//		for (int i = 0; i < cnt; i++) {
//            try {
//                ((NativeImageLoader) imageLoader).asMatrixView(currBatch.get(i),
//                                features.tensorAlongDimension(i, 1, 2, 3));
//            } catch (Exception e) {
//                System.out.println("Image file failed during load: " + currBatch.get(i).getAbsolutePath());
//                throw new RuntimeException(e);
//            }
//        }
		
		
//		Experiment experiment = Configuration.getExperiment();
//		
//		// Make sure we have data prior or load it from disk
//		if (experiment == null) {
//			AptaLogger.log(Level.INFO, this.getClass(), "Loading data from disk");
//			experiment = new Experiment(line.getOptionValue("config"), false);
//		}
//		else{
//			AptaLogger.log(Level.INFO, this.getClass(), "Using existing data");
//		}
//		
//		// Get the instance of the BPPM StructurePool
//		if (experiment.getBppmPool() == null)
//		{
//			experiment.instantiateBppmPool(false);
//		}	
//		
//		// Temporary bitset all 1
//		BitSet bs = new BitSet(experiment.getAptamerPool().size());
//		bs.flip(0, experiment.getAptamerPool().size());
//		
//		SequenceBPPMRecordReader rr = new SequenceBPPMRecordReader(false, null);
//		rr.initialize(bs, InputEncoding.BPPM_PLUS_ONE_CHANNEL);
//		
//		rr.next();
		
		
	}
	
}
