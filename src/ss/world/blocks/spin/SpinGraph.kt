package ss.world.blocks.spin

import arc.graphics.*
import arc.struct.*
import mindustry.gen.*

class SpinGraph {

    var spins: Float = 0f
    var maxStress: Float = 0f
    var currentStress: Float = 0f

    val color: Color

    private val producers: Seq<Building> = Seq()
    private val consumers: Seq<Building> = Seq()
    val all: Seq<Building> = Seq()

    var graphID: Int = lastGraphID++

    companion object {
        private var lastGraphID: Int = 0
        val allGraphs = mutableListOf<SpinGraph>()

        private fun colorFromID(id: Int): Color {
            val hue = (id * 45) % 360
            val c = Color()
            c.fromHsv(hue.toFloat(), 1f, 1f)
            c.a = 0.3f
            return c
        }
    }

    init {
        color = colorFromID(graphID)
        allGraphs.add(this)
    }

    fun add(building: Building?) {
        if (building == null || all.contains(building)) return
        all.add(building)
        val block = building.block
        if (block is SpinBlock) {
            if (block.isProducer()) producers.add(building)
            if (block.isConsumer()) consumers.add(building)
        }
        (building as? SpinBlock.SpinBuild)?.module?.graph = this
    }

    fun merge(other: SpinGraph) {
        if (this == other) return

        for (building in other.all) {
            this.add(building)
            (building as? SpinBlock.SpinBuild)?.module?.graph = this
        }

        other.clear()
        this.calculate()
    }

    fun remove(building: Building) {
        all.remove(building)
        producers.remove(building)
        consumers.remove(building)

        if (all.isEmpty) {
            clear()
            return
        }

        val components = findComponents(building)

        if (components.size == 1) {
            rewriteFrom(components[0])
        } else {
            allGraphs.remove(this)
            clearInternal()
            for (comp in components) {
                val newGraph = SpinGraph()
                newGraph.clearInternal()
                for (b in comp) {
                    newGraph.add(b)
                    (b as? SpinBlock.SpinBuild)?.module?.graph = newGraph
                }
                newGraph.calculate()
            }
        }
    }

    private fun findComponents(removed: Building): List<List<Building>> {
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

    private fun exploreComponent(start: Building, visited: MutableSet<Building>, removed: Building): List<Building> {
        val queue = Queue<Building>()
        val component = mutableListOf<Building>()

        queue.addLast(start)
        while (queue.size > 0) {
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

    private fun buildingConnected(building: Building): Seq<Building> {
        val result = Seq<Building>()
        for (c in building.proximity()) {
            if (c.block is SpinBlock && all.contains(c)) {
                result.add(c)
            }
        }
        return result
    }

    private fun clearInternal() {
        all.clear()
        producers.clear()
        consumers.clear()
        spins = 0f
        maxStress = 0f
        currentStress = 0f
    }

    fun clear() {
        allGraphs.remove(this)
        clearInternal()
    }

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

        if (currentStress > maxStress) {
            spins = 0f
        }

        for (building in all) {
            (building as? SpinBlock.SpinBuild)?.module?.syncWithGraph()
        }
    }

    private fun rewriteFrom(component: List<Building>) {
        clearInternal()
        for (b in component) {
            add(b)
            (b as? SpinBlock.SpinBuild)?.module?.graph = this
        }
        calculate()
    }

    override fun toString(): String {
        return "SpinGraph(" +
                "id=$graphID, spins=$spins, maxStress=$maxStress, " +
                "currentStress=$currentStress, producers=${producers.size}, " +
                "consumers=${consumers.size}, all=${all.size})"
    }
}
