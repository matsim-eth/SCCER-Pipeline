package ethz.ivt.externalities.data;

public interface AggregateDataPerTime<T> {

//    int getNumBins();
//    double getBinSize();
//    Map<Id<T>, Map<String, double[]>> getData();
//
//    double getValue(Id<T> id, int timeBin, String attribute);
//    void setValue(Id<T> id, int timeBin, String attribute, double value);
//
//    // setup
//    void setUpTimeBins(Set<Id<T>> ids);

    //reader and writer
    void loadDataFromCsv(String input);
    void writeDataToCsv(String output);

}