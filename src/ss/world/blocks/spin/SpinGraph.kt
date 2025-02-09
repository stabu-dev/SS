package ss.world.blocks.spin

import arc.graphics.*
import arc.struct.*
import mindustry.gen.*
/**
 * Represents a connected network ("graph") of SpinBlock buildings.
 * Uses BFS to split/rebuild components when a building is removed.
 * Heavy calculations are triggered via [update] or [calculate].
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
            val hue = id * 45
            val c = Color()
            c.fromHsv(hue.toFloat(), 1f, 1f)
            c.a = 0.3f
            return c
        }

        /**
         * Finds all distinct SpinGraphs among neighboring SpinBuilds, which are
         * connectable (bidirectional canConnect(...) returns true).
         */
        fun findNearbyGraphs(building: SpinBlock.SpinBuild): List<SpinGraph> {
            val blockSelf = building.block as? SpinBlock ?: return emptyList()
            val validNeighbors = building.proximity().select { neighbor ->
                val neighborBuild = neighbor as? SpinBlock.SpinBuild ?: return@select false
                val neighborBlock = neighborBuild.block as? SpinBlock ?: return@select false
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
     * Call this method periodically (e.g., from building.updateTile()) to recalculate if the graph is dirty.
     */
    fun update() {
        if (dirty) {
            calculate()
            dirty = false
        }
    }

    /**
     * Adds the specified building to this graph if not already present.
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
     * Merges another graph into this one; clears the other graph afterward.
     */
    fun merge(other: SpinGraph) {
        if (this == other) return
        other.all.forEach { building ->
            this.add(building)
            (building as? SpinBlock.SpinBuild)?.module?.graph = this
        }
        other.resetGraphData()
        other.markDirty()
        markDirty()
    }

    /**
     * Removes a building from this graph.
     * If removal splits the graph into multiple components,
     * brand-new SpinGraphs are created for each component.
     */
    fun remove(building: Building) {
        all.remove(building)
        producers.remove(building)
        consumers.remove(building)
        if (all.isEmpty) {
            resetGraphData()
            return
        }
        val components = findComponents(building)
        if (components.size == 1) {
            rewriteFrom(components[0])
        } else {
            resetGraphData()
            components.forEach { comp ->
                val newGraph = SpinGraph().apply { resetGraphData() }
                comp.forEach { b ->
                    newGraph.add(b)
                    (b as? SpinBlock.SpinBuild)?.module?.graph = newGraph
                }
                newGraph.markDirty()
            }
        }
        markDirty()
    }

    /**
     * Finds connected components using BFS, ignoring the removed building if provided.
     */
    private fun findComponents(removed: Building? = null): List<List<Building>> {
        val visited = mutableSetOf<Building>()
        val components = mutableListOf<List<Building>>()
        all.forEach { b ->
            if (!visited.contains(b)) {
                val comp = exploreComponentBFS(b, visited, removed)
                if (comp.isNotEmpty()) components.add(comp)
            }
        }
        return components
    }

    /**
     * Performs BFS from the starting building, adding connected buildings into a list.
     */
    private fun exploreComponentBFS(start: Building, visited: MutableSet<Building>, removed: Building? = null): List<Building> {
        val component = mutableListOf<Building>()
        val queue = Queue<Building>()
        queue.addLast(start)
        while (!queue.isEmpty) {
            val current = queue.removeFirst()
            if (visited.add(current)) {
                component.add(current)
                buildingConnected(current).forEach { neighbor ->
                    if (neighbor != removed && neighbor !in visited) {
                        queue.addLast(neighbor)
                    }
                }
            }
        }
        return component
    }

    /**
     * Returns all valid SpinBlock neighbors of [building] within this graph (bidirectional canConnect).
     */
    private fun buildingConnected(building: Building): Seq<Building> {
        val result = Seq<Building>()
        building.proximity().forEach { c ->
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
     * Resets internal graph data without notifying others.
     */
    private fun resetGraphData() {
        all.clear()
        producers.clear()
        consumers.clear()
        spins = 0f
        maxStress = 0f
        currentStress = 0f
    }

    /**
     * Recalculates spin/stress usage for the graph.
     */
    fun calculate() {
        spins = 0f
        maxStress = 0f
        currentStress = 0f
        all.forEach { building ->
            val block = building.block
            if (block is SpinBlock) {
                spins += block.getGeneratedSpins(building.tile)
                maxStress += block.getMaxStress(building.tile)
                currentStress += block.getGeneratedStress(building.tile)
            }
        }
        if (currentStress > maxStress) spins = 0f
    }

    /**
     * Reassigns this graph based on a single connected component.
     */
    private fun rewriteFrom(component: List<Building>) {
        resetGraphData()
        component.forEach { b ->
            add(b)
            (b as? SpinBlock.SpinBuild)?.module?.graph = this
        }
        markDirty()
    }

    override fun toString(): String {
        return "SpinGraph(id=$graphID, spins=$spins, maxStress=$maxStress, " +
                "currentStress=$currentStress, producers=${producers.size}, " +
                "consumers=${consumers.size}, all=${all.size})"
    }
}