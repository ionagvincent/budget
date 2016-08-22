package controllers

import javax.inject._

import model.{SplitwiseJsonParsingError, SplitwiseRequestError, SplitwiseRequestTransportError}
import play.api.mvc._
import services.Splitwise

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() extends Controller {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `route
    * s` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {

    val s = new Splitwise("niklas")

    s.expenses match {
      case Right(expenses) => {
        println(s"Got ${expenses.length} expenses")
        expenses.foreach(println)
      }
      case Left(error) => error match {
        case e: SplitwiseRequestError => println(e)
        case e: SplitwiseRequestTransportError => println(e)
        case e: SplitwiseJsonParsingError => println(e)
      }
    }

    Ok("Hello")
  }

}
