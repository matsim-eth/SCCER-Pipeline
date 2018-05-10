package ethz.ivt.externalities.data;

public enum CongestionField {
    COUNT("count"), DELAY_EXPERIENCED("delay_experienced"), DELAY_CAUSED("delay_caused");;

    private final String field;

    CongestionField(String field) { this.field = field; }

    public String getText() {return this.field;}
}
