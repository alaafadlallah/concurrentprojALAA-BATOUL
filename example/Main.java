package montecarlo;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Executors;

public class main {
    static int total_simulations = 1000000;
    static int thread_count = 8;
    static int sims_per_thread = total_simulations/thread_count;

    public main(String[] args) throws InterruptedException, ExecutionException{
        ExecutorService executor = Executors.newFixedThreadPool(thread_count);
        List<Future<SimulationStats>> futures = new ArrayList<>();
        for(int i = 0; i< thread_count; i++) {
            futures.add(executor.submit(new SimulationTask(sims_per_thread)));
        }
            long totalDuration = 0;
            long totalPeak = 0;

            for(Future<SimulationStats> future : futures) {
                SimulationStats stats = future.get();
                totalDuration += stats.totalDuration;
                totalPeak += stats.totalPeak;

            }
                executor.shutdown();

                System.out.println("Average Duration: " + (totalDuration / (double) total_simulations));
                System.out.println("Average Duration: " + (totalPeak / (double) total_simulations));

}