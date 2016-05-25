package ca.uwaterloo.flix.language.phase

import ca.uwaterloo.flix.language.ast._

import scala.collection.mutable

object Typer2 {

  /**
    * A common super-type for type constraints.
    */
  sealed trait TypeConstraint

  object TypeConstraint {

    /**
      * A type constraint that requires `t1` to unify with `t2`.
      */
    case class Eq(t1: Type, t2: Type) extends TypeConstraint

    @deprecated
    case class AllEq(tpes: List[Type]) extends TypeConstraint

    /**
      * A type constraint that requires `t1` to unify with one of the types `ts`.
      */
    case class OneOf(t1: Type, ts: Set[Type]) extends TypeConstraint

  }

  /**
    * The integral types.
    */
  val IntTypes: Set[Type] = Set(
    Type.Int8,
    Type.Int16,
    Type.Int32,
    Type.Int64
  )

  /**
    * The number types (i.e. integral and fractional types).
    */
  val NumTypes: Set[Type] = Set(
    Type.Float32,
    Type.Float64,
    Type.Int8,
    Type.Int16,
    Type.Int32,
    Type.Int64
  )

  // TODO: DOC + Impl
  def typer(program: NamedAst.Program)(implicit genSym: GenSym): Unit = {
    for ((ns, defns) <- program.definitions) {
      for ((name, defn) <- defns) {
        ConstraintGeneration.Expressions.gen(defn.exp)
      }
    }
  }

  /**
    * Phase 1: Constraint Generation.
    */
  object ConstraintGeneration {

    object Expressions {

