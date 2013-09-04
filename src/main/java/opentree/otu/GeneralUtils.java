package opentree.otu;

import java.util.*;

public class GeneralUtils {

    public static final String offendingChars = "[\\Q\"_~`:;/[]{}|<>,.!@#$%^&*()?+=`\\\\\\E\\s]+";
    public static final char offendingJSONChar = '"';
    
    public static int sum_ints(List<Integer> list){
		if(list==null || list.size()<1)
			return 0; // shouldn't this throw a null pointer exception? otherwise how do we differentiate null from a list of zeroes?

		int sum = 0;
		for(Integer i: list)
			sum = sum+i;

		return sum;
	}
    
    public static String[] convertToStringArray(List<String> list) {
    	String[] arr = new String[list.size()];
    	int i = 0;
    	for (String s : list) {
    		arr[i] = s;
    		i++;
    	}
    	return arr;
    }

    public static long[] convertToLongArray(List<Long> list) {
    	long[] arr = new long[list.size()];
    	int i = 0;
    	for (long l : list) {
    		arr[i] = l;
    		i++;
    	}
    	return arr;
    }

    public static String escapeStringForJSON(String dirtyString) {
    	String cleanString = dirtyString.replace(offendingJSONChar, ' ');
	    return cleanString;
    }
    
	/**
	 * Replaces non-alphanumeric characters (excluding "_" and "-") in `dirtyName` with "_" and returns the cleaned name.
	 * Currently slow and crappy, should be updated to use regex and just do a single pass over the string.
	 * 
	 * @param dirtyName
	 * @return cleaned name
	 */
	public static String cleanName(String dirtyName) {
	    String cleanName = dirtyName.replaceAll(offendingChars, "_");	    
	    return cleanName;
	}
}
