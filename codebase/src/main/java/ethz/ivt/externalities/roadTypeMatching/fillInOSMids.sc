

import org.matsim.core.config.ConfigUtils
import org.matsim.core.scenario.ScenarioUtils

import scala.collection.JavaConverters._
import scala.io.Source


val config = ConfigUtils.loadConfig("""C:\Projects\SCCER_project\scenarios\switzerland_10pct\switzerland_config_w_emissions.xml""")
val scenario = ScenarioUtils.loadScenario(config)

val n2 = scenario.getNetwork.getLinks.values().asScala.filter(l => {
  (
    l.getAttributes.getAttribute("osm:way:highway") == "unknown" ||
      l.getAttributes.getAttribute("osm:way:highway") == null ||
      l.getAttributes.getAttribute("osm:way:highway") == "unclassified" ) && l.getAllowedModes.contains("car")
})
//n2.filter(_.getFreespeed > (70/3.6)).map(_.getAttributes.getAttribute("osm:way:id"))

val motorway_osm_ids = (Source.fromFile("""C:\Projects\SCCER_project\scenarios\switzerland_10pct\osm_motorway_and_trunk_ids.txt""")
  .getLines().map(l => {
  val ss = l.split(',')
  (ss(0).toInt, ss(1))
}).toMap)
val matsim_unknown_osm_ids = n2.map(_.getAttributes.getAttribute("osm:way:id")).map(_.toString.toInt).toSet
matsim_unknown_osm_ids.intersect(motorway_osm_ids.keys.toSet)

n2.foreach { l =>
  val t = motorway_osm_ids.get(l.getAttributes.getAttribute("osm:way:id").toString.toInt) //find matching type
  t.foreach(l.getAttributes.putAttribute("osm:way:highway", _)) //if one is found, put it on the link
}
//7509 links in OSM that are motorway or trunk - Relation: Motorways and Trunks in Switzerland (27877)
//overpass api query:
/*[out:csv( ::"id",
         // ::type,
         "highway";
    true; "," )];

relation(27877) ;
>>;
way(r);
out;
*/
//7255 links from matsim with no type which should be motorway or trunk*/