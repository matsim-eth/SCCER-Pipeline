package ethz.ivt.externalities.data;

public enum CongestionPerPersonField {
    DELAY_EXPERIENCED("delay_experienced"), DELAY_CAUSED("delay_caused");

    private final String field;

    CongestionPerPersonField(String field) { this.field = field; }

    public String getText() {return this.field;}
}
