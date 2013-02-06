package ldt.threads;

import net.anotheria.util.BasicComparable;
import net.anotheria.util.StringUtils;
import net.anotheria.util.sorter.IComparable;
import net.anotheria.util.sorter.SortType;
import net.anotheria.util.sorter.StaticQuickSorter;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ThreadDumpReader {

	private static int compareCounter = 0;
	private static FileOutputStream file ;


	private static void log(Object o) throws IOException {
		file.write((""+o+"\n").getBytes());
	}

	private static void out(Object o) throws IOException{
		System.out.println(o);
		log(o);
	}

	public static void compare(String newer, String older) throws Exception{
		System.out.println("====================================");
		System.out.println("COMPARING "+newer+" and "+older);
		file = new FileOutputStream("compare_"+(compareCounter++)+".txt");
		compare(readStackTrace(newer), readStackTrace(older));
		file.flush();
		file.close();
	}

	public static void analyze(String dumpName) throws Exception{
		System.out.println("Analyzing "+dumpName);

		String outputFileName = "analyze_";
		if (dumpName.indexOf('/')==-1){
			outputFileName+=dumpName;
		}else{
			outputFileName+=dumpName.substring(dumpName.lastIndexOf('/')+1);
		}

		file = new FileOutputStream(outputFileName);
		ThreadDump dump = readStackTrace(dumpName);
		System.out.println("Dump: "+dump);

		out("===== LINES =====");
		HashMap<String, LineHolder> lines = new HashMap<String, LineHolder>();

		for (DebugThread dt : dump.threads){
			if (dt.calls.size()<2)
				continue;
			LineHolder line = lines.get(dt.calls.get(1));
			if (line==null){
				line = new LineHolder();
				line.line = dt.calls.get(1);
				lines.put(dt.calls.get(1), line);
			}
			line.count++;
		}

		SortType st = new SortType(0, SortType.DESC);
		List<LineHolder> sortedByOccurence = StaticQuickSorter.sort(lines.values(), st);
		out("Most threads are caught in those lines, sorted by most occurence.");
		for (LineHolder lh : sortedByOccurence){
			out(lh);
		}
		out("");

		out("===== IDENTICAL THREAD TRACES =====");
		HashMap<String, ThreadHolder> threads = new HashMap<String, ThreadHolder>();

		for (DebugThread dt : dump.threads){
			if (dt.calls.size()<2)
				continue;
			ThreadHolder th = threads.get(dt.calls.toString());
			if (th==null){
				th = new ThreadHolder();
				th.dt = dt;
				threads.put(dt.calls.toString(), th);
			}
			th.count++;
		}

		List<ThreadHolder> thSortedByOccurence = StaticQuickSorter.sort(threads.values(), st);
		int countOfIdenticalThreads = 0, distinctCountOfIdenticalThreads = 0;
		out("Identical threads:");
		for (ThreadHolder th : thSortedByOccurence){
			if (th.count>1){
				out(th.dt.getName()+" --- "+th.count);
				countOfIdenticalThreads += th.count;
				distinctCountOfIdenticalThreads++;
				th.dt.dumpOut();
				out("");
			}
		}
		out("found "+distinctCountOfIdenticalThreads+" identical thread type with total of "+countOfIdenticalThreads+" repetitions");
		out("");

	}

	static class ThreadHolder implements IComparable{
		int count;
		DebugThread dt;

		@Override
		public int compareTo(IComparable anotherObject, int method) {
			return BasicComparable.compareInt(count, ((ThreadHolder)anotherObject).count);
		}
	}

	static class LineHolder implements IComparable{
		String line;
		int count;

		@Override
		public int compareTo(IComparable anotherObject, int method) {
			return BasicComparable.compareInt(count, ((LineHolder)anotherObject).count);
		}

		@Override public String toString(){
			return line.trim()+" "+count;
		}
	}

	private static void compare(ThreadDump newer, ThreadDump older) throws IOException{
		out("TC:\t"+newer.getTotalThreadCount()+"\t"+older.getTotalThreadCount()+" Dif: "+(newer.getTotalThreadCount()-older.getTotalThreadCount()));
		out("DC:\t"+newer.getDaemonThreadCount()+"\t"+older.getDaemonThreadCount()+" Dif: "+(newer.getDaemonThreadCount()-older.getDaemonThreadCount()));

		HashSet<DebugThread> oldSet = new HashSet<DebugThread>();
		HashSet<DebugThread> newSet = new HashSet<DebugThread>();
		HashSet<DebugThread> survived = new HashSet<DebugThread>();
		HashSet<DebugThread> died; HashSet<DebugThread> added;
		HashSet<DebugThread> stuck = new HashSet<DebugThread>();

		HashMap<String, DebugThread> oldForInspectionAndCompare = new HashMap<String, DebugThread>();

		for (DebugThread dt:older.threads){
			oldSet.add(dt);
			oldForInspectionAndCompare.put(dt.getId(), dt);
		}
		for (DebugThread dt:newer.threads){
			newSet.add(dt);
			DebugThread oldThread = oldForInspectionAndCompare.get(dt.getId());
			if (oldThread!=null && oldThread.sameState(dt))
				stuck.add(oldThread);
			if (oldSet.contains(dt)){
				oldSet.remove(dt);
				survived.add(dt);
			}
		}

		died = oldSet;

		for (DebugThread dt:older.threads){
			if (newSet.contains(dt)){
				newSet.remove(dt);
			}
		}
		added = newSet;

		System.out.println("Survived "+survived.size()+", Died: "+died.size()+" Born: "+added.size()+", Stuck: "+stuck.size());
		System.out.println("=== DIED: ");
		for (Iterator<DebugThread> it = died.iterator(); it.hasNext(); ){
			System.out.println(it.next());
		}

		System.out.println("=== BORN: ");
		for (Iterator<DebugThread> it = added.iterator(); it.hasNext(); ){
			System.out.println(it.next());
		}

		System.out.println("=== STUCK: ");
		for (Iterator<DebugThread> it = stuck.iterator(); it.hasNext(); ){
			System.out.println(it.next());
		}
	}

	private static ThreadDump readStackTrace(String name) throws Exception{
		FileReader fReader = new FileReader(name);
		LineNumberReader reader = new LineNumberReader(fReader);

		String ts = reader.readLine();
		String desc = reader.readLine();
		reader.readLine();

		ThreadDump ret = new ThreadDump();
		ret.setDateString(ts);

		String line = null;
		int tc = 0;
		int lines = 0;
		boolean inThread = false;
		DebugThread current = null;
		while((line = reader.readLine())!=null){
			lines++;
			if (line.startsWith("JNI global references"))
				continue;
			if (line.length()==0){
				if (inThread){
					inThread = false;
				}else{
					//skip;
				}
			}else{
				if (!inThread){
					tc++;
					inThread = true;
					current = new DebugThread();
					ret.addThread(current);
					int nameStart = line.indexOf('"');
					int nameEnd   = line.lastIndexOf('"');
					try{
						current.setName(line.substring(nameStart+1, nameEnd));
						String otherStuff = line.substring(nameEnd+1);
						//System.out.println("OT "+otherStuff);
						if (otherStuff.indexOf("daemon")!=-1)
							current.setDaemon(true);
						String tt[] = StringUtils.tokenize(otherStuff, ' ');
						for (int t=0; t<tt.length; t++){
							if (tt[t].startsWith("tid")){
								String tid[] = StringUtils.tokenize(tt[t], '=');
								current.setId(tid[1]);
							}
						}
					}catch(Exception e){
						e.printStackTrace();
						System.out.println("Line: "+line);
					}
					//System.out.println(line);
				}else{
					current.addCall(line);
				}
			}
		}

		System.out.println("read "+tc+" threads in "+lines+" lines.");

		return ret;
	}

	public static class DebugThread{
		private String name;
		private String id;
		private List<String> calls = new ArrayList<String>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public boolean isDaemon() {
			return daemon;
		}

		public void setDaemon(boolean daemon) {
			this.daemon = daemon;
		}

		private boolean daemon;

		public void addCall(String call){
			calls.add(call);
		}

		public static String DUMP_FILTER[] = {
				"org.apache",
				"java.lang",
				"java.util",
				"java.",
				"org.jboss",
				"sun.reflect",
				"javax.faces",
				"sun.nio",
				"org.richfaces",
				"com.sun",


		};
		public void dumpOutShortend() {
			for (int i=0; i<calls.size(); i++){
				String call = calls.get(i);
				if (i<5){
					System.out.println(call);
				}else{
					boolean mayPass = true;
					for (int t = 0; t<DUMP_FILTER.length; t++){
						if (mayPass && call.trim().startsWith(("at " + DUMP_FILTER[t])))
							mayPass=false;
					}
					if (mayPass)
						System.out.println(call);
				}
			}
		}

		public void dumpOut() {
			for (int i=0; i<calls.size(); i++){
				String call = calls.get(i);
				System.out.println(call);
			}
		}

		@Override public String toString(){
			return getName()+" (tid: "+getId()+" ) "+calls.size()+" calls";
		}

		@Override public boolean equals(Object o){
			return o instanceof DebugThread && id.equals(((DebugThread)o).id);
		}

		public int hashCode(){
			return id.hashCode();
		}

		public boolean sameState(DebugThread otherThread){
			if (!equals(otherThread))
				return false;
			if (!(calls.size()==otherThread.calls.size()))
				return false;
			for (int i=0; i<calls.size(); i++)
				if (! calls.get(i).equals(otherThread.calls.get(i)))
					return false;
			return true;
		}
	}

	public static class ThreadDump{
		public String getDateString() {
			return dateString;
		}

		public void setDateString(String dateString) {
			this.dateString = dateString;
		}

		private String dateString;
		private List<DebugThread> threads = new ArrayList<DebugThread>();

		public void addThread(DebugThread t){
			threads.add(t);
		}

		public String toString(){
			return "ThreadDump @ "+dateString+" "+threads.size()+" threads.";
		}

		public int getTotalThreadCount(){
			return threads==null ? 0 : threads.size();
		}

		public int getDaemonThreadCount(){
			int ret = 0;
			for (DebugThread dt : threads){
				if (dt.isDaemon())
					ret++;
			}
			return ret;
		}

		public void dumpOut(){
			for (DebugThread dt : threads){
				System.out.println(dt);
			}
		}
		public void dumpOutShortened(){
			for (DebugThread dt : threads){
				System.out.println(dt);
				dt.dumpOutShortend();
				System.out.println();
			}
		}
	}
}

