package ss.content

import arc.*
import arc.assets.loaders.*
import arc.audio.*
import arc.func.Cons
import mindustry.*

object SSSounds {
    var spin = Sound()

    fun load() {
        spin = loadSound("spin")
    }

    private fun loadSound(soundName: String): Sound {
        return if (!Vars.headless) {
            val name = "sounds/$soundName"
            val path = if (Vars.tree.get("$name.ogg").exists()) "$name.ogg" else "$name.mp3"
            val sound = Sound()
            val desc = Core.assets.load(path, Sound::class.java, SoundLoader.SoundParameter(sound))
            desc.errored = Cons { t: Throwable -> t.printStackTrace() }
            sound
        } else {
            Sound()
        }
    }
}