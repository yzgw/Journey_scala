package com.github.yzgw.app
import scala.annotation._
import org.scalatra._
import scalate.ScalateSupport
import fitbitService._
import com.redis._
import java.util.Calendar
import org.scribe.model.Token

import scala.xml._

case class Journey() extends JourneyStack {

  val redisClient = new RedisClient("localhost", 6379)

  lazy val countries = {
    val context = this.getServletContext();
    val xml = XML.loadFile( context.getRealPath("WEB-INF/countries.xml") )
    (xml \ "country").map(Country(_)).toArray
  }
  
  get("/") {
    contentType="text/html"

    val token = session.get("accessToken")
    token match {
      case None => jade("/index", "title" -> "Journey - 世界を歩く。")
      case Some(v:Token) =>
        val today = Calendar.getInstance()
        val steps = fitbitService.getSteps(v, today) match {
          case Some(s: BigInt) => s.toInt
          case _ => 0
        }

        val name = session("displayName").toString
        val (visited, unvisited) = visitedCountriesOf(name)
        
        jade("/list", "title" -> "Journey - 世界を歩く。", "steps" -> steps, "name" -> session("displayName"), 
          "visited" -> visited, "unvisited" -> unvisited, "accessAvailable" -> true) //accessAvailable(name)
      case Some(any) => jade("/index", "title" -> "Journey - 世界を歩く。")
    }
  }
  
  get("/country/:country") {
    contentType="text/html"
    val country = params("country")

    if( session.contains("displayName") ){      
      val name = session("displayName").toString
      val now = System.currentTimeMillis()
      val c = countries.find(_.three_digit_id == country).get
      val (visited, unvisited) = visitedCountriesOf(name)

      if( visited.exists(_.three_digit_id == country) ){
        jade("/country", "title" -> c.jp_name, "country" -> c)
      } else if(visited.contains(country)){
        println(visited.toList)
        jade("/country", "title" -> c.jp_name, "country" -> c)
      }else{
        println(visited.toList)
        if(!accessAvailable(name))
          redirect("/")

        redisClient.sadd(name + ":visitedCountries", country)
        redisClient.set(name + ":lastVisited", now)

        jade("/country", "title" -> c.jp_name, "country" -> c)
      }

    }else{
      redirect("/")
    }
  }
  
  get("/logout") {
    session.clear
    redirect("/")
  }  

  get("/auth") {
    val token = requestToken()
    session("requestToken") = token
    val url = getAuthUrl(token)
    redirect(url)
  }
  
  get("/redirect") {
    val requestToken: Token = new Token(params("oauth_token"), session("requestToken").asInstanceOf[Token].getSecret)
    val accessToken: Token = getAccessToken(requestToken, params("oauth_verifier"))
    session("accessToken") = accessToken

    val displayName = getDisplayName(accessToken) match {
      case Some(name: String) => session("displayName") = name
      case _ => 
    }

    redirect("/")
  }
  
  def visitedCountriesOf(name: String): (Array[Country], Array[Country]) = {
    val visitedCountries = redisClient.smembers(name + ":visitedCountries")
    visitedCountries match {
      case Some(visited) =>
        countries.partition( c => visited.exists(c.three_digit_id == _.get) )
      case None => (Array(), countries)
    }
  }

  def accessAvailable(name: Any): Boolean = {
    val now = System.currentTimeMillis()

    redisClient.get(name + "lastVisited") match {
      case Some(thatTime) =>
        val diff = now - thatTime.toDouble
        println("now " + now + " then " + thatTime + " = " + diff)
        if(diff < 24 * 60 * 60 * 1000)
          false
        else
          true
      case None => true
    }
  }

}

