package services

import configuration.Config
import model._
import org.apache.http.client.methods.HttpGet
import org.joda.time.{DateTime, LocalDate}

import scala.concurrent.{ExecutionContext, Future}

object FixerFormats {

  import play.api.libs.json._

  implicit val dateReads: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd")

  case class Conversions(
   base: String,
   date: LocalDate,
   rates: Map[String, BigDecimal]
  ) {
    def convert(fromCurrencyCode: String, toCurrencyCode: String, amount: BigDecimal): Option[BigDecimal] = {
      if (fromCurrencyCode == toCurrencyCode) {
        Some(amount)
      } else {
        for {
          fromRate <- rates.get(fromCurrencyCode)
          toRate <- rates.get(toCurrencyCode)
        } yield amount * toRate/fromRate
      }
    }
  }

  implicit val conversionFormat: Format[Conversions] = Json.format[Conversions]

}

object Fixer {

  import FixerFormats._

  val httpClient = new AsyncHttpClient

  def constructUrl(date: LocalDate): String = {
    val dateString = date.toString("YYYY-MM-DD")
    s"${Config.Fixer.baseUri}$dateString"
  }

  def conversions(date: LocalDate)(implicit ec: ExecutionContext): Future[Either[Error, Conversions]] = {
    val url = constructUrl(date)
    val request = new HttpGet(url)
    httpClient.json[Conversions](request)
  }

}
