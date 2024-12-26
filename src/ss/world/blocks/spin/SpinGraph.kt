package ss.world.blocks.spin

import arc.graphics.*
import arc.struct.*
import mindustry.gen.*
/**
 * Represents a connected network ("graph") of SpinBlock buildings.
 *
 * This class uses BFS to split/rebuild components when a building is removed.
 * Calls to add/remove/merge mark the graph as 'dirty', so the actual calculation
 * is done once in [update] or manually via [calculate].
 */
class SpinGraph {

    // Current spin power that this graph provides.
    var spins: Float = 0f
    // Maximum stress tolerance of this entire network.
    var maxStress: Float = 0f
    // Current stress that the network experiences.
    var currentStress: Float = 0f

    private val producers: Seq<Building> = Seq()
    private val consumers: Seq<Building> = Seq()
    // All buildings belonging to this graph.
    val all: Seq<Building> = Seq()

    // Lazy recalculation flag.
    private var dirty = false

    // Graph color used for debug rendering.
    val color: Color
    // Simple unique ID for debug/identification.
    var graphID: Int = lastGraphID++

    companion object {
        private var lastGraphID: Int = 0

        /**
         * Assigns a color based on ID to differentiate graphs visually.
         */
        private fun colorFromID(id: Int): Color {
            val hue = (id * 45)
            val c = Color()
            c.fromHsv(hue.toFloat(), 1f, 1f)
            c.a = 0.3f
            return c
        }

        /**
         * Finds all *distinct* SpinGraphs of neighboring SpinBuilds which are
         * genuinely connectable (bidirectional canConnect(...) == true) to [building].
         *
         * If empty, it means no valid neighbors => building should create a new graph.
         */
        fun findNearbyGraphs(building: SpinBlock.SpinBuild): List<SpinGraph> {
            val blockSelf = building.block as? SpinBlock ?: return emptyList()
            val validNeighbors = building.proximity().filter { neighbor ->
                val neighborBuild = neighbor as? SpinBlock.SpinBuild ?: return@filter false
                val neighborBlock = neighborBuild.block as? SpinBlock ?: return@filter false
                // Check bidirectional canConnect
                blockSelf.canConnect(building, neighborBuild) &&
                        neighborBlock.canConnect(neighborBuild, building)
            }
            return validNeighbors
                .mapNotNull { (it as? SpinBlock.SpinBuild)?.module?.graph }
                .distinct()
        }
    }

    init {
        color = colorFromID(graphID)
    }

    /**
     * Marks the graph data as outdated. The actual recalculation
     * will happen once in [update] or when [calculate] is manually called.
     */
    fun markDirty() {
        dirty = true
    }

    /**
     * Call this method periodically (e.g. from building.updateTile())
     * to recalculate if the graph is marked dirty. This way, heavy operations
     * don't happen too frequently.
     */
    fun update() {
        if (dirty) {
            calculate()
            dirty = false
        }
    }

    /**
     * Adds the specified building to this graph if not present.
     */
    fun add(building: Building?) {
        if (building == null || all.contains(building)) return

        all.add(building)

        val block = building.block
        if (block is SpinBlock) {
            if (block.isProducer()) producers.add(building)
            if (block.isConsumer()) consumers.add(building)
        }

        (building as? SpinBlock.SpinBuild)?.module?.graph = this
        markDirty()
    }

    /**
     * Merges another graph into this one. The other graph is cleared afterward.
     */
    fun merge(other: SpinGraph) {
        if (this == other) return

        // Add all buildings from the other graph to this.
        for (building in other.all) {
            this.add(building)
            (building as? SpinBlock.SpinBuild)?.module?.graph = this
        }

        // Clear the other graph since it is merged into 'this'.
        other.clearInternal()
        other.markDirty()

        markDirty()
    }

