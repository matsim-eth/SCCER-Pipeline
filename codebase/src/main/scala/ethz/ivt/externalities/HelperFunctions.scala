package ethz.ivt.externalities

import java.sql.Statement

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.apache.log4j.Logger
import org.matsim.api.core.v01.{Id, Scenario}
import org.matsim.contrib.emissions.EmissionUtils
import org.matsim.vehicles.{EngineInformation, EngineInformationImpl, Vehicle, VehicleType}

import scala.collection.JavaConverters._
import scala.collection.mutable
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

  val PETROL_KEY = "petrol (4S)"
  val DIESEL_KEY = "diesel"
  val ELECTRIC_KEY = "electric"
  val DESCRIPTION_TEMPLATE = "BEGIN_EMISSIONSPASSENGER_CAR;%s;%s;%sEND_EMISSIONS"

  def createVehicleTypeId(fuelType: String, euro: Int, size: String) : Id[VehicleType] =  {
    fuelType match {
      case "Electric" => EmissionUtils.ELECTRIC_VEHICLE_TYPE_ID
      case _          => Id.create(fuelType+ "_"+euro+"_"+size, classOf[VehicleType])
    }

  }

  def createEmissionsString(fuelType: String, euro: Int, size: String) : String =  {
    if (fuelType.equalsIgnoreCase("electric")) {
      return ""
    }

    val mappedFuelType = fuelType.toLowerCase match {
      case "diesel" => DIESEL_KEY
      case "gasoline" => PETROL_KEY
      case "hybrid (gasoline/diesel + electric)" => PETROL_KEY //no hybrid in hbefa database
    }

    val mapped_size = size match {
      case "small_car" => "<1,4L"
      case "medium_car" => "1,4-<2L"
      case "large_car" => "≥2L"
    }

    val mapped_euro = (mappedFuelType match {
      case PETROL_KEY => "PC P Euro-%d"
      case DIESEL_KEY => "PC D Euro-%d"
    }).format(euro)

    val emssions_string = DESCRIPTION_TEMPLATE.format(mappedFuelType, mapped_size, mapped_euro)

    return emssions_string
  }

  def createVehicleTypes(scenario: Scenario): Unit = {
    for (fuelType <- PETROL_KEY :: DIESEL_KEY :: Nil) {
      for (size <- "<1,4L" :: "1,4-<2L" :: "≥2L" :: Nil) {
        for (euro <- 0 to 6) {

          val vehicleTypeId = createVehicleTypeId(fuelType, euro, size)
          val vehicleType = scenario.getVehicles.getFactory.createVehicleType(vehicleTypeId)
          vehicleType.setDescription(createEmissionsString(fuelType, euro, size))
          scenario.getVehicles.addVehicleType(vehicleType)
        }
      }
    }
  }

  def loadVehiclesFromDatabase(scenario: Scenario, dbConfig: HikariConfig): Unit = {
    val ds = new HikariDataSource(dbConfig)

    val con = ds.getConnection()

    val vehicles_sql =
      """
        | select participant_id, fuel_type, euro_standard, size_category_short
        | from vehicle_information
        |
      """.stripMargin
    try {
      val stmt = con.createStatement()
      stmt.execute(vehicles_sql)
      val resultSet = stmt.getResultSet

      val vs = new Iterator[(Id[Vehicle], String, Int, String)] {
        def hasNext = resultSet.next()


        def next() = {
          val vid = Id.createVehicleId(resultSet.getString("participant_id"))
          val fuelType = resultSet.getString("fuel_type")
          val euro = resultSet.getInt("euro_standard")
          val size = resultSet.getString("size_category_short")

          (vid, fuelType, euro, size)
        }
      }.toStream.foreach { case (vid, fuelType, euro, size) =>
        val vehicleTypeId = createVehicleTypeId(fuelType, euro, size)

        if (!scenario.getVehicles.getVehicleTypes.containsKey(vehicleTypeId)) {
          val vehicleType = scenario.getVehicles.getFactory.createVehicleType(vehicleTypeId)
          val emissionsString = createEmissionsString(fuelType, euro, size)
          vehicleType.setDescription(emissionsString)

          scenario.getVehicles.addVehicleType(vehicleType)
        }
        val vehicleType = scenario.getVehicles.getVehicleTypes.get(vehicleTypeId)
        val v = scenario.getVehicles.getFactory.createVehicle(vid, vehicleType)
        scenario.getVehicles.addVehicle(v)
      }
    }
    finally {
      con.close()
    }


    }


}
