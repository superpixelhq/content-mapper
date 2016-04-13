package com.superpixel.jdot

import com.superpixel.jdot.pathing.JPathPair
import com.superpixel.jdot.json4s.JValueAttacher

trait JDotAttacher {

  def attach(attachJson: String, attachToJson: String): String
  
  def attachList(attachJsonList: List[String], attachToJson: String): String
  
}

object JDotAttacher {
  def apply(attachmentPairs: Set[JPathPair]): JDotAttacher = JValueAttacher(attachmentPairs)
}