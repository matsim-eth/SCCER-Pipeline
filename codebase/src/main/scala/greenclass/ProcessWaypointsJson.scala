package greenclass

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.time.format.DateTimeFormatter

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.graphhopper.util.GPXEntry
import ethz.ivt.externalities.MeasureExternalities
import ethz.ivt.externalities.actors.TraceActor.JsonFile
import ethz.ivt.externalities.actors._
import ethz.ivt.externalities.aggregation.CongestionAggregator
import ethz.ivt.externalities.counters.ExternalityCostCalculator
import ethz.ivt.graphhopperMM.{GHtoEvents, MATSimMMBuilder}
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.{Id, Scenario, TransportMode}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.algorithms.EventWriterXML
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84

import scala.collection.JavaConverters._
import scala.io.{BufferedSource, Source}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.matsim.api.core.v01.events._
import org.matsim.api.core.v01.network.Link
import org.matsim.api.core.v01.population.Person

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object ProcessWaypointsJson {

  Logger.getLogger("com.graphhopper.matching.MapMatching").setLevel(Level.WARN)
  Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)

  def main(args: Array[String]) {

    //val args = {"P:\\Projekte\\SCCER\\switzerland_10pct\\switzerland_config_no_facilities.xml", "C:\\Projects\\spark\\green_class_swiss_triplegs.csv","C:\\Projects\\spark\\green_class_waypoints.csv","C:\\Projects\\SCCER_project\\output_gc"}


    val logger = Logger.getLogger(this.getClass)
    val argI = args.iterator
    val config = ConfigUtils.loadConfig(argI.next(), new EmissionsConfigGroup)
    val costValuesFile = Paths.get(argI.next())
    val trips_folder = Paths.get(argI.next())
    val output_dir = Paths.get(argI.next())

    val overwrite: Boolean = args.applyOrElse(argI.next().toInt, (_: Int) => "False").toBoolean

    val scenario: Scenario = ScenarioUtils.loadScenario(config)
    val congestionAggregator = ??? //CongestionAggregator.build(programConfig)
    val ecc = new ExternalityCostCalculator(costValuesFile.toString)

    val processWaypointsJson = new ProcessWaypointsJson(scenario)

    val me : MeasureExternalities = new MeasureExternalities(scenario, null, ecc)


    val _system = ActorSystem("MainEngineActor")


    val extProps = ExternalitiesActor.props(me)
    val externalitiyProcessor = _system.actorOf(extProps, "ExternalityProcessor")

    val eventProps = EventActor.props(processWaypointsJson, externalitiyProcessor)
    val eventsActor = _system.actorOf(eventProps.withRouter(RoundRobinPool(5)), name = "EventActor")

    val traceProps = TraceActor.props(processWaypointsJson, eventsActor)
    val traceProcessors = _system.actorOf(traceProps, name = "TraceActor")

    val jsons = processWaypointsJson.filterJsonFiles(trips_folder)
    jsons.map(JsonFile).foreach(traceProcessors ! _)

    import akka.pattern.gracefulStop
    import scala.concurrent.duration._
    implicit val ec: ExecutionContext = _system.dispatcher

    gracefulStop(traceProcessors, 3 minutes, logger.info("stoped trace processors"))
      .flatMap(_ => gracefulStop(eventsActor, 3 minutes, logger.info("stopped matsim mapmatching actor")))
      .flatMap(_ => gracefulStop(externalitiyProcessor, 3 minutes, logger.info("stopped externality processors")))
      .onComplete(_ => _system.terminate().onComplete(_ => logger.info("Actor system shut down")))




  }



}

class ProcessWaypointsJson(scenario: Scenario) {

  val logger = Logger.getLogger(this.getClass)
  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val gh: GHtoEvents = new MATSimMMBuilder().buildGhToEvents(scenario.getNetwork, new CH1903LV03PlustoWGS84)
  val json_matcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:**.json")

  def readJson(p: Path): List[TripRecord] = {
    logger.info(s"parsing $p")
    val json = Source.fromFile(p.toFile).getLines mkString "\n"
    parse(json).extract[List[TripRecord]]
  }


  def filterJsonFiles(triplegs_folder: Path): Stream[Path] = {
    Files.walk(triplegs_folder).iterator().asScala
      .filter(json_matcher.matches(_))
      .take(5)
      .toStream
  }

  def processJson(personDays : Stream[TripRecord]): Stream[Event] = {

    personDays
      .flatMap(tr => {
          logger.info(s"\tprocessing ${tr.user_id}, ${tr.date}")

          tr.legs
              .filterNot(tl => "activity".equals(tl.mode))
              .map { tl => tripLegToEvents(tr, tl) }
              .filterNot(_.isEmpty) //remove empty triplegs
              .flatten

          }
        )
      .sortBy(_.getTime) //sort by the first event
  }

  def tripLegToEvents (tr : TripRecord, tl : TripLeg) : Seq[Event] = {
    val matsim_mode = mapMode(tl.mode)
    val personId = Id.createPersonId(tr.user_id)
    val vehicleId = determineVehicleType(tr.user_id, tl.mode)
    val linkEvents = if (matsim_mode == TransportMode.car) {
      gh.gpsToEvents(tl.waypoints.map(_.toGPX).asJava, vehicleId).asScala.toList
    } else List.empty
    val events_full = bookendEventswithDepArr(gh, tl, personId, tl.mode, linkEvents)
    events_full
  }

  private def determineVehicleType(user_id: String, mode: String) = {
    Id.createVehicleId(user_id.toString) // + mode)
  }

  def getEventLink(event: Event): Option[Id[Link]] = {
    Some(event).map { case e : HasLinkId => e.getLinkId }
  }

  def mapMode(mode: String) : String = mode.toLowerCase match {
    case "ecar" | "car" => TransportMode.car
    case "bus" => TransportMode.pt
    case "tram" => TransportMode.pt
    case "train" => TransportMode.train
    case "walk"  => TransportMode.walk
    case "bicycle" => TransportMode.bike
    case _ => TransportMode.other


  }

  def bookendEventswithDepArr(gh : GHtoEvents, tl: TripLeg,
                              personId:Id[Person], mappedMode : String,
                              linkEvents : List[Event]) : Seq[Event] = {

    val departureLink : Id[Link] = linkEvents.headOption.flatMap(getEventLink)
      .getOrElse(gh.getNearestLinkId(tl.start_point.toGPX))
    val arrivalLink : Id[Link] = linkEvents.lastOption.flatMap(getEventLink)
      .getOrElse(gh.getNearestLinkId(tl.finish_point.toGPX))

    if (departureLink == null) {
      logger.error(s"the trip start for $tl could not be matched to the matsim network")
    }
    if (arrivalLink == null) {
      logger.error(s"the trip end for $tl could not be matched to the matsim network")
    }

    val departureEvent = new PersonDepartureEvent(tl.getStartedSeconds, personId, departureLink, mappedMode)
    val arrivalEvent = new PersonArrivalEvent(tl.getFinishedSeconds, personId, arrivalLink, mappedMode)
    departureEvent :: (linkEvents ::: List(arrivalEvent))
  }


}
