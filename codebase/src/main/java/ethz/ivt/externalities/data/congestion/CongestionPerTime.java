package ethz.ivt.externalities.data.congestion;

public class CongestionPerTime {
    private double binSize;
    private int numBins;
    private double[] count;
    private double[] delayCaused;
    private double[] delayExperienced;
    private double[] congestionCaused;
    private double[] congestionExperienced;

    public CongestionPerTime(double binSize) {
        this.binSize = binSize; // not tested for anything other than 3600.
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

    public double[] getCount() {
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
}
