package wordbots

sealed trait AstNode extends Product

sealed trait Label extends AstNode
trait MultiLabel extends Label { def labels: Seq[Label] }

sealed trait Action extends AstNode
  case class MultipleActions(actions: Seq[Action]) extends Action
  object And { def apply(actions: Action*): MultipleActions = MultipleActions(actions) }

  case class If(condition: GlobalCondition, action: Action) extends Action
  case class Instead(action: Action) extends Action
  case class Until(duration: Duration, action: Action) extends Action

  case class Become(source: TargetObject, target: TargetCard) extends Action
  case class BecomeACopy(source: TargetObject, target: TargetObject) extends Action
  case class CanAttackAgain(target: TargetObject) extends Action
  case class CanMoveAgain(target: TargetObject) extends Action
  case class CanMoveAndAttackAgain(target: TargetObject) extends Action
  case class DealDamage(target: TargetObjectOrPlayer, num: Number) extends Action
  case class Destroy(target: TargetObject) extends Action
  case class Discard(target: TargetCard) extends Action
  case class Draw(target: TargetPlayer, num: Number) extends Action
  case object EndTurn extends Action
  case class GiveAbility(target: TargetObject, ability: Ability) extends Action
  case class ModifyAttribute(target: TargetObjectOrCard, attribute: Attribute, operation: Operation) extends Action
  case class ModifyEnergy(target: TargetPlayer, operation: Operation) extends Action
  case class MoveObject(target: TargetObject, dest: TargetObject) extends Action
  case class PayEnergy(target: TargetPlayer, amount: Number) extends Action
  case class RemoveAllAbilities(target: TargetObject) extends Action
  case class ReturnToHand(target: TargetObject) extends Action
  case class RestoreAttribute(target: TargetObject, attribute: Attribute, num: Option[Number] = None) extends Action
  case class SetAttribute(target: TargetObject, attribute: Attribute, num: Number) extends Action
  case class SwapAttributes(target: TargetObject, attr1: Attribute, attr2: Attribute) extends Action
  case class TakeControl(player: TargetPlayer, target: TargetObject) extends Action

  case class SaveTarget(target: Target) extends Action

sealed trait Ability extends AstNode
  case class MultipleAbilities(abilities: Seq[Ability]) extends Ability
  case class TriggeredAbility(trigger: Trigger, action: Action) extends Ability
  case class ActivatedAbility(action: Action) extends Ability
  sealed trait PassiveAbility extends Ability
    case class ApplyEffect(target: TargetObjectOrCard, effect: Effect) extends PassiveAbility
    case class AttributeAdjustment(target: TargetObjectOrCard, attribute: Attribute, operation: Operation) extends PassiveAbility
    case class FreezeAttribute(target: TargetObjectOrCard, attribute: Attribute) extends PassiveAbility
    case class HasAbility(target: TargetObjectOrCard, ability: Ability) extends PassiveAbility

sealed trait Effect extends AstNode
  case object CanMoveOverObjects extends Effect with Label
  case object CannotAttack extends Effect with Label
  case object CannotFightBack extends Effect with Label
  case class CanOnlyAttack(target: TargetObject) extends Effect

sealed trait Trigger extends AstNode
  case class AfterAttack(target: TargetObject, attackedObjectType: ObjectType = AllObjects) extends Trigger
  case class AfterCardPlay(target: TargetPlayer, cardType: CardType = AnyCard) extends Trigger
  case class AfterDamageReceived(target: TargetObject) extends Trigger
  case class AfterDestroyed(target: TargetObject, cause: TriggerEvent = AnyEvent) extends Trigger
  case class AfterMove(Target: TargetObject) extends Trigger
  case class AfterPlayed(Target: TargetObject) extends Trigger
  case class BeginningOfTurn(player: TargetPlayer) extends Trigger
  case class EndOfTurn(player: TargetPlayer) extends Trigger

sealed trait Target extends AstNode
  sealed trait TargetObjectOrCard extends Target
  sealed trait TargetObjectOrPlayer extends Target

  sealed trait TargetObject extends TargetObjectOrCard with TargetObjectOrPlayer
    case class ChooseO(collection: ObjectCollection) extends TargetObject
    case class AllO(collection: ObjectCollection) extends TargetObject
    case class RandomO(num: Number, collection: ObjectCollection) extends TargetObject
    case object ThisObject extends TargetObject
    case object ItO extends TargetObject  // (Salient object)
    case object That extends TargetObject  // (Salient object, but preferring the undergoer of the action over the agent)
    case object They extends TargetObject  // (Salient object, but preferring the current object in an iteration over a collection)
    case object SavedTargetObject extends TargetObject
  sealed trait TargetCard extends TargetObjectOrCard
    case class CopyOfC(objToCopy:TargetObject) extends TargetCard
    case class SpecificC() extends TargetCard //currently hardcoded to a 1/1/1 creature. will implement customization in a later patch.
    case class ChooseC(collection: CardCollection) extends TargetCard
    case class AllC(collection: CardCollection) extends TargetCard
    case class RandomC(num: Number, collection: CardCollection) extends TargetCard
  sealed trait TargetPlayer extends Target with TargetObjectOrPlayer
    case object Self extends TargetPlayer
    case object Opponent extends TargetPlayer
    case object AllPlayers extends TargetPlayer
    case object ItP extends TargetPlayer  // (Salient player)
    case class ControllerOf(t: TargetObject) extends TargetPlayer

