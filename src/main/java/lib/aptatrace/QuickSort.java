package lib.aptatrace;

import java.util.ArrayList;

public class QuickSort
{
  private ArrayList<Object[]> elements; //first element String Aptamer, second element int Count
  private int index;

  public void sort(ArrayList<Object[]> values) 
  {
    // check for empty or null array
    if (values ==null || values.size()==0)
    {
      return;
    }
    this.elements = values;
    index = values.size();
    quicksort(0, index - 1);
  }

  private void quicksort(int low, int high) 
  {
    int i = low, j = high;
    // Get the pivot element from the middle of the list
    int pivot = (int) elements.get(low + (high-low)/2)[1];

    // Divide into two lists
    while (i <= j) 
    {
      // If the current value from the left list is smaller then the pivot
      // element then get the next element from the left list
      while ((int)elements.get(i)[1] < pivot) 
      {
        i++;
      }
      // If the current value from the right list is larger then the pivot
      // element then get the next element from the right list
      while ((int)elements.get(j)[1] > pivot) 
      {
        j--;
      }

      // If we have found a values in the left list which is larger then
      // the pivot element and if we have found a value in the right list
      // which is smaller then the pivot element then we exchange the
      // values.
      // As we are done we can increase i and j
      if (i <= j) 
      {
        exchange(i, j);
        i++;
        j--;
      }
    }
    
    // Recursion
    if (low < j)
      quicksort(low, j);
    if (i < high)
      quicksort(i, high);
  }

  private void exchange(int i, int j) 
  {
	  Object[] temp = elements.get(i);
	  elements.set(i, elements.get(j));
	  elements.set(j, temp);
  }
} 
