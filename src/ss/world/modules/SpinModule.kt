package ss.world.modules

import arc.struct.IntSeq
import arc.util.io.*
import ss.world.blocks.spin.SpinGraph

class SpinModule {
    var spins = 0f
    var stress = 0f
    var graph: SpinGraph? = null
    var links = IntSeq()

    fun write(write: Writes) {
        write.s(links.size)
        for (i in 0 until links.size) {
            write.i(links[i])
        }
        write.f(spins)
        write.f(stress)
    }

    fun read(read: Reads) {
        links.clear()
        val amount = read.s().toInt()
        for (i in 0 until amount) {
            links.add(read.i())
        }
        spins = read.f()
        stress = read.f()
        if (spins.isNaN() || spins.isInfinite()) spins = 0f
        if (stress.isNaN() || stress.isInfinite()) stress = 0f
    }

    fun syncWithGraph() {
        graph?.let {
            spins = it.spins
            stress = it.currentStress
        }
    }
}