sealed trait Condition extends AstNode
  case class AdjacentTo(obj: TargetObject) extends Condition
  case class AttributeComparison(attribute: Attribute, comparison: Comparison) extends Condition
  case class ControlledBy(player: TargetPlayer) extends Condition
  case class HasProperty(property: Property) extends Condition
  case object Unoccupied extends Condition
  case class WithinDistanceOf(distance: Number, obj: TargetObject) extends Condition

sealed trait GlobalCondition extends AstNode
  case class CollectionExists(coll: Collection) extends GlobalCondition
  case class TargetHasProperty(target: TargetObject, property: Property) extends GlobalCondition

sealed trait Operation extends AstNode
  case class Constant(num: Number) extends Operation
  case class Plus(num: Number) extends Operation
  case class Minus(num: Number) extends Operation
  case class Multiply(num: Number) extends Operation
  case class Divide(num: Number, rounding: Rounding) extends Operation

sealed trait Comparison extends AstNode
  case class EqualTo(num: Number) extends Comparison
  case class GreaterThan(num: Number) extends Comparison
  case class GreaterThanOrEqualTo(num: Number) extends Comparison
  case class LessThan(num: Number) extends Comparison
  case class LessThanOrEqualTo(num: Number) extends Comparison

sealed trait Number extends AstNode
  case class Scalar(num: Int) extends Number
  case class AttributeSum(collection: Collection, attribute: Attribute) extends Number
  case class AttributeValue(obj: TargetObject, attribute: Attribute) extends Number
  case class Count(collection: Collection) extends Number
  case class EnergyAmount(player: TargetPlayer) extends Number

sealed trait Collection extends AstNode
  sealed trait CardCollection extends Collection
    case class CardsInHand(player: TargetPlayer, cardType: CardType = AnyCard) extends CardCollection
  sealed trait ObjectCollection extends Collection with TargetObject
    case object AllTiles extends ObjectCollection
    object ObjectsInPlay { def apply(objectType: ObjectType): ObjectCollection = ObjectsMatchingConditions(objectType, Seq()) }
    case class ObjectsMatchingConditions(objectType: ObjectType, conditions: Seq[Condition]) extends ObjectCollection
    case class Other(collection: Collection) extends ObjectCollection
    case class TilesMatchingConditions(conditions: Seq[Condition]) extends ObjectCollection

sealed trait CardType extends Label
  case object AnyCard extends CardType
  case object Event extends CardType
  sealed trait ObjectType extends CardType
    case object Robot extends ObjectType
    case object Kernel extends ObjectType
    case object Structure extends ObjectType
    case object AllObjects extends ObjectType
    case class MultipleObjectTypes(labels: Seq[ObjectType]) extends ObjectType with MultiLabel

sealed trait TriggerEvent extends Label
  case object AnyEvent extends TriggerEvent
  case object Combat extends TriggerEvent

sealed trait Attribute extends Label
  case object Attack extends Attribute
  case object Cost extends Attribute
  case object Health extends Attribute
  case object Speed extends Attribute
  case object AllAttributes extends Attribute

sealed trait Property extends Label
  case object AttackedLastTurn extends Property
  case object AttackedThisTurn extends Property
  case object MovedLastTurn extends Property
  case object MovedThisTurn extends Property
  case object IsDamaged extends Property
  case object IsDestroyed extends Property

sealed trait Duration extends AstNode
  case class TurnsPassed(turns: Int) extends Duration

sealed trait Rounding extends Label
  case object RoundedUp extends Rounding
  case object RoundedDown extends Rounding

// The below container classes are used to store state mid-parse but not expressed in the final parsed AST.

// Nullary containers:
case object ItsOwnersHand

// Unary containers:
case class Cards(num: Number)
case class Damage(amount: Number)
case class Energy(amount: Number)
case class Life(amount: Number)
case class Hand(player: TargetPlayer)
case class Spaces(num: Number)
case class Turn(player: TargetPlayer)
case class WithinDistance(spaces: Number)

// Binary containers:
case class TargetAttribute(target: TargetObject, attr: Attribute)
case class AttributeAmount(amount: Number, attr: Attribute)
case class AttributeOperation(op: Operation, attr: Attribute)
case class CardPlay(player: TargetPlayer, cardType: CardType)
case class RandomCards(num: Number, cardType: CardType)
