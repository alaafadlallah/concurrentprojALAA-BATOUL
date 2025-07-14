import java.util.concurrent.Callable;

public class SimulationTask implements Callable<SimulationStats> {

    private final int simulations;

    public SimulationTask(int simulations){
        this.simulations = simulations;
    }

    @Override
    public SimulationStats call(){
        long totalDuration = 0;
        long totalPeak = 0;

        for(int i = 0; i< simulations; i++){
            SIRSimulation sim = new SIRSimulation(1000, 0.3, 0.1);
            sim.reset();
            sim.run();
            totalDuration += sim.getDuration();
            totalPeak += sim.getPeakInfected();
        }

        return new SimulationStats(totalDuration, totalPeak);
    }
}
