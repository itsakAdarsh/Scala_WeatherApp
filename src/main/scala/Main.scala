import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.circe._
import io.circe.parser._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val apiKey = "API_Key"
  val city = "London"
  val weatherApiUrl = s"http://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey"

  def fetchWeatherData(): Future[String] = {
    Http().singleRequest(HttpRequest(uri = weatherApiUrl)).flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        Unmarshal(entity).to[String]
      case HttpResponse(code, _, entity, _) =>
        Unmarshal(entity).to[String].flatMap { entityString =>
          Future.failed(new RuntimeException(s"Request failed with status code $code and entity $entityString"))
        }
    }
  }

  fetchWeatherData().onComplete {
    case Success(jsonResponse) =>
      parse(jsonResponse) match {
        case Left(failure) => println(s"Parsing failed: $failure")
        case Right(json) =>
          val weatherDescription = json.hcursor.downField("weather").downArray.downField("description").as[String]
          val temperature = json.hcursor.downField("main").downField("temp").as[Double]
          (weatherDescription, temperature) match {
            case (Right(desc), Right(temp)) =>
              println(s"Weather in $city: $desc, Temperature: ${temp - 273.15}°C")
            case _ =>
              println("Error extracting weather information")
          }
      }
    case Failure(exception) =>
      println(s"Failed to fetch weather data: $exception")
  }
}