package ethz.ivt.externalities.actors

import java.nio.file.{Path, Paths}
import java.sql.{Connection, Date, DriverManager, SQLException}
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
final case class CREATE_DB(replace : Boolean)

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

  val create_sql =
      s"CREATE TABLE externalities(\n id SERIAL,\n" +
      sqlTypes.map{ case (k, t) => s"\t$k $t"}.mkString(",\n") +
      ");"

  val insert_sql = s"INSERT INTO externalities " +
    s"( ${headers.mkString(",")}) " +
    s"VALUES ( ${headers.map(_=> "?").mkString(",")} )"

  log.info(insert_sql)


  def receive : Receive = {
    case CREATE_DB(replace) => {
      log.info("checking for and creating DB")
      val con = ds.getConnection()

      //check if table exists
      val future : Future[Boolean] = Future {
        if (replace) {
          con.createStatement().execute("DROP TABLE IF EXISTS externalities; \n")
        } else {false}
      } map {
        _ => con.createStatement().execute(create_sql)
      } pipeTo sender()
    }
    case Externalities(tr, ec) => {
      log.info(s"writing ${tr.legs.size} legs to db for ${tr.user_id}")
      val future = Future {
        val con = ds.getConnection()
        try {
          val pst = con.prepareStatement(insert_sql)
          val res = ec.getPersonId2Leg().asScala.map { case (pid, legValues) => {

            log.info(legValues.get(0).keys().collect(Collectors.joining(";")))

            pst.setString(sqlIndex("person_id"), pid.toString)
            pst.setDate(sqlIndex("leg_date"), java.sql.Date.valueOf(tr.date))

            legValues.asScala.zipWithIndex.foreach { case (leg, leg_num) => {
              pst.setInt(sqlIndex("leg"), leg_num)
              pst.setString(sqlIndex("mode_choice"), leg.getMode)

              leg.keys().forEach(k => {
                val v = leg.get(k)
                pst.setString(sqlIndex("variable"), k)
                pst.setDouble(sqlIndex("value"), v)
                pst.addBatch()

              })
            }}
            (pid, pst.executeBatch())
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


