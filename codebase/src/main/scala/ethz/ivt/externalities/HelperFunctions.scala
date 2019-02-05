package ethz.ivt.externalities

import org.apache.log4j.Logger
import org.matsim.api.core.v01.{Id, Scenario}
import org.matsim.vehicles.VehicleType

import scala.collection.JavaConverters._
import scala.io.Source

object HelperFunctions {
  val logger = Logger.getLogger(this.getClass)

  def addVehiclesToScenario(scenario: Scenario, car_ownership_file: String) : Unit = {
    val input = Source.fromFile(car_ownership_file).getLines().map(_.split(",").map(_.stripPrefix("\"").stripSuffix("\"")))
    val headers = input.next().tail
    val car_ownership_ds = input.map(ss => ss.head -> headers.zip(ss.tail).toMap).toMap

    //add vehicle for id, and first car ->  car_1_engine
    val vehicleTypes = scenario.getVehicles.getVehicleTypes.asScala.map{ case (k,v) => (k.toString, v)}

    car_ownership_ds.foreach { case (k, value_map) =>
      val vid = Id.createVehicleId(k)

      val vehicleTypeString = value_map.get("car_1_engine")
      val defaultVehicle : VehicleType = vehicleTypes.get("Benzin").head

      val vehicleType : VehicleType = value_map.get("car_1_engine").flatMap(vehicleTypes.get).getOrElse({
        logger.warn(s"vehicle $vehicleTypeString not found")
        defaultVehicle
      })

      val v = scenario.getVehicles.getFactory.createVehicle(vid, vehicleType)
      scenario.getVehicles.addVehicle(v)
    }
    //default back to petrol if not found.

  }


}
