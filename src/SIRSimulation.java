import java.util.concurrent.ThreadLocalRandom;

public class SIRSimulation {

    private int S, I, R, N;
    private final double beta, gamma;
    private int peakInfected = 0;
    private int days = 0;

    public SIRSimulation(int population, double beta, double gamma){
        this.N = population;
        this.beta = beta;
        this.gamma = gamma;
        reset();
    }


    public void reset() {
        this.S = N - 1;
        this.I = 1;
        this.R = 0;
        this.peakInfected = 0;
        this.days = 0;
    }

    public void run() {

        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (I > 0 && days < 365) {
            int newInfections = 0;
            for (int j = 0; j < I; j++) {
                if (random.nextDouble()  < beta * S / (double) N) {
                    newInfections++;
                }
            }

            int recoveries = 0;
            for (int j = 0; j < I; j++) {
                if (random.nextDouble() < gamma) {
                    recoveries++;
                }
            }


            newInfections = Math.min(newInfections, S);
            recoveries = Math.min(recoveries, I);

            S -= newInfections;
            I += newInfections - recoveries;
            R += recoveries;

            peakInfected = Math.max(peakInfected, I);
            days++;
        }
    }
    public int getDuration(){
        return days;
    }

    public int getPeakInfected(){
        return peakInfected;
    }

}
