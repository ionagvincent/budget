package services

import akka.actor.Status.Failure
import configuration.Config
import model._
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json._
import services.SplitwiseFormats.{Expense, UserDetails}

import scala.util.{Success, Try}

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

  def constructUrl(date: LocalDate): String = {
    val dateString = date.toString("YYYY-MM-DD")
    s"${Config.Fixer.baseUri}${dateString}"
  }
  val httpClient: CloseableHttpClient = HttpClientBuilder.create().build()

  def conversions(date: LocalDate): Either[FixerError, Conversions] = {
    val url = constructUrl(date)

    val request = new HttpGet(url)
    val response = Try(httpClient.execute(request))

    response match {
      case Success(r) => r.getStatusLine.getStatusCode match {
        case 200 => {
          val body = scala.io.Source.fromInputStream(r.getEntity.getContent).getLines().mkString("")
          Json.parse(body).validate[Conversions] match {
            case JsSuccess(conversion, _) => {
              println(conversion)
              Right(conversion)
            }
            case JsError(e) => Left(FixerGenericError())
          }
        }
      }
    }
  }

}
