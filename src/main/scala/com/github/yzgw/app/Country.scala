package com.github.yzgw.app

import scala.xml._

case class Country(jp_name: String, iso_name: String, three_digit_id: String, longitude: Double, latitude: Double, distance: Int)

object Country{
  def apply(node: Node) = {
    new Country( (node \ "jp_name").text, (node \ "iso_name").text, (node \ "three_digit_id").text, (node \ "longitude").text.toDouble, (node \ "latitude").text.toDouble, (node \ "distance").text.toInt
    )
  }  
}
