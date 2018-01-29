/**
 * 
 */
package utilities;

import java.util.BitSet;

/**
 * @author Jan Hoinka
 * Customized version of Quicksort with the option of using a 
 * comparator for primitive int arrays
 */
public class Quicksort {

		
	/**
	 * Sorts a list of primitive ints in ascending order.
	 * Sorting will be in-place using quicksort
	 * @param values the list to be sorted
	 */
	public static void sort(int[] values) {
		
		class StandardQSComparator implements QSComparator{

			@Override
			public int compare(int a, int b) {
				return Integer.compare(a, b);
			}
			
						
		}
		
		quicksort(values, 0, values.length - 1, new StandardQSComparator());
    }
	
	/**
	 * Sorts a list of primitive ints in ascending order.
	 * Sorting will be in-place using quicksort
	 * @param values the list to be sorted
	 * @param c a custom comparator
	 */
	public static void sort(int[] values, QSComparator c) {
		
		quicksort(values, 0, values.length - 1, c);
    }
	
	
	/**
	 * Sorts a list of primitive ints in ascending order based on the data of
	 * another list. In other words, this function performs the same swapping 
	 * operation on <code>values</code> while sorting <code>reference</code>.
	 * Sorting will be in-place using quicksort
	 * @param values the list to be sorted
	 */
	public static void sort(int[] values, int reference[]) {
		
		class StandardQSComparator implements QSComparator{

			@Override
			public int compare(int a, int b) {
				return Integer.compare(a, b);
			}
			
						
		}
		
		quicksort(values, reference, 0, values.length - 1, new StandardQSComparator());
    }
	
