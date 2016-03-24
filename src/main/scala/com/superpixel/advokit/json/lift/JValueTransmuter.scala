package com.superpixel.advokit.json.lift

import com.superpixel.advokit.json.pathing._
import org.json4s._
import org.json4s.native.JsonMethods._
import java.text.NumberFormat
import com.github.nscala_time.time.Imports._
import org.ocpsoft.prettytime.PrettyTime
import java.util.Locale
import java.util.Currency

object JValueTransmuter {
  
  @throws(classOf[JsonTransmutingException])
  def transmute(value: JValue, transmuteType: String, argument: Option[String]): JValue = transmuteType match {
    case "b" => toBoolean(value, argument)
    case "n" => toNumber(value, argument)
    case "s" => toString(value, argument) //toString + operations e.g. .1:u would take first char and uppercase it: simpson -> S
    case "f" => toFormattedFloat(value, argument) //numberFormater, follows printf rules for 'f'
    case "i" => toInt(value, argument) //intFormater, follows printf rules for 'd'
    case "d" => toInt(value, argument) //intFormater, follows printf rules for 'd'
    case "%" => toPercentage(value, argument) //converts to float, then to percentage
    case "date" => toDateFormat(value, argument) //date formatter, takes dateString or timestamp and toString based on argument
    case "ord" => toOrdinal(value, argument) //ordinal formatter 1 -> 1st, 4 - 4th
    case "cur" => toCurrency(value, argument)
    case _ => JString(value.toString)
  }
  
  def stringIndex(s: String, i: Int): Option[Char] = {
      val sLen = s.length
      if (i >= sLen || i < -sLen) None 
      else if (i < 0) Some(s(sLen + i))
      else Some(s(i))
    }
  
  
  val localeTagRegex = """([a-z]{2}-[A-Z]{2})""".r
  val iso4217 = """([A-Z]{3})""".r
  val symbol = """([£$€¥])""".r
  private def toCurrency(value: JValue, argument: Option[String]): JValue = {
    def toCurrencyString(db: Double, tag: Option[String] = None, inSubunits: Boolean = false, whole: Boolean = false): String = {
      tag match {
        case Some(localeTagRegex(localeTag)) => {
          val locale = Locale.forLanguageTag(localeTag)
          val nf = NumberFormat.getCurrencyInstance(locale);
          if (whole) nf.setMaximumFractionDigits(0)
          (inSubunits, nf.getCurrency.getDefaultFractionDigits) match {
            case (true, e) if e > 0 => {
              val ratio = Math.pow(10, -e)
              nf.format(db*ratio)
            }
            case _ => nf.format(db)
          }
        }
        case Some(iso4217(currencyCode)) => {
          val cur = Currency.getInstance(currencyCode);
          val nf = NumberFormat.getCurrencyInstance
          if (whole) nf.setMaximumFractionDigits(0)
          nf.setCurrency(cur)
          (inSubunits, cur.getDefaultFractionDigits) match {
            case (true, e) if e > 0 => {
              val ratio = Math.pow(10, -e)
              nf.format(db*ratio)
            }
            case _ => nf.format(db)
          }
        }
        case Some(symbol(sym)) => {
          val am = if (inSubunits) db*0.01d else db
          if (whole)
            f"$sym$am%,.0f"
          else
            f"$sym$am%,.2f"
        }
        case _ => {
          val nf = NumberFormat.getCurrencyInstance
          val am = if (inSubunits) db*0.01d else db
          if (whole) nf.setMaximumFractionDigits(0)
          nf.format(am)
        }
      }
    }
    
    val dOpt: Option[Double] = value match {
      case JBool(bool) => Some(if (bool) 1d else 0d)
      case JInt(int) => Some(int.toDouble)
      case JDouble(db) => Some(db.toDouble)
      case JDecimal(bd) => Some(bd.toDouble)
      case JLong(l) => Some(l.toDouble)
      case _ => None
    }
    dOpt match {
      case None => toFormattedFloat(toNumber(value, None), argument)
      case Some(d: Double) => argument match {
        case None | Some("") => 
          JString(toCurrencyString(d))
        case Some(s) => (stringIndex(s, 0), stringIndex(s, 1)) match {
          case (Some('_'), Some('0')) | (Some('0'), Some('_')) =>
            JString(toCurrencyString(d, Some(s.drop(2)), true, true))
          case (Some('_'), _) =>
            JString(toCurrencyString(d, Some(s.drop(1)), true, false))
          case (Some('0'), _) =>
            JString(toCurrencyString(d, Some(s.drop(1)), false, true))
          case _ =>
            JString(toCurrencyString(d, Some(s)))
        }
      }
    }
  }
  
