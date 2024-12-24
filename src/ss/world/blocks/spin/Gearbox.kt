package ss.world.blocks.spin

class Gearbox(name: String) : SpinBlock(name) {

    init {
        produceSpin = false
        consumeSpin = false
    }

    inner class GearboxBuild : SpinBuild()
}