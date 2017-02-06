package lib.parser.aptasim;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HMMData {

	private Character key = null;
	private Double    value = null;
	private Map<Character, HMMData> next = null; 
	
	
	public HMMData(Character c, Double d)
	{
		this.key = c;
		this.value = d;
	}
	
	public void set(String s, Double d)
	{
		//case root
		if (key == null)
		{
			//check if next exists
			if (next == null)
			{
				next = new HashMap<Character, HMMData>();
			}
			if (!next.containsKey(s.charAt(0)))
			{
				next.put(s.charAt(0), new HMMData(s.charAt(0), 0.0));
			}
			next.get(s.charAt(0)).set(s, d);
		}
		
		else if (s.length() == 1)
		{
			set(s.charAt(0), d);
		}
		
		else
		{
			//make sure the next char in s exists as key in the map
			if (next == null)
			{
				next = new HashMap<Character, HMMData>();
			}
			if (!next.containsKey(s.charAt(1)))
			{
				next.put(s.charAt(1), new HMMData(s.charAt(1), 0.0));
			}
			next.get(s.charAt(1)).set(s.substring(1), d);
		}
	}
	
	private void set(Character c, Double d)
	{
		this.key = c;
		this.value = d;
	}
	
	public HMMData get(Character c)
	{
		return next.get(c);
	}

	public HMMData get(String s)
	{
		if (s.length() == 1)
		{
			return get(s.charAt(0));
		}
		return get(s.charAt(0)).get(s.substring(1));
	}
	
	public Boolean containsKey(Character c)
	{
		return (next != null && next.containsKey(c));
	}
	
	public Boolean containsKey(String s)
	{
		if (next == null)
		{
			return false;
		}
		if (s.length() == 1)
		{
			return containsKey(s.charAt(0));
		}
		
		return containsKey(s.charAt(0)) && get(s.charAt(0)).containsKey(s.substring(1));
	}
	
	public Character getKey()
	{
		return this.key;
	}
	
	public Double getValue()
	{
		return this.value;
	}
	
	public Double getTotalCounts()
	{
		Double counts = 0.0;
		for ( Entry<Character, HMMData> entry : next.entrySet())
		{
			counts += entry.getValue().getValue();
		}
		
		return counts;
	}
	
	public Set<Entry<Character, HMMData>> getEntrySet()
	{
		return (next!=null) ? next.entrySet() : null;
	}
}
