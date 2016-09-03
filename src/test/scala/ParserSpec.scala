package wordbots

import com.workday.montague.semantics.Form
import org.scalatest._

class ParserSpec extends FlatSpec with Matchers {
  def parse(input: String) = {
    Parser.parse(input).bestParse.get.semantic match {
      case Form(value) => value
    }
  }

  it should "parse simple actions" in {
    parse("draw a card") should be (Draw(Self, 1))
    parse("destroy a robot") should be (Destroy(Choose(Robot, NoCondition)))
    parse("gain 2 energy") should be (EnergyDelta(Self, Plus(2)))
    parse("deal 2 damage to a robot") should be (DealDamage(Choose(Robot, NoCondition), 2))
  }

  it should "parse more complex actions with relative clauses" in {
    parse("deal 2 damage to a robot that has 3 or less speed") should be (
      DealDamage(Choose(Robot, AttributeComparison(Speed, LessThanOrEqualTo(3))), 2)
    )
  }
}