      /**
        * Generates type constraints for the given expression `exp0`.
        */
      def gen(exp0: NamedAst.Expression)(implicit genSym: GenSym): Unit = {

        /*
         * A mutable set to hold the collected constraints.
         */
        val constraints = mutable.Set.empty[TypeConstraint]

        /**
          * Generates type constraints for the given expression `e0` under the given type environment `tenv`.
          */
        def visitExp(e0: NamedAst.Expression, tenv0: Map[Name.Ident, Type]): Type = e0 match {

          /*
           * Wildcard expression.
           */
          case NamedAst.Expression.Wild(id, loc) =>
            ???

          /*
           * Variable expression.
           */
          case NamedAst.Expression.Var(ident, sym, loc) =>
            // TODO: Lookup the type in the type environment.
            ???

          /*
           * Reference expression.
           */
          case NamedAst.Expression.Ref(id, ref, loc) =>
            Type.Var("TODO")
          // TODO

          /*
           * Literal expression.
           */
          case NamedAst.Expression.Unit(id, _) => Type.Unit
          case NamedAst.Expression.True(id, loc) => Type.Bool
          case NamedAst.Expression.False(id, loc) => Type.Bool
          case NamedAst.Expression.Char(id, lit, loc) => Type.Char
          case NamedAst.Expression.Float32(id, lit, loc) => Type.Float32
          case NamedAst.Expression.Float64(id, lit, loc) => Type.Float64
          case NamedAst.Expression.Int8(id, lit, loc) => Type.Int8
          case NamedAst.Expression.Int16(id, lit, loc) => Type.Int16
          case NamedAst.Expression.Int32(id, lit, loc) => Type.Int32
          case NamedAst.Expression.Int64(id, lit, loc) => Type.Int64
          case NamedAst.Expression.BigInt(id, lit, loc) => Type.BigInt
          case NamedAst.Expression.Str(id, lit, loc) => Type.Str

          /*
           * Hook expression.
           */
          //case NamedAst.Expression.Hook(id, hook, tpe, loc) => ???

          /*
           * Lambda expression.
           */
          case NamedAst.Expression.Lambda(args, body, tpe, loc) => ???

          /*
           * Apply expression.
           */
          case NamedAst.Expression.Apply(id, exp1, args, _) =>
            // TODO
            val tpe1 = visitExp(exp1, tenv0)

            val r = Type.Var(genSym.fresh2().name) // TODO
            //(TypeConstraint.EqType(tpe1, Type.Lambda(tpes, r)) :: c1 ::: cs.flatten, r)
            r

          /*
           * Unary expression.
           */
          case NamedAst.Expression.Unary(id, op, exp1, _) => op match {
            case UnaryOperator.LogicalNot =>
              val tpe = visitExp(exp1, tenv0)
              constraints += TypeConstraint.Eq(tpe, Type.Bool)
              Type.Bool

            case UnaryOperator.Plus | UnaryOperator.Minus =>
              val tpe = visitExp(exp1, tenv0)
              constraints += TypeConstraint.OneOf(tpe, NumTypes)
              tpe

            case UnaryOperator.BitwiseNegate =>
              val tpe = visitExp(exp1, tenv0)
              constraints += TypeConstraint.OneOf(tpe, IntTypes)
              tpe
          }

          /*
           * Binary expression.
           */
          // TODO: Check if we can use stuff like ArithmeticOperator, etc.
          case NamedAst.Expression.Binary(id, op, e1, e2, _) => op match {
            case BinaryOperator.Plus | BinaryOperator.Minus | BinaryOperator.Times | BinaryOperator.Divide =>
              val tpe1 = visitExp(e1, tenv0)
              val tpe2 = visitExp(e2, tenv0)
              constraints += TypeConstraint.Eq(tpe1, tpe2)
              constraints += TypeConstraint.OneOf(tpe1, NumTypes)
              constraints += TypeConstraint.OneOf(tpe2, NumTypes)
              tpe1

            case BinaryOperator.Modulo =>
              val tpe1 = visitExp(e1, tenv0)
              val tpe2 = visitExp(e2, tenv0)
              constraints += TypeConstraint.Eq(tpe1, tpe2)
              constraints += TypeConstraint.OneOf(tpe1, NumTypes)
              constraints += TypeConstraint.OneOf(tpe2, NumTypes)
              tpe1

            case BinaryOperator.Exponentiate =>
              val tpe1 = visitExp(e1, tenv0)
              val tpe2 = visitExp(e2, tenv0)
              constraints += TypeConstraint.Eq(tpe1, tpe2)
              constraints += TypeConstraint.OneOf(tpe1, NumTypes)
              constraints += TypeConstraint.OneOf(tpe2, NumTypes)
              tpe1

            case BinaryOperator.Equal | BinaryOperator.NotEqual =>
              val tpe1 = visitExp(e1, tenv0)
              val tpe2 = visitExp(e2, tenv0)
              constraints += TypeConstraint.Eq(tpe1, tpe2)
              Type.Bool

            case BinaryOperator.Less | BinaryOperator.LessEqual | BinaryOperator.Greater | BinaryOperator.GreaterEqual =>
              val tpe1 = visitExp(e1, tenv0)
              val tpe2 = visitExp(e2, tenv0)
              constraints += TypeConstraint.Eq(tpe1, tpe2)
              Type.Bool

            // TODO: Logical Operator, can we shorten?
            case BinaryOperator.LogicalAnd | BinaryOperator.LogicalOr | BinaryOperator.Implication | BinaryOperator.Biconditional =>
              val tpe1 = visitExp(e1, tenv0)
              val tpe2 = visitExp(e2, tenv0)
              constraints += TypeConstraint.Eq(tpe1, Type.Bool)
              constraints += TypeConstraint.Eq(tpe2, Type.Bool)
              Type.Bool

            // TODO: BitwiseOperator, can we shorten?
            case BinaryOperator.BitwiseAnd | BinaryOperator.BitwiseOr | BinaryOperator.BitwiseXor | BinaryOperator.BitwiseLeftShift | BinaryOperator.BitwiseRightShift =>
              val tpe1 = visitExp(e1, tenv0)
              val tpe2 = visitExp(e2, tenv0)
              constraints += TypeConstraint.Eq(tpe1, tpe2)
              constraints += TypeConstraint.OneOf(tpe1, NumTypes)
              constraints += TypeConstraint.OneOf(tpe2, NumTypes)
              tpe1

          }

          /*
           * Let expression.
           */
          case NamedAst.Expression.Let(ident, exp1, exp2, tpe, _) =>
            // update type environment and recurse
            // TODO
            ???

          /*
           * If-then-else expression.
           */
          case NamedAst.Expression.IfThenElse(id, exp1, exp2, exp3, loc) =>
            val tpe1 = visitExp(exp1, tenv0)
            val tpe2 = visitExp(exp2, tenv0)
            val tpe3 = visitExp(exp3, tenv0)

            constraints += TypeConstraint.Eq(tpe1, Type.Bool)
            constraints += TypeConstraint.Eq(tpe2, tpe3)
            tpe2

          /*
           * Match expression.
           */
          case NamedAst.Expression.Match(exp1, rules, tpe, loc) => ???

          /*
           * Switch expression.
           */
          case NamedAst.Expression.Switch(id, rules, loc) =>
            val bodyTypes = mutable.ListBuffer.empty[Type]
            for ((cond, body) <- rules) {
              val condType = visitExp(cond, tenv0)
              bodyTypes += visitExp(body, tenv0)
              constraints += TypeConstraint.Eq(condType, Type.Bool)
            }

            constraints += TypeConstraint.AllEq(bodyTypes.toList)

            bodyTypes.head // TODO: Or generate fresh symbol?

          /*
           * Tag expression.
           */
          case NamedAst.Expression.Tag(id, enum, tag, exp, loc) =>
            // TODO
            val tpe = visitExp(exp, tenv0)
            Type.Enum(???, ???)

          /*
           * Tuple expression.
           */
          case NamedAst.Expression.Tuple(id, elms, loc) =>
            val tpes = elms.map(e => visitExp(e, tenv0))
            Type.Tuple(tpes)

          /*
           * Option expression.
           */
          case NamedAst.Expression.FNone(id, loc) => ???

          case NamedAst.Expression.FSome(id, exp, loc) => ???

          /*
           * Nil expression.
           */
          case NamedAst.Expression.FNil(id, loc) =>
            val sym = genSym.freshId()
            val tpe = Type.Var(sym.toString)
            Type.FList(tpe)

          /*
           * List expression.
           */
          case NamedAst.Expression.FList(id, hd, tl, loc) => ???

          /*
           * Vector expression.
           */
          case NamedAst.Expression.FVec(id, elms, loc) => ???

          /*
           * Set expression.
           */
          case NamedAst.Expression.FSet(id, elms, loc) =>
            val tpes = elms.map(e => visitExp(e, tenv0))
            constraints += TypeConstraint.AllEq(tpes)
            Type.FSet(tpes.head)

          /*
           * Map expression.
           */
          case NamedAst.Expression.FMap(id, elms, loc) => ???

          /*
           * GetIndex expression.
           */
          case NamedAst.Expression.GetIndex(id, exp1, exp2, loc) => ???

          /*
           * PutIndex expression.
           */
          case NamedAst.Expression.PutIndex(id, exp1, exp2, exp3, loc) => ???

          /*
           * Existential expression.
           */
          case NamedAst.Expression.Existential(id, params, e, loc) =>
            val tenv = params.foldLeft(tenv0) {
              case (macc, Ast.FormalParam(name, t)) => macc + (name -> t)
            }
            val tpe = visitExp(e, tenv)
            constraints += TypeConstraint.Eq(tpe, Type.Bool)
            Type.Bool

          /*
           * Universal expression.
           */
          case NamedAst.Expression.Universal(id, params, e, loc) =>
            val tenv = params.foldLeft(tenv0) {
              case (macc, Ast.FormalParam(name, t)) => macc + (name -> t)
            }
            val tpe = visitExp(e, tenv)
            constraints += TypeConstraint.Eq(tpe, Type.Bool)
            Type.Bool

          /*
           * Ascribe expression.
           */
          case NamedAst.Expression.Ascribe(id, exp, tpe, loc) =>
            ???

          /*
           * User Error expression.
           */
          case NamedAst.Expression.UserError(id, loc) =>
            // TODO
            ???

        }

        /**
          * Generates type constraints for the given pattern `p0` under the given type environment `tenv`.
          */
        def visitPat(p0: NamedAst.Pattern, tenv: Map[Name.Ident, Type]): Type = p0 match {
          case NamedAst.Pattern.Wild(loc) => ???
          case NamedAst.Pattern.Var(ident, loc) => ???
          case NamedAst.Pattern.Unit(loc) => Type.Unit
          case NamedAst.Pattern.True(_) => Type.Bool
          case NamedAst.Pattern.False(_) => Type.Bool
          case NamedAst.Pattern.Char(_, _) => Type.Char
          case NamedAst.Pattern.Float32(_, _) => Type.Float32
          case NamedAst.Pattern.Float64(_, _) => Type.Float64
          case NamedAst.Pattern.Int8(_, _) => Type.Int8
          case NamedAst.Pattern.Int16(_, _) => Type.Int16
          case NamedAst.Pattern.Int32(_, _) => Type.Int32
          case NamedAst.Pattern.Int64(_, _) => Type.Int64
          case NamedAst.Pattern.BigInt(_, _) => Type.BigInt
          case NamedAst.Pattern.Str(_, _) => Type.Str
          case NamedAst.Pattern.Tag(enum, tag, p1, loc) => ???
          case NamedAst.Pattern.Tuple(elms, loc) =>
            val tpes = elms.map(e => visitPat(e, tenv))
            Type.Tuple(tpes)

          case _ => ???
        }

        visitExp(exp0, Map.empty)

      }
    }

  }

