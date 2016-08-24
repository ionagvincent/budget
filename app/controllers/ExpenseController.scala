package controllers

import javax.inject._

import configuration.Config
import play.api.libs.json.Json
import play.api.mvc._
import services.Splitwise

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ExpenseController @Inject() extends Controller {

  import model.Expense.expensesFormat

  import scala.concurrent.ExecutionContext.Implicits.global

  def index(person: String) = Action.async {

    val defaultCurrency = Config.Splitwise.People.currency(person)

    val splitwise = new Splitwise(person)
//
//    Fixer.conversions(new LocalDate("2016-08-22")).map {
//      case Right(conversion) => {
//        println(conversion.convert("GBP", "SEK", 10))
//      }
//      case _ =>
//    }

    splitwise.expenses.map {
      case Right(expenses) => Ok(Json.toJson(expenses))
      case Left(error) => InternalServerError(error.toString)
    }

//    s.expenses match {
//      case Right(expenses) => {
//        val expensesToConvert = expenses.filter(_.currencyCode != defaultCurrency).groupBy(e => e.currencyCode -> e.date.toLocalDate)
//        println(expensesToConvert)
//        println(expensesToConvert.keySet)
//        Ok(Json.toJson(expenses))
//      }
//      case Left(error) => error match {
//        case _ => InternalServerError("Something went wrong")
//      }
//    }
  }

}
