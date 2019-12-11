package dev.lue.ktchip8

import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class ProcessorTest {
    val ROM = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/demos/Maze (alt) [David Winter, 199x].ch8"
    val BC_Chip8Test_ROM = "/Users/lue/devel/kotlin/ktchip8/roms/BC_test.ch8"
    @Test
    fun BC_Chip8Test() {
        val _program = File(BC_Chip8Test_ROM).readBytes()
        val program = _program.map { it.toInt() and 0xff }.toIntArray()

        val processor = Processor()
        processor.loadProgram(program)
        processor.run()
    }
}