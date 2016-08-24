package model

import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath

sealed trait SplitwiseError

case class SplitwiseRequestTransportError(error: String) extends SplitwiseError
case class SplitwiseRequestError(statusCode: Int) extends SplitwiseError
case class SplitwiseJsonParsingError(errors: Seq[(JsPath, Seq[ValidationError])]) extends SplitwiseError

sealed trait FixerError
case class FixerGenericError() extends FixerError