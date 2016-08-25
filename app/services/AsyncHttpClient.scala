package services

import model.{Error, HttpError, JsonParsingError}
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.{CloseableHttpAsyncClient, HttpAsyncClients}
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}

import scala.concurrent.{ExecutionContext, Future, Promise}

class ScalaFutureCallbackImpl[T] extends FutureCallback[T] {
  private val promise: Promise[T] = Promise()

  def cancelled(): Unit =
    promise.failure(new RuntimeException("cancelled!"))

  def completed(result: T): Unit = promise.success(result)

  def failed(ex: Exception): Unit = promise.failure(ex)

  def asFuture: Future[T] = promise.future
}

class AsyncHttpClient {

  val httpClient: CloseableHttpAsyncClient = HttpAsyncClients.createDefault()
  val callback = new ScalaFutureCallbackImpl[HttpResponse]
  httpClient.start()

  def execute(request: HttpRequestBase)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    httpClient.execute(request, callback)
    callback.asFuture
  }

  def json[T](request: HttpRequestBase)(implicit readerT: Reads[T], ec: ExecutionContext) : Future[Either[Error, T]] = {
    execute(request).map { response =>
      response.getStatusLine.getStatusCode match {
        case 200 => {
          val body = scala.io.Source.fromInputStream(response.getEntity.getContent).getLines().mkString("")
          httpClient.close()
          Json.parse(body).validate[T] match {
            case JsSuccess(result, _) => Right(result)
            case JsError(e) => Left(JsonParsingError(e))
          }
        }
        case httpStatusCode: Int => Left(HttpError())
      }
    }
  }

}