    /**
     * Removes a building from this graph. If that removal splits the graph
     * into multiple components, create new SpinGraphs for each separate component.
     */
    fun remove(building: Building) {
        all.remove(building)
        producers.remove(building)
        consumers.remove(building)

        if (all.isEmpty) {
            clearInternal()
            return
        }

        // BFS to find how many subcomponents remain, ignoring the 'removed' building.
        val components = findComponents(building)

        if (components.size == 1) {
            // If there's only one connected component, rewrite it to this graph.
            rewriteFrom(components[0])
        } else {
            // If multiple components remain, this old graph is cleared,
            // and we create brand-new SpinGraphs for each sublist.
            clearInternal()
            for (comp in components) {
                val newGraph = SpinGraph()
                newGraph.clearInternal()
                for (b in comp) {
                    newGraph.add(b)
                    (b as? SpinBlock.SpinBuild)?.module?.graph = newGraph
                }
                newGraph.markDirty()
            }
        }
        markDirty()
    }

    /**
     * Searches for connected components using BFS, ignoring the 'removed' building.
     */
    fun findComponents(removed: Building? = null): List<List<Building>> {
        val visited = mutableSetOf<Building>()
        val components = mutableListOf<List<Building>>()

        for (b in all) {
            if (!visited.contains(b)) {
                val comp = exploreComponent(b, visited, removed)
                if (comp.isNotEmpty()) components.add(comp)
            }
        }
        return components
    }

    /**
     * Performs BFS from 'start' building, ignoring 'removed', collecting all connected buildings.
     */
    private fun exploreComponent(
        start: Building,
        visited: MutableSet<Building>,
        removed: Building? = null
    ): List<Building> {
        val queue = Queue<Building>()
        val component = mutableListOf<Building>()

        queue.addLast(start)
        while (!queue.isEmpty) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            component.add(current)

            for (connected in buildingConnected(current)) {
                if (!visited.contains(connected) && connected != removed) {
                    queue.addLast(connected)
                }
            }
        }
        return component
    }

    /**
     * Collects all valid SpinBlock neighbors of [building] *within this graph*
     * that share a connection (bidirectional canConnect).
     */
    private fun buildingConnected(building: Building): Seq<Building> {
        val result = Seq<Building>()
        for (c in building.proximity()) {
            if (c.block is SpinBlock && all.contains(c)) {
                val blockA = building.block as SpinBlock
                val blockB = c.block as SpinBlock
                if (blockA.canConnect(building, c) && blockB.canConnect(c, building)) {
                    result.add(c)
                }
            }
        }
        return result
    }

    /**
     * Clears internal data without marking the graph dirty or removing references from other objects.
     */
    private fun clearInternal() {
        all.clear()
        producers.clear()
        consumers.clear()
        spins = 0f
        maxStress = 0f
        currentStress = 0f
    }

    /**
     * Recalculates spin/stress usage. Called from [update] if [dirty] == true,
     * or can be called manually if needed.
     */
    fun calculate() {
        spins = 0f
        maxStress = 0f
        currentStress = 0f

        for (building in all) {
            val block = building.block
            if (block is SpinBlock) {
                spins += block.getGeneratedSpins(building.tile)
                maxStress += block.getMaxStress(building.tile)
                currentStress += block.getGeneratedStress(building.tile)
            }
        }

        // If stress exceeds the maximum, the entire network effectively produces 0 spin.
        if (currentStress > maxStress) {
            spins = 0f
        }
    }

    /**
     * Reassigns this graph to a single BFS component [component].
     */
    private fun rewriteFrom(component: List<Building>) {
        clearInternal()
        for (b in component) {
            add(b)
            (b as? SpinBlock.SpinBuild)?.module?.graph = this
        }
        markDirty()
    }

    override fun toString(): String {
        return "SpinGraph(" +
                "id=$graphID, spins=$spins, maxStress=$maxStress, " +
                "currentStress=$currentStress, producers=${producers.size}, " +
                "consumers=${consumers.size}, all=${all.size})"
    }
}