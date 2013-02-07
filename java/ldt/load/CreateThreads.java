package ldt.load;

/**
 * TODO comment this class
 *
 * @author lrosenberg
 * @since 07.02.13 17:28
 */
public class CreateThreads {
	public static void main(String a[]){
		int count = 0;
		while(true){
			count++;
			System.out.println("Creating thread "+count);
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					while(true){
						try{
							Thread.currentThread().sleep(100000);
						}catch(InterruptedException ignored){}
					}
				}
			});
			t.start();
		}
	}
}