  val ordinalDayRegex = """(.*)do(.*)""".r
  private def toDateFormat(value: JValue, argument: Option[String]): JValue = {
    val dateOpt: Option[DateTime] = value match {
      case JBool(bool) => Some(DateTime.now)
      case JInt(int) => Some(new DateTime(int.toLong*1000))
      case JDouble(db) => Some(new DateTime(db.toLong*1000))
      case JDecimal(bd) => Some(new DateTime(bd.toLong*1000))
      case JLong(l) => Some(new DateTime(l*1000))
      case JString(now) if now.toLowerCase() == "now" => Some(DateTime.now)
      case JString(str) => Some(DateTime.parse(str))
      case _ => None
    }
    dateOpt match {
      case None => throw new JsonTransmutingException("Cannot convert json value to date.", value)
      case Some(d: DateTime) => argument match {
        case None => JString(d.toString)
        case Some("pretty") => {
          val pt = new PrettyTime()
          JString(pt.format(d.toDate()))
        }
        case Some(ordinalDayRegex(left, right)) => {
          val dayOfMonth = d.getDayOfMonth
          val leftStr = if (left == "") "" else d.toString(left)
          val ord = dayOfMonth.toString + ordSuffix(dayOfMonth % 10)
          val rightStr = if (right == "") "" else d.toString(right)
          JString(leftStr + ord + rightStr)
        }
        case Some(arg: String) => JString(d.toString(arg))
      }
    }
    
  }
  
