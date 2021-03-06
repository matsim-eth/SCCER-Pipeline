package ethz.ivt.externalities.actors

import java.nio.file.{Path, Paths}
import java.sql._
import java.time.LocalDateTime
import java.util.stream.Collectors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import ethz.ivt.externalities.counters.{ExternalityCounter, LegValues}
import ethz.ivt.externalities.data.TripRecord

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import akka.pattern.pipe

import scala.util.Success

object ExternalitiesWriterActor {
  def buildDefault(outputFolder :Path): Props = Props(new DefaultExtWriter(outputFolder))

  def buildPostgres(config: HikariConfig): Props
      = Props(new PostgresExtWriter(config))
}

final case class Externalities(tr : TripRecord, externalitiesCounter : ExternalityCounter)

sealed trait ExternalitiesWriterActor extends Actor with ActorLogging{

}

class DefaultExtWriter(outputFolder : Path) extends ExternalitiesWriterActor {
  override def receive: Receive =  {
    case e : Externalities =>   {
      log.info("writing externalities")
      val outputFile = outputFolder.resolve(e.tr.date + "_" +  e.tr.user_id + ".csv")
      e.externalitiesCounter.writeCsvFile(outputFile)
    }

  }
}

class PostgresExtWriter(config: HikariConfig) extends ExternalitiesWriterActor {
  implicit val executionContext: ExecutionContext = context.dispatcher

  val ds = new HikariDataSource(config)
  val headers = "person_id;leg_date;leg;mode_choice;variable;value".split(";")

  val datatypes : Stream[String] = Stream("varchar(255)", "date", "integer", "varchar(255)", "varchar(255)", "numeric")

  val sqlTypes = headers.zip(datatypes).toList
  val sqlIndex : Map[String, Int] = headers.zipWithIndex.toMap.mapValues(_ + 1)
  //def sqlIndex(s : String) = sqlIndexes(s.replace("(", "_").replace(")", "_").toLowerCase)

  val leg_insert_sql = s"INSERT INTO legs (person_id, leg_date, leg_mode, distance) values (?, ?, ?, ?);"

  val externalities_insert_sql = s"INSERT INTO externalities (leg_id, variable, val) values (?, ?, ?);"

  def receive : Receive = {
    case Externalities(tr, ec) => {
      log.info(s"writing ${tr.legs.size} legs to db for ${tr.user_id}")
      val future = Future {
        val con = ds.getConnection()
        try {
          val leg_pst = con.prepareStatement(leg_insert_sql,  Statement.RETURN_GENERATED_KEYS)
          val externalities_pst = con.prepareStatement(externalities_insert_sql)

          val res = ec.getPersonId2Leg().asScala.map { case (pid, legValues) => {
            val insert_date = LocalDateTime.now()

            leg_pst.setString(1, pid.toString)

            legValues.asScala.zipWithIndex.foreach { case (leg, leg_num) => {
              leg_pst.setTimestamp(2, java.sql.Timestamp.valueOf(leg.getTimestamp))
              leg_pst.setString(3, leg.getMode)
              leg_pst.setDouble(4, leg.getDistance)
              leg_pst.setDouble(4, leg.getDistance)
              val affectedRows = leg_pst.executeUpdate

              if (affectedRows == 0) throw new SQLException("Creating leg failed, no rows affected.")
              val generatedKeys = leg_pst.getGeneratedKeys
              if (!generatedKeys.next()) throw new SQLException("Creating leg failed, no ID obtained.")
              val leg_id = generatedKeys.getInt(1)

              leg.keys().forEach(k => {
                val v = leg.get(k)
                externalities_pst.setInt(1, leg_id)
                externalities_pst.setString(2, k)
                externalities_pst.setDouble(3, v)
                externalities_pst.addBatch()

              })
            }}
            (pid, externalities_pst.executeBatch())
          }
          }.toMap
          log.info (s"${res.values.map(_.length).sum} added to DB")


          res
        }
        finally {
          con.close()
        }
      }
      future pipeTo sender()
    }
  }
}


