package ldt.load;

/**
 * TODO comment this class
 *
 * @author lrosenberg
 * @since 07.02.13 15:20
 */
public class BurnProcessor {
	public static void main(String a[]){
		burn(Integer.parseInt(a[0]));
	}

	public static void burn(int threadCount){
		for (int i=0; i<threadCount; i++){
			new BurnThread().start();
		}
	}

	static class BurnThread extends Thread{
		public void run(){
			long a = 0;
			while(true){
				a++;
				for (int i=0; i<1000000; i++){
					long b = a+1;
					long c = -1 *b;
					double d = b*b-4*a*c;
					double x1 = (b-Math.sqrt(d))/2/a;
				}

				if ((a/1000000000*1000000000)==a)
					System.out.println(a/1000000000);
			}
		}
	}
}
