package ethz.ivt.externalities.counters;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class LegValues {
    Map<String, Double> personId2Leg2Values = new HashMap<>();
    private String triplegId;
    private double distance;
    private LocalDateTime updatedAt;

    public LegValues(LocalDateTime timestamp, String mode, LocalDateTime updatedAt) {
        this.timestamp = timestamp;
        this.mode = mode;
        this.updatedAt = updatedAt;
    }

    private LocalDateTime timestamp;
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
        return personId2Leg2Values.getOrDefault(key, 0.0);
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public java.util.Set<String> keys() {
        return personId2Leg2Values.keySet();
    }

    public String getTriplegId() {
        return triplegId;
    }

    public void setTriplegId(String triplegId) {
        this.triplegId = triplegId;
    }

    public double getDistance () {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
}
