package ethz.ivt.externalities.data;

public enum CountPerLinkField {
    COUNT("count"), DISTANCE("distance"), TIME("time");

    private final String field;

    CountPerLinkField(String field) { this.field = field; }

    public String getText() {return this.field;}
}
