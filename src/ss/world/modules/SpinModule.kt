package ss.world.modules

import arc.util.io.*
import mindustry.Vars.*
import mindustry.world.modules.*
import ss.world.blocks.spin.*

class SpinModule: BlockModule() {
    var graph: SpinGraph? = null

    override fun write(write: Writes) {
        if (graph != null) {
            write.bool(true)
            write.i(graph?.graphID ?: -1)
            write.i(graph?.all?.size ?: -1)

            for (block in graph?.all ?: emptyList()) {
                write.i(block.tile.x.toInt())
                write.i(block.tile.y.toInt())
            }
        } else {
            write.bool(false)
        }
    }

    override fun read(read: Reads) {
        if (read.bool()) {
            graph = SpinGraph()
            graph?.graphID = read.i()

            val blockCount = read.i()
            for (i in 0 until blockCount) {
                val x = read.i()
                val y = read.i()
                val tile = world.tile(x, y)

                val building = tile.build
                if (building is SpinBlock.SpinBuild) {
                    graph?.add(building)
                    building.module.graph = this.graph
                }
            }
            graph?.calculate()
        } else {
            graph = null
        }
    }

    fun syncWithGraph() {
        graph?.let {
            it.spins
            it.currentStress
        }
    }
}