import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    static int total_simulations = 1000000;
    static int[] default_thread_counts = {1, 2, 4, 8, 16, 32};

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        int[] thread_counts = parseThreadCounts(args);

        System.out.println("Total Simulations: " + total_simulations);
        System.out.println("Thread counts to test: " + Arrays.toString(thread_counts));
        System.out.println("=".repeat(70));


        System.out.println("Running sequential baseline...");
        long startSeq = System.nanoTime();
        SimulationStats sequentialStats = runSimulationsSequential(total_simulations);
        long endSeq = System.nanoTime();
        double timeSeq = (endSeq - startSeq) / 1_000_000.0;

        System.out.printf("Sequential Time: %.2f ms\n\n", timeSeq);


        System.out.printf("%-8s %-12s %-12s %-12s %-12s %-12s\n",
                "Threads", "Exec Time", "Speedup", "Efficiency", "FJ Time", "FJ Speedup");
        System.out.println("-".repeat(70));

        List<String> csvData = new ArrayList<>();
        csvData.add("Threads,ExecutorTime,ExecutorSpeedup,ExecutorEfficiency,ForkJoinTime,ForkJoinSpeedup,ForkJoinEfficiency");

        for (int thread_count : thread_counts) {
            double[] results = testThreadCount(thread_count, timeSeq);

            double execTime = results[0];
            double execSpeedup = results[1];
            double execEfficiency = results[2];
            double fjTime = results[3];
            double fjSpeedup = results[4];
            double fjEfficiency = results[5];

            System.out.printf("%-8d %-12.2f %-12.2fx %-12.1f%% %-12.2f %-12.2fx\n",
                    thread_count, execTime, execSpeedup, execEfficiency, fjTime, fjSpeedup);


            csvData.add(String.format("%d,%.2f,%.2f,%.1f,%.2f,%.2f,%.1f",
                    thread_count, execTime, execSpeedup, execEfficiency, fjTime, fjSpeedup, fjEfficiency));
        }


        writeResultsToCSV(csvData);


        generatePlotScript();

        System.out.println("\nResults saved to 'results.csv'");
        System.out.println("Run 'python plot_results.py' to generate graphs");
    }

    private static int[] parseThreadCounts(String[] args) {
        if (args.length == 0) {
            return default_thread_counts;
        }

        try {
            return Arrays.stream(args)
                    .mapToInt(Integer::parseInt)
                    .sorted()
                    .toArray();
        } catch (NumberFormatException e) {
            System.out.println("Invalid arguments. Using defaults.");
            return default_thread_counts;
        }
    }

    private static double[] testThreadCount(int thread_count, double baselineTime)
            throws InterruptedException, ExecutionException {


        long startPar = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(thread_count);
        List<Future<SimulationStats>> futures = new ArrayList<>();

        int sims_per_thread = total_simulations / thread_count;
        for (int i = 0; i < thread_count; i++) {
            int simsForThisThread = (i == thread_count - 1) ?
                    total_simulations - (sims_per_thread * i) : sims_per_thread;
            futures.add(executor.submit(new SimulationTask(simsForThisThread)));
        }

        for (Future<SimulationStats> future : futures) {
            future.get();
        }
        executor.shutdown();

        long endPar = System.nanoTime();
        double execTime = (endPar - startPar) / 1_000_000.0;
        double execSpeedup = baselineTime / execTime;
        double execEfficiency = execSpeedup / thread_count * 100;


        long startFJ = System.nanoTime();
        ForkJoinPool pool = new ForkJoinPool(thread_count);
        SimulationForkTask task = new SimulationForkTask(total_simulations);
        pool.invoke(task);
        pool.shutdown();

        long endFJ = System.nanoTime();
        double fjTime = (endFJ - startFJ) / 1_000_000.0;
        double fjSpeedup = baselineTime / fjTime;
        double fjEfficiency = fjSpeedup / thread_count * 100;

        return new double[]{execTime, execSpeedup, execEfficiency, fjTime, fjSpeedup, fjEfficiency};
    }

    private static void writeResultsToCSV(List<String> csvData) {
        try (FileWriter writer = new FileWriter("results.csv")) {
            for (String line : csvData) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }

    private static void generatePlotScript() {
        String pythonScript = """
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Read the results
df = pd.read_csv('results.csv')

# Create figure with subplots
fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 12))
fig.suptitle('SIR Simulation Parallel Performance Analysis', fontsize=16)

# 1. Speedup Comparison
ax1.plot(df['Threads'], df['ExecutorSpeedup'], 'b-o', label='ExecutorService', linewidth=2)
ax1.plot(df['Threads'], df['ForkJoinSpeedup'], 'r-s', label='ForkJoinPool', linewidth=2)
ax1.plot(df['Threads'], df['Threads'], 'k--', alpha=0.5, label='Perfect Speedup')
ax1.set_xlabel('Number of Threads')
ax1.set_ylabel('Speedup')
ax1.set_title('Speedup vs Thread Count')
ax1.legend()
ax1.grid(True, alpha=0.3)

# 2. Efficiency Comparison
ax2.plot(df['Threads'], df['ExecutorEfficiency'], 'b-o', label='ExecutorService', linewidth=2)
ax2.plot(df['Threads'], df['ForkJoinEfficiency'], 'r-s', label='ForkJoinPool', linewidth=2)
ax2.axhline(y=100, color='k', linestyle='--', alpha=0.5, label='Perfect Efficiency')
ax2.set_xlabel('Number of Threads')
ax2.set_ylabel('Efficiency (%)')
ax2.set_title('Efficiency vs Thread Count')
ax2.legend()
ax2.grid(True, alpha=0.3)

# 3. Execution Time Comparison
ax3.plot(df['Threads'], df['ExecutorTime'], 'b-o', label='ExecutorService', linewidth=2)
ax3.plot(df['Threads'], df['ForkJoinTime'], 'r-s', label='ForkJoinPool', linewidth=2)
ax3.set_xlabel('Number of Threads')
ax3.set_ylabel('Execution Time (ms)')
ax3.set_title('Execution Time vs Thread Count')
ax3.legend()
ax3.grid(True, alpha=0.3)

# 4. Bar Chart of Best Speedups
best_exec = df.loc[df['ExecutorSpeedup'].idxmax()]
best_fj = df.loc[df['ForkJoinSpeedup'].idxmax()]

methods = ['ExecutorService\\n(Best)', 'ForkJoinPool\\n(Best)']
speedups = [best_exec['ExecutorSpeedup'], best_fj['ForkJoinSpeedup']]
threads = [best_exec['Threads'], best_fj['Threads']]

bars = ax4.bar(methods, speedups, color=['blue', 'red'], alpha=0.7)
ax4.set_ylabel('Best Speedup')
ax4.set_title('Best Performance Comparison')
ax4.grid(True, alpha=0.3)

# Add value labels on bars
for bar, speedup, thread in zip(bars, speedups, threads):
    height = bar.get_height()
    ax4.text(bar.get_x() + bar.get_width()/2., height,
             f'{speedup:.1f}x\\n({int(thread)} threads)',
             ha='center', va='bottom')

plt.tight_layout()
plt.savefig('performance_analysis.png', dpi=300, bbox_inches='tight')
plt.show()

print("\\nPerformance Analysis Summary:")
print(f"Best ExecutorService: {best_exec['ExecutorSpeedup']:.2f}x speedup with {int(best_exec['Threads'])} threads")
print(f"Best ForkJoinPool: {best_fj['ForkJoinSpeedup']:.2f}x speedup with {int(best_fj['Threads'])} threads")
print("\\nGraph saved as 'performance_analysis.png'")
""";

        try (FileWriter writer = new FileWriter("plot_results.py")) {
            writer.write(pythonScript);
        } catch (IOException e) {
            System.err.println("Error writing Python script: " + e.getMessage());
        }
    }

    public static SimulationStats runSimulationsSequential(int simulations) {
        long totalDuration = 0;
        long totalPeak = 0;

        SIRSimulation sim = new SIRSimulation(1000, 0.3, 0.1);
        for (int i = 0; i < simulations; i++) {
            sim.reset();
            sim.run();
            totalDuration += sim.getDuration();
            totalPeak += sim.getPeakInfected();
        }

        return new SimulationStats(totalDuration, totalPeak);
    }
}