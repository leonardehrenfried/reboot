import com.ning.http.client.RequestBuilder

package object dispatch {
  /** Type alias for Response, avoid need to import */
  type Res = com.ning.http.client.Response
  /** Type alias for URI, avoid need to import */
  type Uri = java.net.URI

  @deprecated("Use `RequestBuilder.underlying` to preserve referential transparency",
    since="0.11.0")
  implicit def implyReq(builder: RequestBuilder) = Req(_ => builder)

  implicit def implyRequestHandlerTuple(builder: Req) =
    new RequestHandlerTupleBuilder(builder)


  implicit def implyRunnable[U](f: () => U) = new java.lang.Runnable {
    def run() = { f(); () }
  }

  implicit def enrichFuture[T](future: Future[T]) =
    new EnrichedFuture(future)

  /** Type alias to scala.concurrent.Future so you don't have to import */
  type Future[+T] = scala.concurrent.Future[T]

  val Future = scala.concurrent.Future
}
