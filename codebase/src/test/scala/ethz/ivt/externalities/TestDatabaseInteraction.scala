package ethz.ivt.externalities

import java.io.FileWriter
import java.sql.SQLException
import java.time.{LocalDate, LocalDateTime}
import java.util
import java.util.Properties

import akka.actor.ActorSystem
import com.zaxxer.hikari.HikariConfig
import ethz.ivt.externalities.actors.{CREATE_DB, Externalities, ExternalitiesWriterActor}
import ethz.ivt.externalities.counters.{ExternalityCounter, LegValues}
import ethz.ivt.externalities.data.{LatLon, TripLeg, TripRecord}
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.Person

import scala.util.Success

object TestDatabaseInteraction {

  def main(args: Array[String]): Unit = {


 //   val connString = "jdbc:postgresql://localhost:5432/sbb-green"
    val dbProps = new Properties()

    dbProps.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource")
    dbProps.setProperty("dataSource.portNumber", "5432")
    dbProps.setProperty("dataSource.serverName", "localhost")
    dbProps.setProperty("dataSource.user", "postgres")
    dbProps.setProperty("dataSource.password", "password")
    dbProps.setProperty("dataSource.databaseName", "sbb-green")
    dbProps.setProperty("dataSource.currentSchema", "public")

    dbProps.store(new FileWriter("dbProperties.properties"), "Local Postgres Settings")

//    dbProps.setConnectionTestQuery("SELECT 1")
    val config = new HikariConfig(dbProps)

    val writerActorProps = ExternalitiesWriterActor.buildPostgres(config)

    val _system = ActorSystem("MainEngineActor")

    val writerActor = _system.actorOf(writerActorProps, "ExternalityWriter")

    val leg  = TripLeg(
      0,
      LocalDateTime.now(),
      LocalDateTime.now().plusHours(1),
      LatLon(0.0, 0.0), LatLon(0.0, 0.0),
      "Car",
      List.empty
    )

    val tr = TripRecord("test", 0, LocalDate.now(), List(leg))

    class MockEC extends ExternalityCounter(null, LocalDate.now().minusYears(1).toString) {
      import collection.JavaConverters._

      override def getPersonId2Leg() : util.Map[Id[Person], util.List[LegValues]] = {
        val headers = "PersonId;Date;Leg;Mode;StartTime;EndTime;Distance;delay_caused;delay_experienced;clock_time;matsim_time;matsim_delay;CO2_total_;N2O;CO;CH4;NO2;Pb;NOx;Benzene;SO2;CO2_rep_;NH3;HC;FC;PM_urban;PN;NMHC;PM_rural;NOx_costs;Noise_costs;PM_building_damage_costs;CO2_costs;PM_health_costs;Zinc_costs"
        val data = "1723;2016-12-17;1;Car;51529.0000;52060.0000;42315.3256;170.8315;129.5345;512.1594;5454.9129;4966.1800;7947.7154;0.0337;10.6783;0.0183;0.1199;0.0026;2.3971;0.0282;0.0404;7947.5200;0.8382;0.2178;2523.0842;0.0130;29933618993465.2270;0.1995;0.0195;0.0076;0.4221;0.0025;1.0352;0.0173;0.0598"

        val values = headers.split(";").zip(data.split(";")).drop(4).toMap

        val legValues = new LegValues()
        legValues.setMode("Car")
        values.foreach{ case (k,v) => {
          legValues.put(k,v.toDouble)
        }}
        val map = new util.HashMap[Id[Person], util.List[LegValues]]()
        map.put(Id.createPersonId("1723aaa"), List.apply(legValues).asJava)
        map
      }
    }

    import akka.pattern.ask
    import akka.util.Timeout
    import scala.concurrent.duration._
    implicit val timeout = Timeout(5 seconds)
    implicit val ec = _system.dispatcher


    writerActor ? CREATE_DB(replace = true) flatMap { _ =>
        println("database loaded or created successfully")
        writerActor ? Externalities(tr, new MockEC())
    } recover {
        case e : SQLException =>
          throw new RuntimeException(e.getNextException)
          _system terminate
    } onComplete {
      case Success(x : Map[Id[Person], Array[Int]]) =>
      _system terminate
    }

  }

}
