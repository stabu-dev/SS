package ss.world.blocks.spin

import arc.Core.*
import arc.graphics.g2d.*
import mindustry.graphics.*
import mindustry.ui.*
import mindustry.world.*
import ss.world.modules.*
import mindustry.gen.*
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
     * indexes [0..N-1] => down, [N..2N-1] => right, [2N..3N-1] => up, [3N..4N-1] => left.
     * 0 blocks a position, 1 allows it.
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
     * Determines if a building [from] can connect to a building [to], based on [connectionIndexes].
     * Returns true if allowed, false otherwise.
     */

    //TODO: works bad with blocks which size>1
    open fun canConnect(from: Building, to: Building): Boolean {
        val indexes = connectionIndexes
        if (indexes == null || indexes.isEmpty()) {
            return true
        }
        val sideIdx = getSideIndex(from, to)
        if (sideIdx < 0 || sideIdx >= indexes.size) return false
        return (indexes[sideIdx] != 0)
    }

    /**
     * Transforms global tile (gx, gy) into local (lx, ly) coordinates relative to
     * the top-left corner of a multi-tile block with size [size], and with rotation [rot].
     *
     * For rotation=3, invert dx/dy consistently to keep the same pattern.
     */
    fun globalToLocal(gx: Int, gy: Int, x0: Int, y0: Int, size: Int, rot: Int): Pair<Int, Int> {
        val dx = gx - x0
        val dy = gy - y0

        return when (rot and 3) {
            0 -> dx to dy
            1 -> {
                val lx = size - 1 - dy
                lx to dx
            }
            2 -> {
                val lx = size - 1 - dx
                val ly = size - 1 - dy
                lx to ly
            }
            3 -> {
                // for rotation=3: (dx, dy) -> (dy, size-1-dx) or a variant
                val ly = size - 1 - dx
                dy to ly
            }
            else -> dx to dy
        }
    }

    /**
     * Returns an integer index representing which side (and offset along that side) [to] is
     * relative to [from], considering [from]'s rotation and multi-tile size.
     *
     * If it doesn't match any recognized side, returns -1.
     */
    protected fun getSideIndex(from: Building, to: Building): Int {
        val size = from.block.size
        val rot = from.rotation

        val x0 = from.tile.x
        val y0 = from.tile.y

        // Convert global coords of [to] to local coords (lx, ly).
        val (lx, ly) = globalToLocal(
            to.tile.x.toInt(),
            to.tile.y.toInt(),
            x0.toInt(),
            y0.toInt(),
            size,
            rot
        )

        // The next conditions assume the top-left of the block is (0,0) locally
        // and that the block extends to (size-1,size-1). So y = size means "below" the block, etc.

        // side 0: "down" edge if ly == size
        if (ly == size && lx in 0 until size) {
            return 0 * size + lx
        }
        // side 1: "right" edge if lx == size
        if (lx == size && ly in 0 until size) {
            return 1 * size + ly
        }
        // side 2: "up" edge if ly == -1
        if (ly == -1 && lx in 0 until size) {
            val offset = (size - 1 - lx)
            return 2 * size + offset
        }
        // side 3: "left" edge if lx == -1
        if (lx == -1 && ly in 0 until size) {
            val offset = (size - 1 - ly)
            return 3 * size + offset
        }

        return -1
    }

    /**
     * Helper function to get the local position of a side index, for debug or advanced usage.
     */
    fun getConnectSidePos(index: Int, size: Int, rotation: Int): arc.math.geom.Point2 {
        var side = index / size
        side = (side + rotation) % 4

        val tangent = d4((side + 1) % 4)
        var originX = 0
        var originY = 0

        if (size > 1) {
            originX += size / 2
            originY += size / 2
            originY -= size - 1
            if (side > 0) {
                for (i in 1..side) {
                    originX += d4x(i) * (size - 1)
                    originY += d4y(i) * (size - 1)
                }
            }
            originX += tangent.x * (index % size)
            originY += tangent.y * (index % size)
        }
        return arc.math.geom.Point2(originX + d4x(side), originY + d4y(side))
    }

    private fun d4(i: Int): arc.math.geom.Point2 {
        return when (i % 4) {
            0 -> arc.math.geom.Point2(0, -1)
            1 -> arc.math.geom.Point2(1, 0)
            2 -> arc.math.geom.Point2(0, 1)
            3 -> arc.math.geom.Point2(-1, 0)
            else -> arc.math.geom.Point2(0, 0)
        }
    }
    private fun d4x(i: Int) = d4(i).x
    private fun d4y(i: Int) = d4(i).y

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

            // If rotation changed, to re-join or create a graph
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
            // On removal, notify the graph so it can recalc or split as needed.
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