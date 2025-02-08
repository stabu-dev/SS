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
    lateinit var shaft: Block
    lateinit var test1: Block
    lateinit var test2: Block
    lateinit var test3: Block
    lateinit var test4: Block
    lateinit var test5: Block

    fun load() {
        spinSource = SpinSource("spin-source", 1f, 1f).apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
            defaultProducedSpins = 100f
            defaultStressCapacity = 100f
        }

        spinVoid = SpinVoid("spin-void", 1f).apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
            defaultGeneratedStress = 100f
        }

        shaft = Shaft("shaft").apply {
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

        test1 = Gearbox("test").apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
            size = 2
            connectionIndexes = intArrayOf(
                1, 1,
                1, 1,
                1, 1,
                1, 1
            )
        }

        test2 = Gearbox("test2").apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
            size = 3
            connectionIndexes = intArrayOf(
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
                1, 1, 1
            )
        }

        test3 = Gearbox("test3").apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
            size = 4
            connectionIndexes = intArrayOf(
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 1, 1, 1
            )
        }

        test4 = Gearbox("test4").apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
            size = 5
            connectionIndexes = intArrayOf(
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1
            )
        }

        test5 = Gearbox("test5").apply {
            requirements(
                Category.power,
                BuildVisibility.shown,
                empty
            )
            size = 6
            connectionIndexes = intArrayOf(
                1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1
            )
        }
    }
}