package ss.world.blocks.spin

import arc.Core.*
import arc.graphics.g2d.*
import mindustry.graphics.*
import mindustry.ui.*
import mindustry.world.*
import ss.world.modules.*
import mindustry.gen.*
import kotlin.math.*
/**
 * Base class for blocks that produce/consume "spin".
 * Allows for configuration of connection indexes for multi-tile blocks.
 */
abstract class SpinBlock(name: String) : Block(name) {

    var produceSpin: Boolean = false
    var consumeSpin: Boolean = false

    var spins: Float = 0f
    var stress: Float = 0f
    var maxStress: Float = 0f

    /**
     * Determines which side positions are open for connection; for a block of size N,
     * conventional indexing: 0..N-1 => bottom, N..2N-1 => right, 2N..3N-1 => top, 3N..4N-1 => left.
     * For example, for size=1 it will be 4 elements; for size=2 - 8 elements.
     */
    var connectionIndexes: IntArray? = intArrayOf(1, 1, 1, 1)

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

    /**
     * Determines if a building [from] can connect to a building [to] based on connectionIndexes.
     */
    open fun canConnect(from: Building, to: Building): Boolean {
        val indexes = connectionIndexes
        if (indexes == null || indexes.isEmpty()) return true
        val sideIdx = getSideIndex(from, to)
        if (sideIdx < 0 || sideIdx >= indexes.size) return false
        return (indexes[sideIdx] != 0)
    }

    /**
     * Computes the connection index for the neighbor building [to] relative to building [from],
     * taking into account multi-tile size and rotation.
     */
    private fun getSideIndex(from: Building, to: Building): Int {
        val size = from.block.size

        val fromCx = from.tile.x + if (size % 2 == 0) 0.5f else 0f
        val fromCy = from.tile.y + if (size % 2 == 0) 0.5f else 0f

        val toSize = to.block.size
        val toCx = to.tile.x + if (toSize % 2 == 0) 0.5f else 0f
        val toCy = to.tile.y + if (toSize % 2 == 0) 0.5f else 0f

        val dx = toCx - fromCx
        val dy = toCy - fromCy

        val (rdx, rdy) = when (from.rotation and 3) {
            0 -> dx to dy
            1 -> -dy to dx
            2 -> -dx to -dy
            3 -> dy to -dx
            else -> dx to dy
        }

        val side = if (abs(rdx) >= abs(rdy)) {
            if (rdx >= 0f) 1 else 3
        } else {
            if (rdy >= 0f) 2 else 0
        }

        // Compute normalized offset along the chosen side.
        // For horizontal sides (0,2) use rdx; for vertical (1,3) use rdy.
        val norm = when (side) {
            0, 2 -> (rdx / (size / 2f) + 1f) / 2f
            1, 3 -> (rdy / (size / 2f) + 1f) / 2f
            else -> 0f
        }
        val off = (norm * (size - 1)).roundToInt().coerceIn(0, size - 1)
        return side * size + off
    }

    /**
     * Represents a building instance of this SpinBlock.
     * Each building has a [SpinModule] that references its [SpinGraph].
     */
    abstract inner class SpinBuild : Building() {
        val module = SpinModule()
        private var lastRotation: Int = -1

        override fun placed() {
            super.placed()
            lastRotation = rotation
            rejoinGraph()
        }

        override fun updateTile() {
            super.updateTile()

            if (rotation != lastRotation) {
                lastRotation = rotation
                rejoinGraph()
            }

            module.graph?.update()
        }

        /**
         * Ensures this building is removed from the old graph and added to a new or existing one,
         * according to actual connectivity checks in [SpinGraph].
         */
        private fun rejoinGraph() {
            module.graph?.remove(this)
            module.graph = findOrCreateGraph()
            module.graph?.add(this)
            module.graph?.markDirty()
        }

        override fun remove() {
            module.graph?.remove(this)
            super.remove()
        }

        /**
         * Finds all existing graphs among valid neighbors (with canConnect(...) == true),
         * merges them if found, or creates a brand new SpinGraph if none is found.
         */
        private fun findOrCreateGraph(): SpinGraph {
            val neighbors = SpinGraph.findNearbyGraphs(this)
            if (neighbors.isNotEmpty()) {
                val main = neighbors.first()
                neighbors.drop(1).forEach { main.merge(it) }
                return main
            } else {
                // No connectable neighbors => new graph
                return SpinGraph()
            }
        }

        override fun draw() {
            super.draw()
            // Debug overlay showing the graph's color
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