/**
 * 
 */
package lib.aptanet.input;

import java.util.ArrayList;
import java.util.logging.Level;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import exceptions.InvalidConfigurationException;
import gnu.trove.map.hash.TByteDoubleHashMap;
import gnu.trove.map.hash.TByteIntHashMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import lib.aptamer.datastructures.AptamerBounds;
import lib.aptanet.AptamerLabelPair;
import utilities.AptaLogger;
import utilities.Index;

/**
 * @author Jan Hoinka
 * Static methods to convert aptamer data into
 * tensors which can be fed into neural networks
 */
public class TensorFactory {
	
	/**
	 * Defines the order of the channels in the resulting INDArrays
	 */
	private static TByteObjectHashMap<TByteIntHashMap> basePairsToChannelIndex = new TByteObjectHashMap<TByteIntHashMap>();
	static {
		
		// Set the channel index mappings
		basePairsToChannelIndex.put((byte)'A', new TByteIntHashMap());
		basePairsToChannelIndex.put((byte)'C', new TByteIntHashMap());
		basePairsToChannelIndex.put((byte)'G', new TByteIntHashMap());
		basePairsToChannelIndex.put((byte)'T', new TByteIntHashMap());

		basePairsToChannelIndex.get((byte)'A').put((byte)'A', 0);
		basePairsToChannelIndex.get((byte)'A').put((byte)'C', 1);
		basePairsToChannelIndex.get((byte)'A').put((byte)'G', 2);
		basePairsToChannelIndex.get((byte)'A').put((byte)'T', 3);
		basePairsToChannelIndex.get((byte)'C').put((byte)'A', 4);
		basePairsToChannelIndex.get((byte)'C').put((byte)'C', 5);
		basePairsToChannelIndex.get((byte)'C').put((byte)'G', 6);
		basePairsToChannelIndex.get((byte)'C').put((byte)'T', 7);
		basePairsToChannelIndex.get((byte)'G').put((byte)'A', 8);
		basePairsToChannelIndex.get((byte)'G').put((byte)'C', 9);
		basePairsToChannelIndex.get((byte)'G').put((byte)'G', 10);
		basePairsToChannelIndex.get((byte)'G').put((byte)'T', 11);
		basePairsToChannelIndex.get((byte)'T').put((byte)'A', 12);
		basePairsToChannelIndex.get((byte)'T').put((byte)'C', 13);
		basePairsToChannelIndex.get((byte)'T').put((byte)'G', 14);
		basePairsToChannelIndex.get((byte)'T').put((byte)'T', 15);
		
	}
	
	/**
	 * Defines a value representing the id of of a pair
	 */
	private static TByteObjectHashMap<TByteDoubleHashMap> basePairsToValue = new TByteObjectHashMap<TByteDoubleHashMap>();
	static {
		
		// Set the channel index mappings
		basePairsToValue.put((byte)'A', new TByteDoubleHashMap());
		basePairsToValue.put((byte)'C', new TByteDoubleHashMap());
		basePairsToValue.put((byte)'G', new TByteDoubleHashMap());
		basePairsToValue.put((byte)'T', new TByteDoubleHashMap());

		basePairsToValue.get((byte)'A').put((byte)'A', 0.0/15.0);
		basePairsToValue.get((byte)'A').put((byte)'C', 1.0/15.0);
		basePairsToValue.get((byte)'A').put((byte)'G', 2.0/15.0);
		basePairsToValue.get((byte)'A').put((byte)'T', 3.0/15.0);
		basePairsToValue.get((byte)'C').put((byte)'A', 4.0/15.0);
		basePairsToValue.get((byte)'C').put((byte)'C', 5.0/15.0);
		basePairsToValue.get((byte)'C').put((byte)'G', 6.0/15.0);
		basePairsToValue.get((byte)'C').put((byte)'T', 7.0/15.0);
		basePairsToValue.get((byte)'G').put((byte)'A', 8.0/15.0);
		basePairsToValue.get((byte)'G').put((byte)'C', 9.0/15.0);
		basePairsToValue.get((byte)'G').put((byte)'G', 10.0/15.0);
		basePairsToValue.get((byte)'G').put((byte)'T', 11.0/15.0);
		basePairsToValue.get((byte)'T').put((byte)'A', 12.0/15.0);
		basePairsToValue.get((byte)'T').put((byte)'C', 13.0/15.0);
		basePairsToValue.get((byte)'T').put((byte)'G', 14.0/15.0);
		basePairsToValue.get((byte)'T').put((byte)'T', 15.0/15.0);
		
	}	
	
	

