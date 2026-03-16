import kotlin.test.Test
import kotlin.test.assertEquals
/**
 * A `Hierarchy` stores an arbitrary _forest_ (an ordered collection of ordered trees)
 * as an array of node IDs in the order of DFS traversal, combined with a parallel array of node depths.
 *
 * Parent-child relationships are identified by the position in the array and the associated depth.
 * Each tree root has depth 0, its children have depth 1 and follow it in the array, their children have depth 2 and follow them, etc.
 *
 * Example:
 * ```
 * nodeIds: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * depths:  0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2
 * ```
 *
 * the forest can be visualized as follows:
 * ```
 * 1
 * - 2
 * - - 3
 * - - - 4
 * - 5
 * 6
 * - 7
 * 8
 * - 9
 * - 10
 * - - 11
 *```
 * 1 is a parent of 2 and 5, 2 is a parent of 3, etc. Note that depth is equal to the number of hyphens for each node.
 *
 * Invariants on the depths array:
 *  * Depth of the first element is 0.
 *  * If the depth of a node is `D`, the depth of the next node in the array can be:
 *      * `D + 1` if the next node is a child of this node;
 *      * `D` if the next node is a sibling of this node;
 *      * `d < D` - in this case the next node is not related to this node.
 */
interface Hierarchy {
  /** The number of nodes in the hierarchy. */
  val size: Int

  /**
   * Returns the unique ID of the node identified by the hierarchy index. The depth for this node will be `depth(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun nodeId(index: Int): Int

  /**
   * Returns the depth of the node identified by the hierarchy index. The unique ID for this node will be `nodeId(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun depth(index: Int): Int

  fun formatString(): String {
    return (0 until size).joinToString(
      separator = ", ",
      prefix = "[",
      postfix = "]"
    ) { i -> "${nodeId(i)}:${depth(i)}" }
  }
}

/**
 * A node is present in the filtered hierarchy if its node ID passes the predicate and all of its ancestors pass it as well.
 */
fun Hierarchy.filter(nodeIdPredicate: (Int) -> Boolean): Hierarchy {
  val filteredIds = mutableListOf<Int>()
  val filteredDepths = mutableListOf<Int>()
  var failingAncestorDepth = -1

  for (i in 0 until size) {
    val d = depth(i)

    if (failingAncestorDepth != -1 && d <= failingAncestorDepth) {
      failingAncestorDepth = -1
    }

    val validNode = failingAncestorDepth == -1 && nodeIdPredicate(nodeId(i))

    if (validNode) {
      filteredIds.add(nodeId(i))
      filteredDepths.add(d)
    } else {
      failingAncestorDepth = d
    }
  }

  return ArrayBasedHierarchy(filteredIds.toIntArray(), filteredDepths.toIntArray())
}

class ArrayBasedHierarchy(
  private val myNodeIds: IntArray,
  private val myDepths: IntArray,
) : Hierarchy {
  override val size: Int = myDepths.size

  override fun nodeId(index: Int): Int = myNodeIds[index]

  override fun depth(index: Int): Int = myDepths[index]
}

class FilterTest {
  @Test
  fun testFilter() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
    val filteredExpected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 5, 8, 10, 11),
      intArrayOf(0, 1, 1, 0, 1, 2))
    assertEquals(filteredExpected.formatString(), filteredActual.formatString())
  }

  @Test
  fun rootOnlyPasses() {
    val h: Hierarchy = ArrayBasedHierarchy(intArrayOf(5), intArrayOf(0))
    val filtered = h.filter { it == 5 }
    assertEquals("[5:0]", filtered.formatString())
  }

  @Test
  fun childDroppedWhenParentFails() {
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3),
      intArrayOf(0, 1, 0)
    )
    val filtered = h.filter { id -> id != 1 }
    assertEquals("[3:0]", filtered.formatString())
  }

  @Test
  fun failedNodeDoesNotAffectLaterSiblings() {
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4),
      intArrayOf(0, 1, 0, 1)
    )
    val filtered = h.filter { id -> id != 2 } // drop child of first root
    assertEquals("[1:0, 3:0, 4:1]", filtered.formatString())
  }

    @Test
  fun emptyList() {
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(),
      intArrayOf()
    )
    val filtered = h.filter { id -> id != 2 } 
    assertEquals("[]", filtered.formatString())
  }
}
