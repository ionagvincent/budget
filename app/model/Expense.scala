package model


import org.joda.time.DateTime

case class Expense(
  id: Long,
  groupId: Option[Long],
  description: String,
  cost: BigDecimal,
  currencyCode: String,
  category: String,
  date: DateTime
)

object Expense {

  import play.api.libs.json._

  implicit val dateWrites: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm")

  implicit val expensesFormat: Format[Expense] = Format(Json.reads[Expense], Json.writes[Expense])

}