	/**
	 * Returns and INDArray with the representation as stated in <code>encoding</code>
	 * @param sequence
	 * @param structure
	 * @param start (inclusive)
	 * @param end (exclusive)
	 * @param encoding
	 * @return
	 */
	public static INDArray getTensor(byte[] sequence, double[] structure, int start, int end, InputEncoding encoding) {
		
		switch (encoding) {
			
			case BPPM_PLUS_SIXTEEN_CHANNELS: {
				
				// Allocate space:
		        // Note the order here:
		        //  dimension 0 = number of examples in minibatch
				//  dimension 1 = Number of channels (16 in this case) 
		        //  dimension 2 = end-start
		        //  dimension 3 = end-start

		        // Why 'f' order here? See http://deeplearning4j.org/usingrnns.html#data section "Alternative: Implementing a custom DataSetIterator"
				INDArray input = Nd4j.zeros(new int[]{1, 17, end-start, end-start}, 'f');
				
				return getBPPMPlusSixteenChannelsTensor(sequence, structure, start, end, input, encoding);
			}
			
			case BPPM_PLUS_ONE_CHANNEL: {
				
				// Allocate space:
		        // Note the order here:
		        //  dimension 0 = number of examples in minibatch
				//  dimension 1 = Number of channels (2 in this case) 
		        //  dimension 2 = end-start
		        //  dimension 3 = end-start

		        // Why 'f' order here? See http://deeplearning4j.org/usingrnns.html#data section "Alternative: Implementing a custom DataSetIterator"
				INDArray input = Nd4j.zeros(new int[]{1, 2, end-start, end-start}, 'f');
				
				return getBPPMPlusOneChannelTensor(sequence, structure, start, end, input, encoding);
			}
			
			default: {
				
				AptaLogger.log(Level.SEVERE, TensorFactory.class, String.format("The encoding %s is not compatible with the provided data", encoding.name()));
				throw new InvalidConfigurationException(String.format("The encoding %s is not compatible with the provided data", encoding.name()) );
				
			}
		}
	}

	/**
	 * Sets an INDArray with the representation as stated in <code>encoding</code>
	 * @param sequence
	 * @param structure
	 * @param start (inclusive)
	 * @param end (exclusive)
	 * @param INDArray input
	 * @param encoding
	 * @return
	 */
	public static INDArray setTensor(byte[] sequence, double[] structure, int start, int end, INDArray input, InputEncoding encoding) {
		
		switch (encoding) {
			
			case BPPM_PLUS_SIXTEEN_CHANNELS: {
				
				return getBPPMPlusSixteenChannelsTensor(sequence, structure, start, end, input, encoding);
				
			}
			
			case BPPM_PLUS_ONE_CHANNEL: {
				
				return getBPPMPlusOneChannelTensor(sequence, structure, start, end, input, encoding);
				
			}
			
			default: {
				
				AptaLogger.log(Level.SEVERE, TensorFactory.class, String.format("The encoding %s is not compatible with the provided data", encoding.name()));
				throw new InvalidConfigurationException(String.format("The encoding %s is not compatible with the provided data", encoding.name()) );
				
			}
		}
	}

	private static INDArray getBPPMPlusSixteenChannelsTensor(byte[] sequence, double[] bppm, int start, int end, INDArray input, InputEncoding encoding) {
		

		// Fill the structure portion with the base pair probabilities
		for(int x=start; x<end; x++) {
			
			for(int y=x; y<end; y++) {
				
				if (x==y) continue;
				
				// Fill the Base Pair Probability Channel 
				input.putScalar(new int[] {0, 0 , x-start, y-start}, bppm[Index.triu(x, y, sequence.length)]);
				input.putScalar(new int[] {0, 0 , y-start, x-start}, bppm[Index.triu(x, y, sequence.length)]);
			
			}
			
		}
		
		// Fill the sequence portion with a one-hot representation of the aptamer
		for(int x=start; x<end; x++) {
			
			for(int y=start; y<end; y++) {
				
				// Set the right position hot
				input.putScalar(new int[] {0, 1+basePairsToChannelIndex.get(sequence[x]).get(sequence[y]) , x-start, y-start}, 1.0);
				
			}
			
		}
				
		return input;
		
	}
	
	
	private static INDArray getBPPMPlusOneChannelTensor(byte[] sequence, double[] bppm, int start, int end, INDArray input, InputEncoding encoding) {
		
		// Fill the structure portion with the base pair probabilities
		for(int x=start; x<end; x++) {
			
			for(int y=x; y<end; y++) {
				
				if (x==y) continue;
				
				// Fill the Base Pair Probability Channel 
				input.putScalar(new int[] {0, 0 , x-start, y-start}, bppm[Index.triu(x, y, sequence.length)]);
				input.putScalar(new int[] {0, 0 , y-start, x-start}, bppm[Index.triu(x, y, sequence.length)]);
			
			}
			
		}
		
		// Fill the sequence portion with a one-hot representation of the aptamer
		for(int x=start; x<end; x++) {
			
			for(int y=start; y<end; y++) {
				
				// Set the right position hot
				input.putScalar(new int[] {0, 1, x-start, y-start}, basePairsToValue.get(sequence[x]).get(sequence[y]));
				
			}
			
		}
				
		return input;	
		
	}
	
	
	
	
	
	
	
}
