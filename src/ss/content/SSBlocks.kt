package ss.content

import mindustry.type.*
import mindustry.type.ItemStack.*
import mindustry.world.*
import mindustry.world.meta.*
import ss.world.blocks.spin.*

class SSBlocks {
    lateinit var spinSource: Block
    lateinit var spinVoid: Block
    lateinit var gearbox: Block

    fun load() {
        spinSource = SpinSource("spin-source", 1f, 1f).apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
        }

        spinVoid = SpinVoid("spin-void", 1f).apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
        }

        gearbox = Gearbox("gearbox").apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
        }
    }
}