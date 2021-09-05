package ethz.ivt.externalities.actors

import java.nio.file.{Path, Paths}
import java.sql._
import java.time.LocalDateTime
import java.util.stream.Collectors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import ethz.ivt.externalities.counters.{AutobahnSplitCounter, ExternalityCounter, LegValues}
import ethz.ivt.externalities.data.TripRecord

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.pipe

import scala.util.Success

object ExternalitiesWriterActor {
  def buildDefault(outputFolder :Path): Props = Props(new DefaultExtWriter(outputFolder))

  def buildMobis(config: HikariConfig): Props
    = Props(new MobisExtWriter(config))

  def buildDummy(): Props = Props(new DummyExtWriter())
}

final case class Externalities(tr : TripRecord, externalitiesCounter : ExternalityCounter)

sealed trait ExternalitiesWriterActor extends Actor with ActorLogging  {

}

class DefaultExtWriter(outputFolder : Path) extends ExternalitiesWriterActor {
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive =  {
    case e : Externalities =>   {
      val future = Future {
          val trip_id = e.tr.legs.headOption.map(l => l.leg_id).getOrElse("_None")
          val outputFile = outputFolder.resolve(e.tr.date + "_" + e.tr.user_id + "_" + trip_id + ".csv")
          e.externalitiesCounter.writeCsvFile(outputFile)
          Future.successful(0)
        }
      future pipeTo sender()
    }
  }
}

class DummyExtWriter extends ExternalitiesWriterActor  with ActorLogging {
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive =  {
    case e : Externalities =>   {
      Future.successful(0) pipeTo sender()
    }
  }
}


class MobisExtWriter(config: HikariConfig) extends ExternalitiesWriterActor {
  implicit val executionContext: ExecutionContext = context.dispatcher

  var ds : HikariDataSource = _

  val externalities_insert_sql = s"INSERT INTO externalities_list_covid (leg_id, key, value) values (?, ?, ?);"
  val trip_processed_at_sql = s"UPDATE motion_tag_trips set externalities_for_update = ? where mt_trip_id = ?;"

  override def preStart() = {
    super.preStart
    ds = new HikariDataSource(config)
  }

  def receive : Receive = {
    case Externalities(tr, ec) => {
      log.info(s"writing ${tr.legs.size} legs to mobis db for ${tr.user_id}")
      val future = Future {
        val con = ds.getConnection()
        try {
          val externalities_pst = con.prepareStatement(externalities_insert_sql)
          val trip_processed_at_pst = con.prepareStatement(trip_processed_at_sql)

          val res = ec.getPersonId2Leg().asScala.map { case (pid, legValues) =>
            val insert_date = LocalDateTime.now()

            legValues.asScala.zipWithIndex.foreach { case (leg, leg_num) =>
              val leg_id = leg.getTriplegId
              leg.keys().asScala.foreach { k =>
                val v = leg.get(k)
                if (k != null) {
                  externalities_pst.setString(1, leg_id)
                  externalities_pst.setString(2, k)
                  externalities_pst.setDouble(3, v)
                  externalities_pst.addBatch()
                } else {
                  log.warning(s"Error key NULL on $leg_id")
                }
              }
              trip_processed_at_pst.setTimestamp(1, Timestamp.valueOf(leg.getUpdatedAt()))
              trip_processed_at_pst.setString(2, leg_id)
              trip_processed_at_pst.addBatch()
            }
            con.setAutoCommit(false)
            val ret_vals = (pid, externalities_pst.executeBatch())
            (pid, trip_processed_at_pst.executeBatch())
            con.commit()
            ret_vals
          }.toMap
          log.info (s"${res.values.map(_.length).sum} added to DB")


          res
        }
        catch {
          case ex : Exception => {
            log.error(ex, "Error writing externalities 22 ")
            Future.failed(ex)
          }
        }
        finally {
          con.close()
        }
      }
      future pipeTo sender()
    }
  }
}


