package com.github.yzgw.app

import fitbitService._
import org.scribe.builder._
import org.scribe.builder.api._
import org.scribe.model._
import org.scribe.oauth._
import net.liftweb.json._
import java.util.Calendar

object fitbitService extends OAuthService {
  final val baseUrl = "https://api.fitbit.com/1/"
  
  def getSteps(accessToken: Token, today: Calendar): Option[BigInt] = {
    val date = "%tF" format today
    // println(date)
    val req = fitbitService.get(baseUrl + "user/-/activities/date/" + date + ".json", accessToken)

    req match {
      case Some(json) =>
        val s = (json \\ "summary" \ "steps")
        // println(json \\ "summary")
        // println(json \\ "summary" \ "steps")
        s match {
          case JInt(i) => Some(i)
          case _          => None
        }
      case None => None
    }

  }
  
  def getDisplayName(accessToken: Token): Option[String] = {
    val req = fitbitService.get("https://api.fitbit.com/1/user/-/profile.json", accessToken)

    req match {    
      case Some(json) =>
        val s = (json \\ "user" \ "displayName")
        s match {
          case JString(st) => Some(st)
          case _          => None
        }
      case None => None
    }
  }
}

trait OAuthService {
  val service = new ServiceBuilder()
                              .provider(classOf[FitbitApi])
                              .apiKey("")
                              .apiSecret("")
                              // .callback("http://localhost:8080/redirect")
                              .build();

  def requestToken(): Token = service.getRequestToken

  def getAuthUrl(requestToken: Token): String = service.getAuthorizationUrl(requestToken)

  def getAccessToken(requestToken: Token, verifierCode: String): Token = {
    val verifier: Verifier = new Verifier(verifierCode);
    service.getAccessToken(requestToken, verifier);
  }

  def get(url: String, accessToken: Token): Option[JValue] = {
    val r = request(Verb.GET, url, accessToken)
    r match {
      case Some(str:String) => Some(parse(str))
      case None => None
    }
  }

  def request(verb: Verb, url: String, accessToken: Token): Option[String] = {
    val request: OAuthRequest = new OAuthRequest(verb, url)
    service.signRequest(accessToken, request)
    val response: Response = request.send
    response.getCode match {
      case 401 => None
      case _ => Some(response.getBody)
    }
  }
}
