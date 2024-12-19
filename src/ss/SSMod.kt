package ss

import arc.util.*
import mindustry.mod.*
import ss.content.*

class SSMod : Mod(){

    init{
        Log.info("Loaded Spins & Stress constructor.")
    }

    override fun init(){
        Log.info("Loaded Spins & Stress init.")
    }

    override fun loadContent(){
        Log.info("Loading some Spins & Stress content.")
        SSBlocks().load()
    }
}
