package ca.uwaterloo.flix.runtime.datastore

import ca.uwaterloo.flix.language.ast.TypedAst
import ca.uwaterloo.flix.runtime.Value

import scala.collection.mutable

/**
 * A class that stores a relation in an indexed database.
 *
 * The indexes are given as a set of sequences over the column offsets. Offsets start from zero.
 *
 * For example, if given Set(Seq(1)) then the table has exactly one index on the 1st attribute of the relation.
 * As another example, if given Set(Seq(1, 2), Seq(0, 3)) then the relation has two indexes:
 * One index on the 1st and 2nd attributes and another index on the 0th and 3rd column index.
 *
 * @param relation the relation.
 * @param indexes the indexes.
 */
class IndexedRelation(relation: TypedAst.Collection.Relation, indexes: Set[Seq[Int]]) {

  /**
   * A map from keys, i.e. (index, value) pairs, to rows matching the key.
   */
  val store = mutable.Map.empty[(Seq[Int], Seq[Value]), mutable.Set[Array[Value]]]

  /**
   * The default index (which always exists).
   *
   * This corresponds to an index on all attributes.
   */
  val defaultIndex: Seq[Int] = relation.attributes.zipWithIndex.map(_._2)

  /**
   * Returns an iterator over all rows currently in the relation.
   *
   * This operation may be slow and should only be used for debugging.
   */
  def table: Iterator[Array[Value]] = scan

  /**
   * Processes a new inferred fact `f`.
   *
   * Adds the fact to the relation and returns `true` iff it did not already exist.
   */
  def inferredFact(f: Array[Value]): Boolean = {
    val key = (defaultIndex, defaultIndex map f)

    // check if the fact already exists in the primary index.
    // if so, no changes are needed and we return false.
    if (store contains key)
      return false

    // otherwise we must add the fact to the relation.
    newFact(f)
  }

  /**
   * Updates all indexes and tables with a new fact `f`.
   */
  private def newFact(f: Array[Value]): Boolean = {
    for (idx <- indexes + defaultIndex) {
      val key = (idx, idx map f)
      val table = store.getOrElse(key, {
        val newTable = mutable.Set.empty[Array[Value]]
        store(key) = newTable
        newTable
      })
      table += f
    }
    true
  }

  /**
   * Performs a lookup of the given row. The row may contain `null` entries. If so, these are interpreted as free variables.
   *
   * Returns a traversable over the matched rows.
   */
  def lookup(row: Array[Value]): Iterator[Array[Value]] = {
    val idx = row.toSeq.zipWithIndex.collect {
      case (v, i) if v != null => i
    }
    val key = (idx, idx map row)

    if (indexes contains idx) {
      // use index
      store(key).iterator
    } else {
      // table scan
      table
    }
  }

  /**
   * Returns all rows in the relation using a table scan.
   */
  // TODO: Improve performance ...
  def scan: Iterator[Array[Value]] = (store map {
    case (_, rows) => rows
  }).toList.flatten.toIterator

}