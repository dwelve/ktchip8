package dev.lue.ktchip8

import java.io.File

class Program {
    fun loadFromFile(path: String) {
        val _program = File(path).readBytes()
        val program = _program.map { it.toInt() and 0xff }.toIntArray()
        val memory = IntArray(0)
    }
}