package model

import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath

sealed trait Error
case class JsonParsingError(errors: Seq[(JsPath, Seq[ValidationError])]) extends Error
case class HttpError() extends Error