  val ordSuffix = Array("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th");
  val ordFullString = Array("Zeroth", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth", "Eleventh", "Twelth")
  private def toOrdinal(value: JValue, argument: Option[String]): JValue = {
    val iOpt: Option[Int] = value match {
      case JBool(bool) => Some(if (bool) 1 else 0)
      case JInt(int) => Some(int.toInt)
      case JDouble(db) => Some(db.toInt)
      case JDecimal(bd) => Some(bd.toInt)
      case JLong(l) => Some(l.toInt)
      case _ => None
    }
    iOpt.map { _.abs } match {
      case None => toOrdinal(toNumber(value, None), argument)
      case Some(i: Int) => argument.map{ _.toLowerCase } match {
        case None | Some("suffix") => JString(i.toString + ordSuffix(i % 10)) 
        case Some("full") if (i >= 0 && i < ordFullString.length) =>
          JString(ordFullString(i))
        case _ => JString(i.toString + ordSuffix(i % 10))
      }
    }
  }
  
  
  val numericRegex = """(\d+)""".r
  val opNumericRegex = """\!(\d+)""".r
  private def toPercentage(value: JValue, argument: Option[String]): JValue = {
    val fOpt: Option[Double] = value match {
      case JBool(bool) => Some(if (bool) 1d else 0d)
      case JInt(int) => Some(int.toDouble)
      case JDouble(db) => Some(db)
      case JDecimal(bd) => Some(bd.toDouble)
      case JLong(l) => Some(l.toDouble)
      case _ => None
    }
    val perNF = NumberFormat.getPercentInstance()
    perNF.setMaximumFractionDigits(2)
    fOpt match {
      case None => toPercentage(toNumber(value, None), argument)
      case Some(d: Double) => argument.map { _.toLowerCase() } match {
        case None => JString(perNF.format(d))
        case Some("!") | Some("c") => JString(perNF.format(1.0d-d))
        case Some(numericRegex(of)) => try {
          JString(perNF.format(d/of.toDouble))
        } catch {
          case e: Throwable => throw new JsonTransmutingException(s"Cannot convert $of to a double for percentage format: ${e.getMessage}", value)
        }
        case Some(opNumericRegex(of)) => try {
          JString(perNF.format(1.0d-(d/of.toDouble)))
        } catch {
          case e: Throwable => throw new JsonTransmutingException(s"Cannot convert $of to a double for percentage format: ${e.getMessage}", value)
        }
        case _ => JString(perNF.format(d))
      }
    }
  }
  
  private def toInt(value: JValue, argument: Option[String]): JValue = {
    val iOpt: Option[Int] = value match {
      case JBool(bool) => Some(if (bool) 1 else 0)
      case JInt(int) => Some(int.toInt)
      case JDouble(db) => Some(db.toInt)
      case JDecimal(bd) => Some(bd.toInt)
      case JLong(l) => Some(l.toInt)
      case _ => None
    }
    iOpt match {
      case None => toInt(toNumber(value, None), argument)
      case Some(i: Int) => try {
          val formatString: String = s"%${argument.getOrElse("")}d"
          JString(formatString.format(i))
        } catch {
          case e : Throwable => throw new JsonTransmutingException(s"Did not understand formatted integer transmutation arguments - $argument - Nested: ${e.getMessage}", value)
        }
    }
  }
  
  private def toFormattedFloat(value: JValue, argument: Option[String]): JValue = {
    val fOpt: Option[Float] = value match {
      case JBool(bool) => Some(if (bool) 1f else 0f)
      case JInt(int) => Some(int.toFloat)
      case JDouble(db) => Some(db.toFloat)
      case JDecimal(bd) => Some(bd.toFloat)
      case JLong(l) => Some(l.toFloat)
      case _ => None
    }
    fOpt match {
      case None => toFormattedFloat(toNumber(value, None), argument)
      case Some(fl: Float) => try {
          val formatString: String = s"%${argument.getOrElse("")}f"
          JString(formatString.format(fl))
        } catch {
          case e : Throwable => throw new JsonTransmutingException(s"Did not understand formatted float transmutation arguments - $argument - Nested: ${e.getMessage}", value)
        }
    }
    
  }
  
  val substringRegex = """(\d*)\.(\d*)""".r
  private def toString(value: JValue, argument: Option[String]): JString = {
    def argApply(args: Seq[String], string: String): String = args match {
      case Nil => string
      case "u" +: tl => argApply(tl, string.toUpperCase())
      case "l" +: tl => argApply(tl, string.toLowerCase())
      case "1u" +: tl => argApply(tl, string.capitalize)
      case substringRegex(left, right) +: tl => {
        (left, right) match {
          case ("", d) => argApply(tl, string.take(d.toInt))
          case (d, "") => argApply(tl, string.drop(d.toInt))
          case (d1, d2) => argApply(tl, string.substring(d1.toInt, d2.toInt))
          case _ => argApply(tl, string)
        }
      }
      case arg +: tl => try {
        argApply(tl, String.format("%" + arg + "s", string))
      } catch {
        case e : Throwable => argApply(tl, string)
      }
    }
    
    val str: String = value match {
      case JNothing => ""
      case JString(str) => str
      case JBool(bool) => bool.toString()
      case JInt(int) => int.toString()
      case JDouble(db) => db.toString()
      case JDecimal(bd) => bd.toString()
      case JLong(l) => l.toString()
      case jv => compact(render(jv))
    }
    
    JString(argument.map { s => s.toLowerCase().split("""\:""") } match {
      case None => str
      case Some(arr) => {
        argApply(arr, str)
      }
    })
  }
  
  private def toNumber(value: JValue, argument: Option[String]): JValue = {
    val argTup = argument.map(str => {
      val arr = str.split(":") 
      if (arr.length == 1) {
        (arr(0), arr(0))
      } else {
        (arr(0), arr(1))
      }
    })
    val num: JValue = value match {
      case JBool(bool) => if(bool) JInt(1) else JInt(0)
      case ji: JInt => ji
      case jdb: JDouble => jdb
      case jdc: JDecimal => jdc
      case jl: JLong => jl
      case _:JObject | _:JArray | JNothing | JNull => 
        throw new JsonTransmutingException("Cannot convert " + value.getClass.getName + " to a json number", value)
      case JString(str: String) => argTup match {
        case Some((base, _)) if base == "2" || base == "8" || base == "16" => string2Number(str, Some(base.toInt))
        case _ => toNumber(string2Number(str), argument)
      }
    }
    
    argTup match {
      case None => num
      case Some((_,"l")) => num match {
        case jl: JLong => jl
        case JInt(i) => JLong(i.toLong)
        case JDouble(db) => JLong(db.toLong)
        case JDecimal(dc) => JLong(dc.toLong)
        case _ => num
      }
      case Some((_,"d")) | Some((_,"f")) => num match {
        case jdb: JDouble => jdb
        case JInt(i) => JDouble(i.toDouble)
        case JLong(l) => JDouble(l.toDouble)
        case JDecimal(dc) => JDouble(dc.toDouble)
        case _ => num
      }
      case Some((_,"i")) => num match {
        case ji: JInt => ji
        case JLong(l) => JInt(l.toInt)
        case JDouble(db) => JInt(db.toInt)
        case JDecimal(dc) => JInt(dc.toInt)
        case _ => num
      }
      case Some(_) => num
    }
}
  
  val hexCharRegex = "[a-fA-F]".r
  private def string2Number(str: String, base: Option[Int] = None): JValue = try {
    if (str.take(2) == "0x") {
      JLong(Integer.parseInt(str.drop(2), 16))
    } else if (str.take(2) == "0b") {
      JLong(Integer.parseInt(str.drop(2), 2))
    } else if (base != None) {
      JLong(Integer.parseInt(str, base.get))
    } else str.toLowerCase().splitAt(str.length - 1) match {
      
      case (l, "l") => JLong(l.toLong)
      case (d, "d") => JDouble(d.toDouble)
      case (f, "f") => JDouble(f.toFloat)
      case (i, "i") => JInt(i.toInt)
      case _ => if (str.contains(".")) {
          JDouble(str.toDouble)
        } else {
          JInt(str.toInt)
        }
    }
  } catch {
    case e: Throwable => throw new JsonTransmutingException(s"Could not convert string $str to number: ${e.getMessage}", JString(str))
  }

  private def toBoolean(value: JValue, argument: Option[String]): JBool = {
    val bool: Boolean = value match {
      case jb: JBool => jb.value
      case JString(str: String) => 
        !(str.toLowerCase == "false" ||
          str.toLowerCase == "f" ||
          str.toLowerCase == "n")
      case JInt(int) => int != 0
      case JDouble(db) => db != 0
      case JDecimal(bd) => bd != 0
      case JLong(l) => l != 0
      case JObject(fieldLs) => !fieldLs.isEmpty
      case JArray(itemLs) => !itemLs.isEmpty
      case JNothing | JNull => false
    }
    
    JBool(argument.map { s => s.toLowerCase() } match {
      case None => bool
      case Some("!") | Some("not") => !bool
      case _ => bool
    })
  }
}


class JsonTransmutingException(val message: String, val jVal: JValue) 
  extends RuntimeException(s"$message. For Json:\n " + (if (jVal == JNothing) "nothing" else pretty(render(jVal))))

//    case JBool(bool) => 
//    case JString(str: String) => 
//    case JInt(int) => 
//    case JDouble(db) => 
//    case JDecimal(bd) => 
//    case JLong(l) =>
//    case JObject(fieldLs) => 
//    case JArray(itemLs) => 
//    case JNothing | JNull => 