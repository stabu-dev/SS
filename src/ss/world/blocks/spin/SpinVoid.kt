package ss.world.blocks.spin

import arc.scene.ui.layout.*
import mindustry.world.*

class SpinVoid(name: String, var defaultGeneratedStress: Float) : SpinBlock(name) {

    init {
        configurable = true
        update = true
        produceSpin = false
        consumeSpin = true
        spins = 0f
    }

    override fun getGeneratedStress(tile: Tile): Float {
        val build = tile.build
        return if (build is SimpleConsumerBuild) build.generatedStress else defaultGeneratedStress
    }

    inner class SimpleConsumerBuild : SpinBuild() {
        var generatedStress: Float = defaultGeneratedStress

        override fun buildConfiguration(table: Table) {
            super.buildConfiguration(table)

            table.add("Generated Stress:").row()
            table.slider(0f, 200f, 5f, generatedStress) { value ->
                configure("stress|$value")
            }.row()
        }

        override fun configure(value: Any?) {
            if (value is String) {
                val parts = value.split("|")
                if (parts.size == 2) {
                    val param = parts[0]
                    val newValue = parts[1].toFloatOrNull() ?: return

                    when (param) {
                        "stress" -> {
                            generatedStress = newValue
                            module.graph?.calculate()
                        }
                    }
                }
            }
        }
    }
}