	/**
	 * Sorts a list of primitive ints in ascending order base on the data of
	 * another list. In other words, this function performs the same swapping 
	 * operation on <code>values</code> while sorting <code>reference</code>.
	 * Sorting will be in-place using quicksort
	 * @param values the list to be sorted
	 * @param c a custom comparator
	 */
	public static void sort(int[] values, int[] reference, QSComparator c) {
		
		quicksort(values, reference, 0, values.length - 1, c);
    }
	
	    
	/**
	 * Performs quicksort on the specified list
	 * @param numbers the arry to be sorted
	 * @param low lower bound of quicksort
	 * @param high upper bound of quicksort
	 * @param c the comparator to be used
	 */
	public static void quicksort(int[] numbers, int low, int high, QSComparator c) {
		int i = low, j = high;
		// Get the pivot element from the middle of the list
		int pivot = numbers[low + (high-low)/2];

		// Divide into two lists
		while (i <= j) {
			
			// If the current value from the left list is smaller than the pivot
			// element then get the next element from the left list
            //while (numbers[i] < pivot) {
			while (c.compare(numbers[i], pivot) == -1) {
                i++;
            }
            
            // If the current value from the right list is larger than the pivot
            // element then get the next element from the right list
            //while (numbers[j] > pivot) {
			while(c.compare(numbers[j], pivot) == 1){
                j--;
            }

            // If we have found a value in the left list which is larger than
            // the pivot element and if we have found a value in the right list
            // which is smaller than the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchange(numbers, i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort(numbers, low, j, c);
        if (i < high)
            quicksort(numbers, i, high, c);
    }

	
	/**
	 * Performs quicksort by sorting one array based on the content of the other
	 * @param numbers the array to be sorted
	 * @param reference the array which is used to sort the other list
	 * @param low lower bound of quicksort
	 * @param high upper bound of quicksort
	 * @param c the comparator to be used
	 */
	public static void quicksort(int[] numbers, int[] reference, int low, int high, QSComparator c) {
		int i = low, j = high;
		// Get the pivot element from the middle of the list
		int pivot = reference[low + (high-low)/2];

		// Divide into two lists
		while (i <= j) {
			
			// If the current value from the left list is smaller than the pivot
			// element then get the next element from the left list
            //while (numbers[i] < pivot) {
			while (c.compare(reference[i], pivot) == -1) {
                i++;
            }
            
            // If the current value from the right list is larger than the pivot
            // element then get the next element from the right list
            //while (numbers[j] > pivot) {
			while(c.compare(reference[j], pivot) == 1){
                j--;
            }

            // If we have found a value in the left list which is larger than
            // the pivot element and if we have found a value in the right list
            // which is smaller than the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchange(reference, i, j);
                exchange(numbers, i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort(numbers, reference, low, j, c);
        if (i < high)
            quicksort(numbers, reference, i, high, c);
    }
	
	/**
	 * Performs quicksort by sorting one array based on the content of the other
	 * @param numbers the array to be sorted
	 * @param reference the array which is used to sort the other list
	 * @param low lower bound of quicksort
	 * @param high upper bound of quicksort
	 * @param c the comparator to be used
	 */
	public static void quicksort(int[] numbers, int[] reference, BitSet bitset, int low, int high, QSComparator c) {
		int i = low, j = high;
		// Get the pivot element from the middle of the list
		int pivot = reference[low + (high-low)/2];

		// Divide into two lists
		while (i <= j) {
			
			// If the current value from the left list is smaller than the pivot
			// element then get the next element from the left list
            //while (numbers[i] < pivot) {
			while (c.compare(reference[i], pivot) == -1) {
                i++;
            }
            
            // If the current value from the right list is larger than the pivot
            // element then get the next element from the right list
            //while (numbers[j] > pivot) {
			while(c.compare(reference[j], pivot) == 1){
                j--;
            }

            // If we have found a value in the left list which is larger than
            // the pivot element and if we have found a value in the right list
            // which is smaller than the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchange(reference, i, j);
                exchange(numbers, i, j);
                if (bitset.get(i) != bitset.get(j)) {
                	
                	bitset.flip(i);
                	bitset.flip(j);
                	
                }
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort(numbers, reference, bitset, low, j, c);
        if (i < high)
            quicksort(numbers, reference, bitset, i, high, c);
    }

    private static void exchange(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

	/**
	 * Performs quicksort by sorting one array based on the content of the other
	 * @param numbers the array to be sorted
	 * @param reference the array which is used to sort the other list
	 * @param low lower bound of quicksort
	 * @param high upper bound of quicksort
	 * @param c the comparator to be used
	 */
	public static void quicksort(int[] numbers, double[] reference, int low, int high, QSDoubleComparator c) {
		
		int i = low, j = high;
		// Get the pivot element from the middle of the list
		double pivot = reference[low + (high-low)/2];

		// Divide into two lists
		while (i <= j) {
			
			// If the current value from the left list is smaller than the pivot
			// element then get the next element from the left list
            //while (numbers[i] < pivot) {
			while (c.compare(reference[i], pivot) == -1) {
                i++;
            }
            
            // If the current value from the right list is larger than the pivot
            // element then get the next element from the right list
            //while (numbers[j] > pivot) {
			while(c.compare(reference[j], pivot) == 1){
                j--;
            }

            // If we have found a value in the left list which is larger than
            // the pivot element and if we have found a value in the right list
            // which is smaller than the pivot element then we exchange the
            // values.
            // As we are done we can increase i and j
            if (i <= j) {
                exchange(reference, i, j);
                exchange(numbers, i, j);
                i++;
                j--;
            }
        }
        // Recursion
        if (low < j)
            quicksort(numbers, reference, low, j, c);
        if (i < high)
            quicksort(numbers, reference, i, high, c);
		
	}
	
	
    private static void exchange(double[] array, int i, int j) {
        double temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
	
    
    public static QSComparator AscendingQSComparator() {
    	
		class StandardQSComparator implements QSComparator{

			@Override
			public int compare(int a, int b) {
				return Integer.compare(a, b);
			}
						
		}
    	
    	return new StandardQSComparator();
    	
    }
    
    
    public static QSComparator DescendingQSComparator() {
    	
		class StandardQSComparator implements QSComparator{

			@Override
			public int compare(int a, int b) {
				return Integer.compare(b, a);
			}
						
		}
    	
    	return new StandardQSComparator();
    	
    }
    
}
