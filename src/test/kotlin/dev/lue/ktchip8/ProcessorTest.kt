package dev.lue.ktchip8

import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ProcessorTest {
    @Test
    fun foo() {
        val processor = Processor()
        processor.debug()
    }
}