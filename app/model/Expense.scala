package model

import org.joda.time.{DateTime}

case class Expense(
  id: Long,
  groupId: Option[Long],
  description: String,
  cost: BigDecimal,
  currencyCode: String,
  category: String,
  date: DateTime
)