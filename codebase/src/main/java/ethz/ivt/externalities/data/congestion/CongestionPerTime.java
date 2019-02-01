package ethz.ivt.externalities.data.congestion;

public class CongestionPerTime {
    private double binSize = 60.;
    private int numBins;
    private double[] count;
    private double[] delayCaused;
    private double[] delayExperienced;
    private double[] congestionCaused;
    private double[] congestionExperienced;

    public CongestionPerTime() {
        this(60.);
    }

    public CongestionPerTime(double binSize) {
        this.binSize = binSize;
        this.numBins = (int) (30 * 3600 / binSize);
        this.count = new double[this.numBins];
        this.delayCaused = new double[this.numBins];
        this.delayExperienced = new double[this.numBins];
        this.congestionCaused = new double[this.numBins];
        this.congestionExperienced = new double[this.numBins];
    }

    public double getBinSize() {
        return binSize;
    }

    public int getNumBins() {
        return numBins;
    }

    /*
     * Get all values.
     */

    public double[] getCounts() {
        return count;
    }

    public double[] getDelayCaused() {
        return delayCaused;
    }

    public double[] getDelayExperienced() {
        return delayExperienced;
    }

    public double[] getCongestionCaused() {
        return congestionCaused;
    }

    public double[] getCongestionExperienced() {
        return congestionExperienced;
    }

    /*
     * Get values at a specified time.
     */

    public double getCountAtTime(double time) {
        return count[this.getTimeBin(time)];
    }

    public double getDelayCausedAtTime(double time) {
        return delayCaused[this.getTimeBin(time)];
    }

    public double getDelayExperiencedAtTime(double time) {
        return delayExperienced[this.getTimeBin(time)];
    }

    public double getCongestionCausedAtTime(double time) {
        return congestionCaused[this.getTimeBin(time)];
    }

    public double getCongestionExperiencedAtTime(double time) {
        return congestionExperienced[this.getTimeBin(time)];
    }

    /*
     * Get values at a specified time bin.
     */

    public double getCountAtTimeBin(int timeBin) {
        if (timeBin < count.length) {
            return count[timeBin];
        }
        return 0.0;
    }

    public double getDelayCausedAtTimeBin(int timeBin) {
        if (timeBin < delayCaused.length) {
            return delayCaused[timeBin];
        }
        return 0.0;
    }

    public double getDelayExperiencedAtTimeBin(int timeBin) {
        if (timeBin < delayExperienced.length) {
            return delayExperienced[timeBin];
        }
        return 0.0;
    }

    public double getCongestionCausedAtTimeBin(int timeBin) {
        if (timeBin < congestionCaused.length) {
            return congestionCaused[timeBin];
        }
        return 0.0;
    }

    public double getCongestionExperiencedAtTimeBin(int timeBin) {
        if (timeBin < congestionExperienced.length) {
            return congestionExperienced[timeBin];
        }
        return 0.0;
    }

    /*
     * Set values for a specified time.
     */

    public void setCountAtTime(double value, double time) {
        this.count[this.getTimeBin(time)] = value;
    }

    public void setDelayCausedAtTime(double value, double time) {
        this.delayCaused[this.getTimeBin(time)] = value;
    }

    public void setDelayExperiencedAtTime(double value, double time) {
        this.delayExperienced[this.getTimeBin(time)] = value;
    }

    public void setCongestionCausedAtTime(double value, double time) {
        this.congestionCaused[this.getTimeBin(time)] = value;
    }

    public void setCongestionExperiencedAtTime(double value, double time) {
        this.congestionExperienced[this.getTimeBin(time)] = value;
    }

    /*
     * Set values for a specified time bin.
     */

    public void setCountAtTimeBin(double value, int timeBin) {
        if (timeBin < this.count.length) {
            this.count[timeBin] = value;
        }
    }

    public void setDelayCausedAtTimeBin(double value, int timeBin) {
        if (timeBin < this.delayCaused.length) {
            this.delayCaused[timeBin] = value;
        }
    }

    public void setDelayExperiencedAtTimeBin(double value, int timeBin) {
        if (timeBin < this.delayExperienced.length) {
            this.delayExperienced[timeBin] = value;
        }
    }

    public void setCongestionCausedAtTimeBin(double value, int timeBin) {
        if (timeBin < this.congestionCaused.length) {
            this.congestionCaused[timeBin] = value;
        }
    }

    public void setCongestionExperiencedAtTimeBin(double value, int timeBin) {
        if (timeBin < this.congestionExperienced.length) {
            this.congestionExperienced[timeBin] = value;
        }
    }

    /*
     * Add values at a specified time.
     */

    public void addCountAtTime(double value, double time) {
        this.count[this.getTimeBin(time)] += value;
    }

    public void addDelayCausedAtTime(double value, double time) {
        this.delayCaused[this.getTimeBin(time)] += value;
    }

    public void addDelayExperiencedAtTime(double value, double time) {
        this.delayExperienced[this.getTimeBin(time)] += value;
    }

    public void addCongestionCausedAtTime(double value, double time) {
        this.congestionCaused[this.getTimeBin(time)] += value;
    }

    public void addCongestionExperiencedAtTime(double value, double time) {
        this.congestionExperienced[this.getTimeBin(time)] += value;
    }

    /*
     * Add values at a specified time bin.
     */

    public void addCountAtTimeBin(double value, int timeBin) {
        if (timeBin < this.count.length) {
            this.count[timeBin] += value;
        }
    }

    public void addDelayCausedAtTimeBin(double value, int timeBin) {
        if (timeBin < this.delayCaused.length) {
            this.delayCaused[timeBin] += value;
        }
    }

    public void addDelayExperiencedAtTimeBin(double value, int timeBin) {
        if (timeBin < this.delayExperienced.length) {
            this.delayExperienced[timeBin] += value;
        }
    }

    public void addCongestionCausedAtTimeBin(double value, int timeBin) {
        if (timeBin < this.congestionCaused.length) {
            this.congestionCaused[timeBin] += value;
        }
    }

    public void addCongestionExperiencedAtTimeBin(double value, int timeBin) {
        if (timeBin < this.congestionExperienced.length) {
            this.congestionExperienced[timeBin] += value;
        }
    }

    /*
     * Get time bin.
     */

    private int getTimeBin(double time) {
        /*
         * Agents who end their first activity before the simulation has started
         * will depart in the first time step.
         */
        if (time <= 0.0) return 0;

        /*
         * Calculate the bin for the given time.
         */
        int bin = (int) (time / this.binSize);

        /*
         * Anything larger than 30 hours gets placed in the final bin.
         */
        return Math.min(bin, this.numBins-1);
    }
}
