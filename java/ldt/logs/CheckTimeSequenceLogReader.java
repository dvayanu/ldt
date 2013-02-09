package ldt.logs;

import net.anotheria.util.StringUtils;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TODO comment this class
 *
 * @author lrosenberg
 * @since 09.02.13 11:50
 */
public class CheckTimeSequenceLogReader {
	public static void main(String a[]) throws Exception{
		String target = null;
		if (a.length==0)
			target = "access_log.2013-02-09";
		else
			target = a[0];

		FileReader fileReader = new FileReader(target);
		LineNumberReader lineReader = new LineNumberReader(fileReader);
		String line = null;
		int count = 0;
		SimpleDateFormat format= new SimpleDateFormat("hh:MM:ss");
		long previous = Long.MIN_VALUE;
		long current;
		String previousLine = null;
		while( (line=lineReader.readLine())!=null){
			//if (count++/100*100)
			//	break;
			String []t = StringUtils.tokenize(line, ' ');
			String time = t[1];
			time = time.substring(time.indexOf(':')+1);
			Date dt = format.parse(time);
			current = dt.getTime();
			if (current<previous){
				System.out.println("ERROR wrong order \n  "+line+"\n  "+previousLine);
			}
			previous = current;
			previousLine = line;
		}
	}
}
