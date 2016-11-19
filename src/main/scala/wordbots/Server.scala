package wordbots

import com.workday.montague.semantics.Form
import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder

object Server {
  object InputParamMatcher extends QueryParamDecoderMatcher[String]("input")
  object FormatParamMatcher extends QueryParamDecoderMatcher[String]("format")

  val service = HttpService {
    case request @ GET -> Root / "parse" :? InputParamMatcher(input) +& FormatParamMatcher(format) =>
      val result = Parser.parse(input)

      format match {
        case "js" =>
         result.bestParse.map(_.semantic) match {
            case Some(Form(v: AstNode)) => Ok(CodeGenerator.generateJS(v))
            case _ => InternalServerError("Parse failed")
          }

        case "svg" =>
          result.bestParse
            .map(parse => Ok(parse.toSvg))
            .getOrElse(InternalServerError("Parse failed"))

        case _ => BadRequest("Invalid format")
      }
  }

  val host = "0.0.0.0"
  val port = (Option(System.getenv("PORT")) orElse
    Option(System.getenv("HTTP_PORT")))
    .map(_.toInt)
    .getOrElse(8080)

  def main(args: Array[String]): Unit = {
    println(s"Starting server on '$host:$port' ...")

    BlazeBuilder
      .bindHttp(port, host)
      .mountService(service)
      .run
      .awaitShutdown
  }
}