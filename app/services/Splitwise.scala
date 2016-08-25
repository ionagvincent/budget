package services

import configuration.Config
import model._
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.http.client.methods.HttpGet
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

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
  case class Expenses(
                     expenses: Seq[Expense]
                    )

  implicit val createdByFormat: Format[SplitwiseFormats.UserDetails] = Json.format[UserDetails]
  implicit val categoryFormat: Format[SplitwiseFormats.Category] = Json.format[Category]
  implicit val usersFormat: Format[SplitwiseFormats.User] = Json.format[User]
  implicit val expenseFormat: Reads[SplitwiseFormats.Expense] = (
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
  implicit def readExpenses: Reads[SplitwiseFormats.Expenses] = new Reads[SplitwiseFormats.Expenses] {
    def reads(in: JsValue) = {
      (in \ "expenses").validate[Seq[SplitwiseFormats.Expense]].map(expenses => Expenses(expenses))
    }
  }

}

class Splitwise(person: String) {

  import SplitwiseFormats._

  val httpClient = new AsyncHttpClient

  val consumer = new CommonsHttpOAuthConsumer(Config.Splitwise.consumerKey, Config.Splitwise.consumerSecret)
  consumer.setTokenWithSecret(Config.Splitwise.People.accessToken(person), Config.Splitwise.People.accessSecret(person))

  val userId = Config.Splitwise.People.userId(person)

  def constructUrl(path: String, query: Seq[(String, String)] = Seq.empty): String = {
    s"${Config.Splitwise.baseUri}$path?${query.map(q => s"${q._1}=${q._2}").mkString("&")}"
  }

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

  def expenses(implicit ec: ExecutionContext): Future[Either[Error, Seq[model.Expense]]] = {
    val url = constructUrl("get_expenses", Seq("limit" -> "0"))
    val request = new HttpGet(url)
    consumer.sign(request)

    httpClient.json[Expenses](request).map {
      case Right(e) => Right(e.expenses.filter(isValidExpense).map(e => simplifyExpense(e)))
      case Left(error) => Left(error)
    }
  }

}
