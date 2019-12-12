package dev.lue.ktchip8

import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class DisassemblerTest {
    //val ROM = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/demos/Maze [David Winter, 199x].ch8"
    val ROM = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/demos/Maze (alt) [David Winter, 199x].ch8"
    //val ROM = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/games/UFO [Lutz V, 1992].ch8"
    val TEST_ROM2 = "/Users/lue/devel/kotlin/ktchip8/roms/c8_test.c8"

    @Test
    fun testDisassembler() {
        val _program = File(TEST_ROM2).readBytes()
        val program = _program.map { it.toInt() and 0xff }.toIntArray()
        //println("${_program.contentToString()}")
        println("${program.contentToString()}")
        val dsa = Disassembler()
        dsa.disassemble(program)

    }
}