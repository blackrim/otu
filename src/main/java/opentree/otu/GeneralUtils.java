package opentree.otu;

import java.util.*;

/* 
 * Is this used anywhere?
 * 
 * Is so, what are the properties of the list being passed in?
 * 1. Is it guaranteed to start at 1 (or 0)?
 * 2. Is it consecutive from start to length(list)?
 * 
 */

public class GeneralUtils {

    // all common non-alphanumeric chars except "_" and "-", for use when cleaning strings
//    public final static String badchars = "`!@#$%^&*()+=;':\",.<>/\\?|\b\t\n\f\r";

    public static final String offendingChars = "[\\Q\"_~`:;/[]{}|<>,.!@#$%^&*()?+=`\\\\\\E\\s]+";

    public static final char offendingJSONChars = '"';
    
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
    	String cleanString = dirtyString.replace(offendingJSONChars, ' ');
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
