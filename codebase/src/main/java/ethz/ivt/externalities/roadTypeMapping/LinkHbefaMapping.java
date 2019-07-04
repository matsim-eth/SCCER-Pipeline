package ethz.ivt.externalities.roadTypeMapping;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionUtils;

public class LinkHbefaMapping extends HbefaRoadTypeMapping {
    @Override
    public String determineHebfaType(Link link) {
        return EmissionUtils.getHbefaRoadType(link);
    }
}
