package wordbots

import com.workday.montague.ccg.CcgCat
import com.workday.montague.parser.SemanticParseNode
import com.workday.montague.semantics.{Form, Lambda, Nonsense}

import scala.util.{Failure, Success}

case class ParserError(description: String, suggestions: Set[String] = Set())

object ErrorAnalyzer {
  def diagnoseError(input: String, parseResult: Option[SemanticParseNode[CcgCat]])
                   (implicit validationMode: ValidationMode = ValidateUnknownCard): Option[ParserError] = {
    parseResult.map(_.semantic) match {
      case Some(Form(v: AstNode)) =>
        // Handle successful semantic parse.
        // Does the parse produce a sentence (CCG category S)?
        parseResult.map(_.syntactic.category).getOrElse("None") match {
          case "S" =>
            // Does the parse produce a valid AST?
            AstValidator(validationMode).validate(v) match {
              case Success(_) => None
              case Failure(ex: Throwable) => Some(ParserError(ex.getMessage))
            }
          case category => Some(ParserError(s"Parser did not produce a complete sentence - expected category: S, got: $category"))
        }
      case Some(f: Form[_]) =>
        // Handle a semantic parse that finishes but produces an unexpected result.
        Some(ParserError(s"Parser did not produce a valid expression - expected an AstNode, got: $f"))
      case Some(l: Lambda[_]) =>
        // Handle successful syntactic parse but incomplete semantic parse.
        val firstArgType = l.k.toString.split(": ")(1).split(" =>")(0)
        Some(ParserError(s"Parse failed (missing $firstArgType)"))
      case Some(Nonsense(_)) =>
        // Handle successful syntactic parse but failed semantic parse.
        Some(ParserError(s"Parse failed (${diagnoseSemanticsError(parseResult)})"))
      case _ =>
        // Handle failed parse.
        if (findUnrecognizedTokens(input).nonEmpty) {
          Some(ParserError(s"Unrecognized word(s): ${findUnrecognizedTokens(input).mkString(", ")}"))
        } else {
          Some(diagnoseSyntaxError(input))
        }
    }
  }

  def findUnrecognizedTokens(input: String): Seq[String] = {
    val tokens = Parser.tokenizer(input)
    val lexicon = Lexicon.lexicon

    tokens.filter { token =>
      !lexicon.map.keys.exists(_.split(' ').contains(token)) && lexicon.funcs.forall(_(token).isEmpty)
    }
  }

  private def diagnoseSyntaxError(input: String): ParserError = {
    val words = input.split(" ")
    val edits = findValidEdits(words)

    val error: Option[String] = edits.headOption.map(_.description(words))
    val suggestions: Set[String] = edits.flatMap(_(words)).toSet.filter(isSemanticallyValid)

    ParserError(s"Parse failed (${error.getOrElse("syntax error")})", suggestions)
  }

  private def diagnoseSemanticsError(parseResult: Option[SemanticParseNode[CcgCat]]): ParserError = {
    val error = parseResult.map(_.exs.nonEmpty) match {
      case Some(true) =>
        val msgs = parseResult.get.exs.map (
          _.getMessage
            .replace("cannot be cast to", "is not a")
            .replaceAllLiterally("$", "")
            .replaceAllLiterally("wordbots.", "")
        )

        s"semantics mismatch - ${msgs.mkString(", ")}"
      case _ => "semantics mismatch"
    }

    ParserError(s"Parse failed ($error)")
  }

  private def findValidEdits(words: Seq[String]): Stream[Edit] = {
    // The time complexity of findValidEdits() is O(W*C) where W is the # of words and C is the # of CCG categories to try.
    // So ...
    val categories: Map[String, CcgCat] = {
      if (words.length <= 6) {
        // ... for shorter inputs, try all categories.
        Lexicon.categoriesMap.mapValues(_.head._1)
      } else if (words.length <= 15) {
        // ... for medium-length inputs, only try terminal categories.
        Lexicon.terminalCategoriesMap.mapValues(_.head._1)
      } else {
        // ... for very long inputs, don't try any categories (only attempt deletions).
        Map()
      }
    }

    val insertions: Stream[Edit] = for {
      i <- words.indices.inclusive.toStream
      (cat, pos) <- categories.toStream
      candidate = words.slice(0, i).mkString(" ") + s" $cat " + words.slice(i, words.length).mkString(" ")
      if isSyntacticallyValid(candidate)
    } yield Insert(i, pos)

    val deletions: Stream[Edit] = for {
      i <- words.indices.toStream
      candidate = words.slice(0, i).mkString(" ") + " " + words.slice(i + 1, words.length).mkString(" ")
      if isSyntacticallyValid(candidate)
    } yield Delete(i)

    val replacements: Stream[Edit] = for {
      i <- words.indices.toStream
      (cat, pos) <- categories.toStream
      candidate = words.slice(0, i).mkString(" ") + s" $cat " + words.slice(i + 1, words.length).mkString(" ")
      if isSyntacticallyValid(candidate)
    } yield Replace(i, pos)

    insertions ++ deletions ++ replacements
  }

  private def isSyntacticallyValid(candidate: String): Boolean = {
    candidate.nonEmpty && Parser.parseWithLexicon(candidate, Lexicon.syntaxOnlyLexicon).bestParse.isDefined
  }

  private def isSemanticallyValid(candidate: String): Boolean = {
    val parseResult = Parser.parse(candidate).bestParse
    parseResult.map(_.semantic) match {
      // Is the semantic parse successful?
      case Some(Form(v: AstNode)) =>
        // Does the parse produce a sentence (CCG category S)?
        parseResult.map(_.syntactic.category) == Some("S")
      case _ => false
    }
  }
}
