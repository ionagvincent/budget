package controllers

import javax.inject._

import configuration.Config
import model.{SplitwiseJsonParsingError, SplitwiseRequestError, SplitwiseRequestTransportError}
import org.joda.time.LocalDate
import play.api.libs.json.Json
import play.api.mvc._
import services.Splitwise
import services.Fixer

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() extends Controller {

  import model.Expense.expensesFormat

  def index(person: String) = Action {

    val defaultCurrency = Config.Splitwise.People.currency(person)

    val s = new Splitwise(person)

    Fixer.conversions(new LocalDate("2016-08-22")) match {
      case Right(conversion) => {
        println(conversion.convert("GBP", "SEK", 10))
      }
      case _ =>
    }

    s.expenses match {
      case Right(expenses) => {
        val expensesToConvert = expenses.filter(_.currencyCode != defaultCurrency).groupBy(e => e.currencyCode -> e.date.toLocalDate)
        println(expensesToConvert)
        println(expensesToConvert.keySet)
        Ok(Json.toJson(expenses))
      }
      case Left(error) => error match {
        case _ => InternalServerError("Something went wrong")
      }
    }
  }

}
