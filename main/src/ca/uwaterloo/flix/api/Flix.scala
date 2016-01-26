package ca.uwaterloo.flix.api

import java.nio.file.{Files, Path, Paths}

import ca.uwaterloo.flix.language.Compiler
import ca.uwaterloo.flix.language.ast._
import ca.uwaterloo.flix.runtime.{Invokable, Model, Solver}
import ca.uwaterloo.flix.util.{Options, Validation}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Main programmatic interface for Flix.
  */
class Flix {

  /**
    * A sequence of strings to parsed into Flix ASTs.
    */
  private val strings = ListBuffer.empty[String]

  /**
    * A sequence of paths to be parsed into Flix ASTs.
    */
  private val paths = ListBuffer.empty[Path]

  /**
    * A map of hooks to JVM invokable methods.
    */
  private val hooks = mutable.Map.empty[Name.Resolved, Ast.Hook]

  /**
    * The current Flix options.
    */
  private var options = Options.Default

  /**
    * Adds the given string `s` to the list of strings to be parsed.
    */
  def addStr(s: String): Flix = {
    if (s == null)
      throw new IllegalArgumentException("'s' must be non-null.")
    strings += s
    this
  }

  /**
    * Adds the given path `p` to the list of paths to be parsed.
    */
  def addPath(p: String): Flix = {
    if (p == null)
      throw new IllegalArgumentException("'p' must be non-null.")
    paths += Paths.get(p)
    this
  }

  /**
    * Adds the given path `p` to the list of paths to be parsed.
    */
  def addPath(p: Path): Flix = {
    if (p == null)
      throw new IllegalArgumentException("'p' must be non-null.")
    if (!Files.exists(p))
      throw new IllegalArgumentException("'p' must a file.")
    if (!Files.isRegularFile(p))
      throw new IllegalArgumentException("'p' must a regular file.")
    if (!Files.isReadable(p))
      throw new IllegalArgumentException("'p' must a readable file.")

    paths += p
    this
  }

  /**
    * Adds the given invokable `inv` with the given `name.`
    *
    * @param name the fully qualified name for the invokable.
    * @param tpe  the Flix type of the invokable.
    * @param inv  the invokable method.
    */
  def addHook(name: String, tpe: Type.Lambda, inv: Invokable): Flix = {
    if (name == null)
      throw new IllegalArgumentException("'name' must be non-null.")
    if (inv == null)
      throw new IllegalArgumentException("'inv' must be non-null.")
    if (tpe == null)
      throw new IllegalArgumentException("'tpe' must be non-null.")

    val rname = Name.Resolved.mk(name)
    hooks.get(rname) match {
      case None => hooks += (rname -> Ast.Hook(rname, inv, tpe))
      case Some(otherHook) =>
        throw new IllegalStateException(s"Another hook already exists for the given '$name'.")
    }
    this
  }

  /**
    * Sets the options used for this Flix instance.
    */
  def setOptions(opts: Options): Flix = {
    if (opts == null)
      throw new IllegalArgumentException("'opts' must be non-null.")
    options = opts
    this
  }

  /**
    * Compiles the Flix program and returns the typed ast.
    */
  def compile(): Validation[TypedAst.Root, FlixError] = {
    if (strings.isEmpty && paths.isEmpty)
      throw new IllegalStateException("No input specified. Please add at least one string or path input.")

    Compiler.compile(getSourceInputs, hooks.toMap)
  }

  /**
    * Solves the Flix program and returns the minimal model.
    *
    * NB: Automatically calls `compile()` thus there is no reason to do so manually.
    */
  def solve(): Validation[Model, FlixError] = {
    compile() map {
      case ast => new Solver()(Solver.SolverContext(ast, options)).solve()
    }
  }

  /**
    * Returns a list of source inputs constructed
    * from the strings and paths in this builder.
    */
  private def getSourceInputs: List[SourceInput] = {
    val si1 = strings.foldLeft(List.empty[SourceInput]) {
      case (xs, s) => SourceInput.Str(s) :: xs
    }
    val si2 = paths.foldLeft(List.empty[SourceInput]) {
      case (xs, p) if p.getFileName.toString.endsWith(".flix") => SourceInput.TxtFile(p) :: xs
      case (xs, p) if p.getFileName.toString.endsWith(".flix.zip") => SourceInput.ZipFile(p) :: xs
      case (xs, p) if p.getFileName.toString.endsWith(".flix.gzip") => SourceInput.ZipFile(p) :: xs
      case (_, p) => throw new IllegalStateException(s"Unknown file type '${p.getFileName}'.")
    }

    si1 ::: si2
  }


  //def mkBool()

}