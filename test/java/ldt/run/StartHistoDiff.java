package ldt.run;

import ldt.histo.HistoDiffReader;

public class StartHistoDiff{
	// To run a diff specify the path to the new histogram and the old histogram and run this class.

	public static void main(String a[]) throws Exception{
		String newP = "/Users/another/Documents/Projects/XXX/CRASH/2013-02-05/histo3.txt";
		String oldP = "/Users/another/Documents/Projects/XXX/CRASH/2013-02-05/histo2.txt";
		HistoDiffReader.compare(oldP, newP);
	}
}

