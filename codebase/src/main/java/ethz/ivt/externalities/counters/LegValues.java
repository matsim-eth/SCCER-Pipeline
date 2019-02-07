package ethz.ivt.externalities.counters;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class LegValues {
    Map<String, Double> personId2Leg2Values = new HashMap<>();
    private LocalDateTime timestamp = LocalDateTime.now();
    private String mode;

    public void addTempValues(Map<String, Double> values) {
        personId2Leg2Values.putAll(values);

    }

    public void put(String key, double value) {
        personId2Leg2Values.put(key, value);
    }

    public void putIfAbsent(String key, double v) {
        personId2Leg2Values.putIfAbsent(key, v);
    }

    public double get(String key) {
        if (personId2Leg2Values.containsKey(key)) {
            return personId2Leg2Values.get(key);
        } else {
            return Double.NaN;
        }
    }

    public void putAll(Map<String, Double> map) {
        personId2Leg2Values.putAll(map);
    }

    public boolean containsKey(String key) {
        return personId2Leg2Values.containsKey(key);
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Stream<String> keys() {
        return personId2Leg2Values.keySet().stream();
    }
}
