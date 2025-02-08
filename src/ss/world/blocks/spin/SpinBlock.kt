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

        val offset = if (size % 2 == 0) 0.5f else 0f
        val centerX = from.tile.x + offset
        val centerY = from.tile.y + offset

        val toSize = to.block.size
        val toOffset = if (toSize % 2 == 0) 0.5f else 0f
        val toCenterX = to.tile.x + toOffset
        val toCenterY = to.tile.y + toOffset

        val dx = toCenterX - centerX
        val dy = toCenterY - centerY

        // TODO: perhaps it is overcomplicated
        val rotRad = Math.toRadians((from.rotation * 90).toDouble())
        val rdx = (dx * cos(-rotRad) - dy * sin(-rotRad)).toFloat()
        val rdy = (dx * sin(-rotRad) + dy * cos(-rotRad)).toFloat()

        var ang = Math.toDegrees(atan2(rdy.toDouble(), rdx.toDouble()))
        if (ang < 0) ang += 360

        val side = when {
            ang >= 225 && ang < 315 -> 0 // bottom
            ang >= 315 || ang < 45 -> 1 // right
            ang >= 45 && ang < 135 -> 2 // top
            ang >= 135 && ang < 225 -> 3 // left
            else -> -1
        }

        // Calculate the offset along the side.
        // For bottom and top, using the horizontal component (rdx),
        // for right and left - the vertical component (rdy).
        val off: Int = when (side) {
            0, 2 -> {
                // Normalize rdx from the range [-1,1] to [0, size-1]
                (((rdx + 1f) / 2f) * (size - 1)).roundToInt().coerceIn(0, size - 1)
            }
            1, 3 -> {
                (((rdy + 1f) / 2f) * (size - 1)).roundToInt().coerceIn(0, size - 1)
            }
            else -> 0
        }

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