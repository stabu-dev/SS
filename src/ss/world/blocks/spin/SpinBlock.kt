package ss.world.blocks.spin

import arc.Core.*
import arc.graphics.g2d.*
import mindustry.graphics.*
import mindustry.ui.*
import mindustry.world.*
import ss.world.modules.*

abstract class SpinBlock(name: String) : Block(name) {

    var produceSpin: Boolean = false
    var consumeSpin: Boolean = false
    var spins: Float = 0f
    var stress: Float = 0f
    var maxStress: Float = 0f

    init {
        update = true
    }

    open fun isProducer(): Boolean = produceSpin
    open fun isConsumer(): Boolean = consumeSpin
    open fun getGeneratedSpins(tile: Tile): Float = spins
    open fun getGeneratedStress(tile: Tile): Float = stress
    open fun getMaxStress(tile: Tile): Float = maxStress

    override fun setBars() {
        super.setBars()
        addBar<SpinBuild>("spins") {
            Bar(
                { bundle.format("bar.spins", it.module.graph?.spins) },
                { Pal.ammo },
                { it.module.graph?.spins?.div(200) ?: 0f }
            )
        }

        addBar<SpinBuild>("stress") {
            Bar(
                { bundle.format("bar.stress", it.module.graph?.currentStress, it.module.graph?.maxStress) },
                { Pal.accent },
                { (it.module.graph?.currentStress?.div(it.module.graph?.maxStress ?: 1f)) ?: 0f }
            )
        }
    }

    abstract inner class SpinBuild : mindustry.gen.Building() {
        val module = SpinModule()

        override fun placed() {
            super.placed()
            module.graph = findOrCreateGraph()
            module.graph?.add(this)
            module.graph?.calculate()
            module.syncWithGraph()
        }

        override fun remove() {
            module.graph?.remove(this)
            super.remove()
        }

        private fun findOrCreateGraph(): SpinGraph {
            val nearbyGraphs = proximity().mapNotNull { (it as? SpinBuild)?.module?.graph }
            return if (nearbyGraphs.isNotEmpty()) {
                val mainGraph = nearbyGraphs.first()
                nearbyGraphs.drop(1).forEach { mainGraph.merge(it) }
                mainGraph
            } else {
                SpinGraph()
            }
        }

        override fun draw() {
            super.draw()
            module.graph?.let { g ->
                Draw.z(Layer.blockOver)
                Draw.color(g.color)
                Draw.rect(region, x, y)
                Draw.color()
            }
        }

        override fun write(write: arc.util.io.Writes) {
            super.write(write)
            module.write(write)
        }

        override fun read(read: arc.util.io.Reads, revision: Byte) {
            super.read(read, revision)
            module.read(read)
        }
    }
}
