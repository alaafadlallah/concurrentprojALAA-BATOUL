import java.util.concurrent.RecursiveTask;

public class SimulationForkTask extends RecursiveTask<SimulationStats> {
    private final int simulations;
    // Much higher threshold - aim for reasonable chunk sizes
    private final int threshold = 50000; // Was 5000, now 50K

    public SimulationForkTask(int simulations) {
        this.simulations = simulations;
    }

    @Override
    protected SimulationStats compute() {
        if (simulations <= threshold) {
            // Run directly with object reuse
            long totalDuration = 0;
            long totalPeak = 0;

            // Reuse simulation object
            SIRSimulation sim = new SIRSimulation(1000, 0.3, 0.1);

            for (int i = 0; i < simulations; i++) {
                sim.reset();
                sim.run();
                totalDuration += sim.getDuration();
                totalPeak += sim.getPeakInfected();
            }
            return new SimulationStats(totalDuration, totalPeak);
        } else {
            // Split into two subtasks
            int mid = simulations / 2;
            SimulationForkTask left = new SimulationForkTask(mid);
            SimulationForkTask right = new SimulationForkTask(simulations - mid);

            left.fork();                     // run left in parallel
            SimulationStats rightResult = right.compute(); // run right now
            SimulationStats leftResult = left.join();      // wait for left

            return new SimulationStats(
                    leftResult.totalDuration + rightResult.totalDuration,
                    leftResult.totalPeak + rightResult.totalPeak
            );
        }
    }
}