  /**
    * Phase 2: Unification.
    */
  object Unification {

    // TODO: Possibly return map from names to types.
    def unify(cs: List[TypeConstraint.Eq]): List[TypeConstraint.Eq] = ???

    def unify(tc: TypeConstraint, tenv: Map[String, Type]): Map[String, Type] = tc match {
      // TODO: Probably needs to return Validation.
      case TypeConstraint.Eq(tpe1, tpe2) => unify(tpe1, tpe2)
      case TypeConstraint.AllEq(tpes) => ??? // TODO
      case TypeConstraint.OneOf(tpe1, ts) => ???
      // try to unify each, and require at least one?
      // how to ensure unique typing? need list of maps?

    }

    def unify(tpe1: Type, tpe2: Type): Map[String, Type] = (tpe1, tpe2) match {
      case (Type.Var(id), x) => ???
      case (x, Type.Var(id)) => ???
      case (Type.Unit, Type.Unit) => Map.empty
      case _ => ???
    }

  }



  /**
    * Reassembles the given expression `exp0` under the given type environment `tenv0`.
    */
  def reassemble(exp0: NamedAst.Expression, tenv0: Map[Int, Type]): TypedAst.Expression = exp0 match {
    /*
     * Wildcard expression.
     */
    case NamedAst.Expression.Wild(id, loc) => ??? // TODO

    /*
     * Variable expression.
     */
    case NamedAst.Expression.Var(id, sym, loc) => TypedAst.Expression.Var(???, tenv0(id), loc) // TODO

    /*
     * Reference expression.
     */
    case NamedAst.Expression.Ref(id, ref, loc) => TypedAst.Expression.Ref(???, tenv0(id), loc) // TODO

    /*
     * Literal expression.
     */
    case NamedAst.Expression.Unit(id, loc) => TypedAst.Expression.Unit(loc)
    case NamedAst.Expression.True(id, loc) => TypedAst.Expression.True(loc)
    case NamedAst.Expression.False(id, loc) => TypedAst.Expression.False(loc)
    case NamedAst.Expression.Char(id, lit, loc) => TypedAst.Expression.Char(lit, loc)
    case NamedAst.Expression.Float32(id, lit, loc) => TypedAst.Expression.Float32(lit, loc)
    case NamedAst.Expression.Float64(id, lit, loc) => TypedAst.Expression.Float64(lit, loc)
    case NamedAst.Expression.Int8(id, lit, loc) => TypedAst.Expression.Int8(lit, loc)
    case NamedAst.Expression.Int16(id, lit, loc) => TypedAst.Expression.Int16(lit, loc)
    case NamedAst.Expression.Int32(id, lit, loc) => TypedAst.Expression.Int32(lit, loc)
    case NamedAst.Expression.Int64(id, lit, loc) => TypedAst.Expression.Int64(lit, loc)
    case NamedAst.Expression.BigInt(id, lit, loc) => TypedAst.Expression.BigInt(lit, loc)
    case NamedAst.Expression.Str(id, lit, loc) => TypedAst.Expression.Str(lit, loc)

    /*
     * Apply expression.
     */
    case NamedAst.Expression.Apply(id, lambda, args, loc) => ??? // TODO

    /*
     * Lambda expression.
     */
    case NamedAst.Expression.Lambda(id, params, exp, loc) => ??? // TODO

    /*
     * Unary expression.
     */
    case NamedAst.Expression.Unary(id, op, exp, loc) =>
      val e = reassemble(exp, tenv0)
      TypedAst.Expression.Unary(op, e, tenv0(id), loc)

    /*
     * Binary expression.
     */
    case NamedAst.Expression.Binary(id, op, exp1, exp2, loc) =>
      val e1 = reassemble(exp1, tenv0)
      val e2 = reassemble(exp2, tenv0)
      TypedAst.Expression.Binary(op, e1, e2, tenv0(id), loc)

    /*
     * If-then-else expression.
     */
    case NamedAst.Expression.IfThenElse(id, exp1, exp2, exp3, loc) =>
      val e1 = reassemble(exp1, tenv0)
      val e2 = reassemble(exp2, tenv0)
      val e3 = reassemble(exp3, tenv0)
      TypedAst.Expression.IfThenElse(e1, e2, e3, tenv0(id), loc)

    /*
     * Let expression.
     */
    case NamedAst.Expression.Let(id, sym, exp1, exp2, loc) => ??? // TODO

    /*
     * Match expression.
     */
    case NamedAst.Expression.Match(id, exp, rules, loc) => ???  // TODO

    /*
     * Switch expression.
     */
    case NamedAst.Expression.Switch(id, rules, loc) =>
      val rs = rules.map {
        case (cond, body) => (reassemble(cond, tenv0), reassemble(body, tenv0))
      }
      TypedAst.Expression.Switch(rs, tenv0(id), loc)

    /*
     * Tag expression.
     */
    case NamedAst.Expression.Tag(id, enum, tag, exp, loc) =>
      val e = reassemble(exp, tenv0)
      val tpe: Type.Enum = tenv0(id).asInstanceOf[Type.Enum]
      TypedAst.Expression.Tag(???, tag, e, tpe, loc) // TODO

    /*
     * Tuple expression.
     */
    case NamedAst.Expression.Tuple(id, elms, loc) =>
      val es = elms.map(e => reassemble(e, tenv0))
      val tpe: Type.Tuple = tenv0(id).asInstanceOf[Type.Tuple]
      TypedAst.Expression.Tuple(es, tpe, loc)

    /*
     * None expression.
     */
    case NamedAst.Expression.FNone(id, loc) =>
      val tpe: Type.FOpt = tenv0(id).asInstanceOf[Type.FOpt]
      TypedAst.Expression.FNone(tpe, loc)

    /*
     * Some expression.
     */
    case NamedAst.Expression.FSome(id, exp, loc) =>
      val tpe: Type.FOpt = tenv0(id).asInstanceOf[Type.FOpt]
      TypedAst.Expression.FSome(reassemble(exp, tenv0), tpe, loc)

    /*
     * Nil expression.
     */
    case NamedAst.Expression.FNil(id, loc) =>
      val tpe: Type.FList = tenv0(id).asInstanceOf[Type.FList]
      TypedAst.Expression.FNil(tpe, loc)

    /*
     * List expression.
     */
    case NamedAst.Expression.FList(id, hd, tl, loc) =>
      val e1 = reassemble(hd, tenv0)
      val e2 = reassemble(tl, tenv0)
      val tpe: Type.FList = tenv0(id).asInstanceOf[Type.FList]
      TypedAst.Expression.FList(e1, e2, tpe, loc)

    /*
     * Vec expression.
     */
    case NamedAst.Expression.FVec(id, elms, loc) =>
      val es = elms.map(e => reassemble(e, tenv0))
      val tpe: Type.FVec = tenv0(id).asInstanceOf[Type.FVec]
      TypedAst.Expression.FVec(es, tpe, loc)

    /*
     * Set expression.
     */
    case NamedAst.Expression.FSet(id, elms, loc) =>
      val es = elms.map(e => reassemble(e, tenv0))
      val tpe: Type.FSet = tenv0(id).asInstanceOf[Type.FSet]
      TypedAst.Expression.FSet(es, tpe, loc)

    /*
     * Map expression.
     */
    case NamedAst.Expression.FMap(id, elms, loc) =>
      val es = elms map {
        case (key, value) => (reassemble(key, tenv0), reassemble(value, tenv0))
      }
      val tpe: Type.FMap = tenv0(id).asInstanceOf[Type.FMap]
      TypedAst.Expression.FMap(es, tpe, loc)

    /*
     * GetIndex expression.
     */
    case NamedAst.Expression.GetIndex(id, exp1, exp2, loc) =>
      val e1 = reassemble(exp1, tenv0)
      val e2 = reassemble(exp2, tenv0)
      TypedAst.Expression.GetIndex(e1, e2, tenv0(id), loc)

    /*
     * PutIndex expression.
     */
    case NamedAst.Expression.PutIndex(id, exp1, exp2, exp3, loc) =>
      val e1 = reassemble(exp1, tenv0)
      val e2 = reassemble(exp2, tenv0)
      val e3 = reassemble(exp3, tenv0)
      TypedAst.Expression.PutIndex(e1, e2, e3, tenv0(id), loc)

    /*
     * Existential expression.
     */
    case NamedAst.Expression.Existential(id, params, exp, loc) =>
      val e = reassemble(exp, tenv0)
      TypedAst.Expression.Existential(params, e, loc)

    /*
     * Universal expression.
     */
    case NamedAst.Expression.Universal(id, params, exp, loc) =>
      val e = reassemble(exp, tenv0)
      TypedAst.Expression.Universal(params, e, loc)

    /*
     * Ascribe expression.
     */
    case NamedAst.Expression.Ascribe(id, exp, tpe, loc) =>
      // simply reassemble the nested expression.
      reassemble(exp, tenv0)

    /*
     * User Error expression.
     */
    case NamedAst.Expression.UserError(id, loc) =>
      TypedAst.Expression.Error(tenv0(id), loc)
  }

}