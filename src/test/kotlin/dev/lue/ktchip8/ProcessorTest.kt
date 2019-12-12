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
    val ROM2 = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/demos/Maze [David Winter, 199x].ch8"
    val BC_Chip8Test_ROM = "/Users/lue/devel/kotlin/ktchip8/roms/BC_test.ch8"
    val TEST_ROM = "/Users/lue/devel/kotlin/ktchip8/roms/test_opcode.ch8"
    val TEST_ROM2 = "/Users/lue/devel/kotlin/ktchip8/roms/c8_test.c8"
    val PARTICLE_DEMO = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/demos/Particle Demo [zeroZshadow, 2008].ch8"
    val SIERPINSKI_DEMO = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/demos/Sierpinski [Sergey Naydenov, 2010].ch8"
    val IBM_LOGO = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/programs/IBM Logo.ch8"
    val CHIP8_LOGO = "/Users/lue/devel/kotlin/chip8-resources/chip8/roms/programs/Chip8 emulator Logo [Garstyciuks].ch8"

    @Test
    fun processorPassesTestRom() {
        val _program = File(TEST_ROM).readBytes()
        val program = _program.map { it.toInt() and 0xff }.toIntArray()

        val processor = Processor()
        processor.loadProgram(program)
        processor.run()
    }
}