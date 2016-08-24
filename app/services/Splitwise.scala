package services

import akka.actor.Status.Failure
import configuration.Config
import model._
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.joda.time.DateTime
import play.api.libs.json._
import services.SplitwiseFormats.Expense

import scala.util.{Success, Try}

object SplitwiseFormats {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val dateReads: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ssZ")

  case class UserDetails(id: Long)

  case class Category(
                       id: Long,
                       name: String
                     )

  case class User(
                   user: UserDetails,
                   user_id: Long,
                   paid_share: Option[BigDecimal],
                   owed_share: Option[BigDecimal],
                   net_balance: BigDecimal
                  )

  case class Expense(
                      id: Long,
                      group_id: Option[Long],
                      description: String,
                      creation_method: Option[String],
                      cost: BigDecimal,
                      currency_code: String,
                      date: DateTime,
                      created_at: DateTime,
                      created_by: UserDetails,
                      updated_at: DateTime,
                      deleted_by: Option[UserDetails],
                      category: Category,
                      payment: Boolean,
                      users: List[User]
                    )

  implicit val createdByFormat: Format[UserDetails] = Json.format[UserDetails]
  implicit val categoryFormat: Format[Category] = Json.format[Category]
  implicit val usersFormat: Format[User] = Json.format[User]
  implicit val expenseFormat: Reads[Expense] = (
    (__ \ "id").read[Long] and
    (__ \ "group_id").readNullable[Long] and
    (__ \ "description").read[String] and
    (__ \ "creation_method").readNullable[String] and
    (__ \ "cost").read[BigDecimal] and
    (__ \ "currency_code").read[String] and
    (__ \ "date").read[DateTime] and
    (__ \ "created_at").read[DateTime] and
    (__ \ "created_by").read[UserDetails] and
    (__ \ "updated_at").read[DateTime] and
    (__ \ "deleted_by").readNullable[UserDetails] and
    (__ \ "category").read[Category] and
    (__ \ "payment").read[Boolean] and
    (__ \ "users").read[List[User]]
  )(Expense)

}


class Splitwise(person: String) {

  import SplitwiseFormats._

  val consumer = new CommonsHttpOAuthConsumer(Config.Splitwise.consumerKey, Config.Splitwise.consumerSecret)
  consumer.setTokenWithSecret(Config.Splitwise.People.accessToken(person), Config.Splitwise.People.accessSecret(person))

  val userId = Config.Splitwise.People.userId(person)

  def constructUrl(path: String, query: Seq[(String, String)] = Seq.empty): String = {
    s"${Config.Splitwise.baseUri}$path?${query.map(q => s"${q._1}=${q._2}").mkString("&")}"
  }

  val httpClient: CloseableHttpClient = HttpClientBuilder.create().build()

  def expenses: Either[SplitwiseError, Seq[model.Expense]] = {
    val url = constructUrl("get_expenses", Seq("limit" -> "0"))
    val request = new HttpGet(url)

    consumer.sign(request)
    val response = Try(httpClient.execute(request))

    def owedShare(e: Expense): BigDecimal = e.users.find(_.user_id == userId).flatMap(_.owed_share).getOrElse(0)

    def isValidExpense(e: Expense): Boolean = e.deleted_by.isEmpty && !e.payment && owedShare(e) > 0

    def simplifyExpense(e: Expense): model.Expense = model.Expense(
      e.id,
      e.group_id,
      e.description.trim,
      owedShare(e),
      e.currency_code,
      e.category.name,
      e.date
    )

    response match {
      case Success(r) => r.getStatusLine.getStatusCode match {
        case 200 => {
          val body = scala.io.Source.fromInputStream(r.getEntity.getContent).getLines().mkString("")
          (Json.parse(body) \ "expenses").toEither match {
            case Right(json) => json.validate[Seq[Expense]] match {
              case JsSuccess(expenses, _) => {
                Right(expenses.filter(isValidExpense).map(e => simplifyExpense(e)))
              }
              case JsError(e) => Left(SplitwiseJsonParsingError(e))
            }
            case Left(e) => Left(SplitwiseRequestTransportError(""))
          }
        }
        case sc => Left(SplitwiseRequestError(sc))
      }
      case e => Left(SplitwiseRequestTransportError(""))
    }
  }

}
