package ss.world.blocks.spin

import mindustry.world.Tile

class Shaft(name: String) : SpinBlock(name) {

    override fun isProducer(): Boolean = false
    override fun isConsumer(): Boolean = false

    override fun getGeneratedSpins(tile: Tile): Float = 0f
    override fun getGeneratedStress(tile: Tile): Float = 0f
    override fun getMaxStress(tile: Tile): Float = 0f

    inner class ShaftBuild : SpinBuild()
}