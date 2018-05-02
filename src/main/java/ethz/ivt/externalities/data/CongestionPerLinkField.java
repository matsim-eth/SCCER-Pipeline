package ethz.ivt.externalities.data;

public enum CongestionPerLinkField {
    COUNT("count"), DELAY("delay");

    private final String field;

    CongestionPerLinkField(String field) { this.field = field; }

    public String getText() {return this.field;}
}
