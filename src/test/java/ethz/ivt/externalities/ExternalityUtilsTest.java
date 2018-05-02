package ethz.ivt.externalities;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExternalityUtilsTest {

    @Test
    public void getTimeBin() {
        assertEquals("Computed time bin is incorrect!", 0, ExternalityUtils.getTimeBin(-100.0,3600.0));
        assertEquals("Computed time bin is incorrect!", 0, ExternalityUtils.getTimeBin(50.0,3600.0));
        assertEquals("Computed time bin is incorrect!", 1, ExternalityUtils.getTimeBin(3650.0,3600.0));
        assertEquals("Computed time bin is incorrect!", 2, ExternalityUtils.getTimeBin(7250.0,3600.0));
        assertEquals("Computed time bin is incorrect!", 10, ExternalityUtils.getTimeBin(36050.0,3600.0));
        assertEquals("Computed time bin is incorrect!", 10, ExternalityUtils.getTimeBin(36050.0,3600.0));
        assertEquals("Computed time bin is incorrect!", 19, ExternalityUtils.getTimeBin(72000.0,3600.0));
    }

    @Test
    public void getDate() {
        assertTrue("Computed wrong date!", ExternalityUtils.getDate("01012000_events.xml.gz").equals("01012000"));
    }
}