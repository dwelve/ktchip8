package dev.lue.ktchip8


class Disassembler {
    val ops = mapOf(
        "00E0" to "CLS",
        "00EE" to "RET",
        "1NNN" to "JP n",
        "2NNN" to "CALL n",
        "3XNN" to "SE Vx n",
        "4XNN" to "SNE Vx n",
        "5XY0" to "SE Vx Vy",
        "6XNN" to "LD Vx n",
        "7XNN" to "ADD Vx n",
        "8XY0" to "LD Vx Vy",
        "8XY1" to "OR Vx Vy",
        "8XY2" to "AND Vx Vy",
        "8XY3" to "XOR Vx Vy",
        "8XY4" to "ADD Vx Vy",
        "8XY5" to "SUB Vx Vy",
        "8XY6" to "SHR Vx",
        "8XY7" to "SUBN Vx Vy",
        "8XYE" to "SHL Vx",
        "9XY0" to "SNE Vx Vy",
        "ANNN" to "LD I n",
        "BNNN" to "JP V0 n",
        "CXNN" to "RND Vx n",
        "DXYN" to "DRW Vx Vy n",
        "EX9E" to "SKP Vx",
        "EXA1" to "SKNP Vx",
        "FX07" to "LD Vx DT",
        "FX0A" to "LD Vx K",
        "FX15" to "LD DT Vx",
        "FX18" to "LD ST Vx",
        "FX1E" to "ADD I Vx",
        "FX29" to "LD F Vx",
        "FX33" to "LD B Vx",
        "FX55" to "LD I Vx",
        "FX65" to "LD Vx I"
    )

    fun formatInstruction(opcode: Opcode, format: String): String {
        var line = ops[format]!!
        val N = when (format.count {it == 'N'}) {
            1 -> opcode.d
            2 -> opcode.byte
            3 -> opcode.address
            else -> null
        }
        if (format[1] == 'X') {
            line = line.replace("x", opcode.x.toString(16).toUpperCase())
        }
        if (format[2] == 'Y') {
            line = line.replace("y", opcode.y.toString(16).toUpperCase())
        }
        // replace literal last so we can use 0x prefix
        if (N != null) {
            line = line.replace("n", "0x${N.toString(16).toUpperCase()}")
        }
        return line
    }

    fun disassemble(program: IntArray) {
        for ( instr in  program.asList().chunked(2).withIndex()) {
            if (instr.value.size != 2) {
                continue
            }
            val msb = instr.value[0]
            val lsb = instr.value[1]
            val pc = instr.index * 2
            val _opcode = (msb shl 8) or lsb
            val opcode = Opcode.create(_opcode)

            val format = when (opcode.a) {
                0 -> {
                    when (opcode.int) {
                        0x00E0 -> "00E0"
                        0x00EE -> "00EE"
                        else -> null //error("Invalid opcode: ${opcode.hex}")
                    }
                }
                1 -> "1NNN"
                2 -> "2NNN"
                3 -> "3XNN"
                4 -> "4XNN"
                5 -> "5XY0"
                6 -> "6XNN"
                7 -> "7XNN"
                8 -> {
                    when (opcode.d) {
                        0 -> "8XY0"
                        1 -> "8XY1"
                        2 -> "8XY2"
                        3 -> "8XY3"
                        4 -> "8XY4"
                        5 -> "8XY5"
                        6 -> "8XY6"
                        7 -> "8XY7"
                        0xE -> "8XYE"
                        else -> null //error("Invalid opcode: ${opcode.hex}")
                    }
                }
                9 -> "9XY0"
                0xA -> "ANNN"
                0xB -> "BNNN"
                0xC -> "CXNN"
                0xD -> "DXYN"
                0xE -> when (opcode.byte) {
                    0x9E -> "EX9E"
                    0xA1 -> "EXA1"
                    else -> null //error("Invalid opcode: ${opcode.hex}")
                }
                0xF -> when (opcode.byte) {
                    0x0A -> "FX0A"
                    0x15 -> "FX15"
                    0x18 -> "FX18"
                    0x1E -> "FX1E"
                    0x29 -> "FX29"
                    0x33 -> "FX33"
                    0x55 -> "FX55"
                    0x65 -> "FX65"
                    else -> null //error("Invalid opcode: ${opcode.hex}")
                }
                else -> null//error("Invalid opcode: ${opcode.hex}")
            }
            val pc2 = pc + 0x200
            if (format != null) {
                format.let {
                    val line = formatInstruction(opcode, it)
                    println("${pc2.toString(16).padStart(4, '0')}:\t${opcode.hex}\t$line\t\t$it")
                }
            } else {
                println("${pc2.toString(16).padStart(4, '0')}:\t${opcode.hex}\tDATA")
            }
        }
    }
}