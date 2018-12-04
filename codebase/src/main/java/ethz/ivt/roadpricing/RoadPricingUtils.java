/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package ethz.ivt.roadpricing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;
import org.matsim.roadpricing.TollFactor;
import org.matsim.vehicles.Vehicle;

/**
 * Utility to create different road pricing schemes.
 * 
 * @author molloyj
 */
public class RoadPricingUtils {


    public RoadPricingScheme createDistanceToll(Network network) {
        RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
        scheme.setName("flat_distance_toll");
        scheme.setType("distance");
        network.getLinks().forEach((key, value) -> scheme.addLink(key)); //TODO: filter
        scheme.createAndAddCost(0, 30*60*60, 0.05);

        return scheme;
    }

    public RoadPricingScheme createVaryingDistanceToll(Network network) {

        TollFactor tollFactor = new TollFactor(){
            @Override public double getTollFactor(Id<Person> personId, Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
                return 1;
            }
        };

        RoadPricingScheme scheme = createDistanceToll(network);

  /*     RoadPricingSchemeUsingTollFactor schemeWfactor = new RoadPricingSchemeUsingTollFactor(scheme, tollFactor);
        scheme.setName("flat_distance_toll");
        scheme.setType("distance");
        network.getLinks().forEach((key, value) -> scheme.addLink(key)); //TODO: filter
        scheme.createAndAddCost(0, 30*60*60, 0.05);
*/
        return scheme;
    }

    public void saveScheme(RoadPricingScheme roadPricingScheme, String outfilename) {
        new RoadPricingWriterXMLv1(roadPricingScheme).writeFile(outfilename);
    }
}
