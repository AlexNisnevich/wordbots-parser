package wordbots

import com.workday.montague.parser.TokenMatcher

object StrictIntegerMatcher extends TokenMatcher[Int] {
  def apply(str: String): Seq[Int] = {
    if (str forall Character.isDigit) {
      Seq(Integer.parseInt(str))
    } else {
      Nil
    }
  }
}

case class PrefixedIntegerMatcher(prefix: String) extends TokenMatcher[Int] {
  def apply(str: String): Seq[Int] = {
    try {
      if (str.startsWith(prefix)) {
        Seq(Integer.parseInt(str.stripPrefix(prefix)))
      } else {
        Nil
      }
    } catch {
      case nfe: NumberFormatException => Nil
    }
  }
}

object NumberWordMatcher extends TokenMatcher[Int] {
  val numberWords = Seq("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")

  def apply(str: String): Seq[Int] = {
    numberWords.indexOf(str) match {
      case idx if idx > -1 => Seq(idx)
      case _               => Nil
    }
  }
}
