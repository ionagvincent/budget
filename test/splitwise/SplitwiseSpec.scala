package splitwise

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsResult, JsSuccess}
import play.api.libs.json.Json.parse
import services.SplitwiseFormats.Expenses

import scala.io.Source

class SplitwiseSpec extends FlatSpec with Matchers {

  it should "deserialise JSON" in {
     val json = parse(
        Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("splitwise/expenses.json")).mkString
      )
      json.validate[Expenses] match {
        case JsSuccess(expenses, _) => {
          val firstExpense = expenses.expenses.find(_.id == 114828821).get
          firstExpense.cost shouldBe 10.44
          firstExpense.currency_code shouldBe "GBP"
          firstExpense.creation_method shouldBe Some("split")
          firstExpense.description shouldBe "Book"
          val secondExpense = expenses.expenses.find(_.id == 117348454).get
          secondExpense.cost shouldBe 2.1
        }
        case _ => fail("Could not parse JSON for expenses")
      }
  }

}
