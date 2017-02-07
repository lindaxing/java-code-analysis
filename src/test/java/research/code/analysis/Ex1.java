package research.code.analysis;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class Ex1 {

    public static void main(String[] args) {
        replaceSpace();
        
        int threadNums = 3;
        int count = 100000000;
        
        long  timeInNano2 = test(threadNums, count, Ex1::longAddInc);
        long  timeInNano1 = test(threadNums, count, Ex1::atomicInc);
        
        System.out.printf("%d times increment "
                + "\nAtomicLong:%20d %15d"
                + "\n LongAdder:%20d %15d\n", 
                threadNums*count,timeInNano1,atomicLong.get(),timeInNano2,longAdder.longValue());
    }

    
    public static long test(int threadNums,final int count,final IntConsumer consumer){
        ExecutorService executorService = Executors.newFixedThreadPool(threadNums); 
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(threadNums);
        final CountDownLatch countDownLatch = new CountDownLatch(threadNums);
        AtomicLong totalTime = new AtomicLong();
        
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    cyclicBarrier.await();
                    long startTime = System.nanoTime(); 
                    for (int i = 0; i < count; i++) {
                        consumer.accept(i);
                    }
                    long endTime = System.nanoTime(); 
                    totalTime.addAndGet((endTime)-(startTime));
                    countDownLatch.countDown();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
        };

        for (int i = 0; i < threadNums; i++) {
            executorService.execute(runnable);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        return totalTime.get();

    }

    public static void atomicInc(int i){
        atomicLong.incrementAndGet();
    }
    
    public static void longAddInc(int i){
        longAdder.add(1L);
    }
    
    
    private static AtomicLong atomicLong = new AtomicLong(0);
    private static LongAdder  longAdder  = new LongAdder();
    
    
    /**
     * 
     */
    private static void replaceSpace() {
        String s = "lindxasdmmlasd";
        StringBuilder sb = new StringBuilder();
        s.chars().forEach(
            x -> {
                if(x == ' ') {
                    sb.append("%20");
                } else {
                    sb.append((char)x);
                }
            }
        );
        System.out.println(sb);
        
    }


}