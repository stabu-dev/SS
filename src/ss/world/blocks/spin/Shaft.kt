package ss.world.blocks.spin

class Shaft(name: String) : SpinBlock(name) {
    init {
        produceSpin = false
        consumeSpin = false
        rotate = true
        connectionIndexes = intArrayOf(0, 1, 0, 1)
    }

    inner class ShaftBuild : SpinBuild()
}