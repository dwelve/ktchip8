package dev.lue.ktchip8

data class Opcode(val a: Int, val b: Int, val c: Int, val d: Int) {
    companion object Factory {
        fun create(opcode: Int): Opcode {
            val a = (opcode and 0xF000) shr 12
            val b = (opcode and 0x0F00) shr 8
            val c = (opcode and 0x00F0) shr 4
            val d = (opcode and 0x000F)
            return Opcode(a, b, c, d)
        }
    }

    val int get() = (this.a shl 12) or (this.b shl 8) or (this.c shl 4) or this.d
    val hex get() = this.int.toString(16).padStart(4, '0')

    val x get() = this.b
    /*fun getX(): Int {
        return this.b
    }*/

    val y get() = this.c
    /*fun getY(): Int {
        return this.c
    }*/

    val byte get() = (this.c shl 4) or this.d
    /*fun getByte(): Int {
        // Get lower byte
        return (this.c shl 4) or this.d
    }*/

    val address get() = (this.b shl 8) or (this.c shl 4) or this.d
    /*fun getAddress(): Int {
        // Get address by lower 12 bits
        return (this.b shl 8) or (this.c shl 4) or this.d
    }*/
}

typealias OpFunc = (Opcode) -> Unit

data class Decoding(val format: String, val assemblyTemplate: String, val callable: OpFunc)
data class DecodedInstruction(val decoding: Decoding, val opcode: Opcode) {
    fun execute() {
        this.decoding.callable(this.opcode)
    }

    fun formatInstruction(): String {
        var line = this.decoding.assemblyTemplate
        val format = this.decoding.format
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
}

@ExperimentalUnsignedTypes
class Processor {
    companion object {
        const val NUMBER_OF_REGISTERS = 16
        const val MEMORY_SIZE = 4096
        const val STACK_SIZE = 16
        const val F_REGISTER = 15
        const val PROGRAM_START = 0x200
        const val DISPLAY_WIDTH = 64
        const val DISPLAY_HEIGHT = 32
    }

    val decodings = listOf(
        Decoding("00E0", "CLS", ::_CLS),
        Decoding("00EE", "RET", ::_RET),
        Decoding("1NNN", "JP n", ::_JP_addr),
        Decoding("2NNN", "CALL n", ::_CALL_addr),
        Decoding("3XNN", "SE Vx n", ::_SE_Vx_byte),
        Decoding("4XNN", "SNE Vx n", ::_SNE_Vx_byte),
        Decoding("5XY0", "SE Vx Vy", ::_SE_Vx_Vy),
        Decoding("6XNN", "LD Vx n", ::_LD_Vx_byte),
        Decoding("7XNN", "ADD Vx n", ::_ADD_Vx_byte),
        Decoding("8XY0", "LD Vx Vy", ::_LD_Vx_Vy),
        Decoding("8XY1", "OR Vx Vy", ::_OR_Vx_Vy),
        Decoding("8XY2", "AND Vx Vy", ::_AND_Vx_Vy),
        Decoding("8XY3", "XOR Vx Vy", ::_XOR_Vx_Vy),
        Decoding("8XY4", "ADD Vx Vy", ::_ADD_Vx_Vy),
        Decoding("8XY5", "SUB Vx Vy", ::_SUB_Vx_Vy),
        Decoding("8XY6", "SHR Vx", ::_SHR_Vx),
        Decoding("8XY7", "SUBN Vx Vy", ::_SUBN_Vx_Vy),
        Decoding("8XYE", "SHL Vx", ::_SHL_Vx),
        Decoding("9XY0", "SNE Vx Vy", ::_SNE_Vx_Vy),
        Decoding("ANNN", "LD I n", ::_LD_I_addr),
        Decoding("BNNN", "JP V0 n", ::_JP_V0_addr),
        Decoding("CXNN", "RND Vx n", ::_RND_Vx_byte),
        Decoding("DXYN", "DRW Vx Vy n", ::_DRW_Vx_Vy_nibble),
        Decoding("EX9E", "SKP Vx", ::_SKP_Vx),
        Decoding("EXA1", "SKNP Vx", ::_SKNP_Vx),
        Decoding("FX07", "LD Vx DT", ::_LD_Vx_DT),
        Decoding("FX0A", "LD Vx K", ::_LD_Vx_K),
        Decoding("FX15", "LD DT Vx", ::_LD_DT_Vx),
        Decoding("FX18", "LD ST Vx", ::_LD_ST_Vx),
        Decoding("FX1E", "ADD I Vx", ::_ADD_I_Vx),
        Decoding("FX29", "LD F Vx", ::_LD_F_Vx),
        Decoding("FX33", "LD B Vx", ::_LD_B_Vx),
        Decoding("FX55", "LD I Vx", ::_LD_I_Vx),
        Decoding("FX65", "LD Vx I", ::_LD_Vx_I)
        )

    val ops = decodings.map { it.format to it }.toMap()

    //val memory : MutableList<UByte> = UByteArray(MEMORY_SIZE) { 0u }.toMutableList()
    //val registers: MutableList<UByte> = UByteArray(NUMBER_OF_REGISTERS) { 0u }.toMutableList()

    // it's really annoying to have to typecast UBytes or UShorts back and forth to Int,
    // so I'll just use Ints everywhere and make sure to properly handle overflow and underflow

    var program = IntArray(0)
    val memory = IntArray(MEMORY_SIZE) { 0 }
    val registers = IntArray(NUMBER_OF_REGISTERS) { 0 }
    var indexRegister: Int = 0
    val stack =  IntArray(STACK_SIZE) { 0 }
    var stackPointer: Int = 0       // easier to just make this an Int
    var delayTimer: Int = 0
    var soundTimer: Int = 0
    var programCounter: Int = 0

    val display: Array<IntArray> = (1..DISPLAY_HEIGHT).map { IntArray(DISPLAY_WIDTH) {0} }.toTypedArray()

    fun reset() {
        memory.fill(0)
        this.program.copyInto(memory, PROGRAM_START)
        registers.fill(0)
        indexRegister = 0
        stack.fill(0)
        stackPointer = 0
        delayTimer = 0
        soundTimer = 0
        programCounter = PROGRAM_START

        for (row in display) {
            row.fill(0)
        }
    }

    fun loadProgram(program: IntArray) {
        this.program = program
    }

    fun debug() {
        reset()
        println("registers of size ${registers.size}: $registers")
        println("memory.size: ${memory.size}")
    }

    inline fun readOpcode() : Int {
        return (memory[programCounter] shl 8) or memory[programCounter+1]
    }

    fun decodeInstruction(): DecodedInstruction {
        val opcode = Opcode.create(readOpcode())

        val format = when (opcode.a) {
            0 -> {
                when (opcode.int) {
                    0x00E0 -> "00E0"
                    0x00EE -> "00EE"
                    else -> error("Invalid opcode: ${opcode.hex}")
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
                    else -> error("Invalid opcode: ${opcode.hex}")
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
                else -> error("Invalid opcode: ${opcode.hex}")
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
                else -> error("Invalid opcode: ${opcode.hex}")
            }
            else -> error("Invalid opcode: ${opcode.hex}")
        }
        val decoding = ops[format] ?: error("Invalid format: $format")
        return DecodedInstruction(decoding, opcode)
    }

    fun step() {
        val decodedInstruction = decodeInstruction()

        val line = decodedInstruction.formatInstruction()

        println("${programCounter.toString(16).padStart(4, '0')}:\t${decodedInstruction.decoding.format}\t${decodedInstruction.decoding.assemblyTemplate}\t\t$line")
        decodedInstruction.execute()

    }

    fun run() {
        reset()
        var counter = 0
        while (true) {
            step()
            Thread.sleep(1)
            counter += 1
            if (counter > 2000) {
                break
            }
        }
        println("[")
        for (row in display) {
            println(row.contentToString() + ",")
        }
        println("]")
    }

    // instructions
    // http://devernay.free.fr/hacks/chip8/C8TECH10.HTM
    // https://en.wikipedia.org/wiki/CHIP-8

    inline fun advanceToNextInstruction() {
        programCounter += 2
    }
    inline fun advanceAndSkipToNextInstruction() {
        programCounter += 4
    }
    inline fun V(n: Int): Int = registers[n]

    fun _CLS(o: Opcode) {
        /* 00E0 - CLS
           Clear the display.
         */
        println("Cleared the display")
        advanceToNextInstruction()
    }

    fun _RET(o: Opcode) {
        /* Return from a subroutine.
           The interpreter sets the program counter to the address at the top of the stack, then subtracts 1 from the stack pointer
         */
        programCounter = stack[stackPointer]
        stackPointer -= 1
    }

    fun _JP_addr(o: Opcode) {
        /* 1nnn - JP addr
           Jump to location nnn.

           The interpreter sets the program counter to nnn.
         */
        programCounter = o.address
    }

    fun _CALL_addr(o: Opcode) {
        /* 2nnn - CALL addr
          Call subroutine at nnn.

          The interpreter increments the stack pointer, then puts the current PC on the top of the stack.
          The PC is then set to nnn.
         */
        stackPointer += 1
        stack[stackPointer] = programCounter
        programCounter = o.address
    }

    fun _SE_Vx_byte(o: Opcode) {
        /*
        3xkk - SE Vx, byte
        Skip next instruction if Vx = kk.

        The interpreter compares register Vx to kk, and if they are equal, increments the program counter by 2.
         */
        if (V(o.x) == o.byte) {
            advanceAndSkipToNextInstruction()
        } else {
            advanceToNextInstruction()
        }
    }

    fun _SNE_Vx_byte(o: Opcode) {
        if (V(o.x) != o.byte) {
            advanceAndSkipToNextInstruction()
        } else {
            advanceToNextInstruction()
        }
    }

    fun _SE_Vx_Vy(o: Opcode) {
        if (V(o.x) == V(o.y)) {
            advanceAndSkipToNextInstruction()
        } else {
            advanceToNextInstruction()
        }
    }

    fun _LD_Vx_byte(o: Opcode) {
        registers[o.x] = o.byte
        advanceToNextInstruction()
    }

    fun _ADD_Vx_byte(o: Opcode) {
        registers[o.x] = (V(o.x) + o.byte) % 255
        advanceToNextInstruction()
    }

    fun _LD_Vx_Vy(o: Opcode) {
        registers[o.x] = V(o.y)
        advanceToNextInstruction()
    }

    fun _OR_Vx_Vy(o: Opcode) {
        registers[o.x] = V(o.x) or V(o.y)
        advanceToNextInstruction()
    }

    fun _AND_Vx_Vy(o: Opcode) {
        registers[o.x] = V(o.x) and V(o.y)
        advanceToNextInstruction()
    }

    fun _XOR_Vx_Vy(o: Opcode) {
        registers[o.x] = V(o.x) xor V(o.y)
        advanceToNextInstruction()
    }

    fun _ADD_Vx_Vy(o: Opcode) {
        val foo = (V(o.x) + V(o.y))
        registers[o.x] = foo % 255
        registers[F_REGISTER] = if (foo > 255) 1 else 0     // overflow
        advanceToNextInstruction()
    }

    fun _SUB_Vx_Vy(o: Opcode) {
        val foo = (V(o.x) - V(o.y))
        registers[o.x] = foo % 255
        registers[F_REGISTER] = if (foo < 0) 0 else 1       // underflow
        advanceToNextInstruction()
    }

    fun _SHR_Vx(o: Opcode) {
        val vx = V(o.x)
        registers[F_REGISTER] = vx and 0x1
        registers[o.x] = vx shr 1
        advanceToNextInstruction()
    }

    fun _SUBN_Vx_Vy(o: Opcode) {
        val foo = (V(o.y) - V(o.x))
        registers[o.x] = foo % 255
        registers[F_REGISTER] = if (foo < 0) 0 else 1       // underflow
        advanceToNextInstruction()
    }

    fun _SHL_Vx(o: Opcode) {
        val vx = V(o.x)
        registers[F_REGISTER] = vx shr 15
        registers[o.x] = (vx shl 1) and 0xFF
        advanceToNextInstruction()
    }

    fun _SNE_Vx_Vy(o: Opcode) {
        if (V(o.x) != V(o.y)) {
            advanceAndSkipToNextInstruction()
        } else {
            advanceToNextInstruction()
        }
    }

    fun _LD_I_addr(o: Opcode) {
        indexRegister = o.address
        advanceToNextInstruction()
    }

    fun _JP_V0_addr(o: Opcode) {
        programCounter = V(0) + o.address
    }

    fun _RND_Vx_byte(o: Opcode) {
        registers[o.x] = (0..255).random() and o.byte
        advanceToNextInstruction()
    }

    fun _DRW_Vx_Vy_nibble(o: Opcode) {
        val didUnset = draw(V(o.x), V(o.y), o.d)
        registers[F_REGISTER] = if (didUnset) 1 else 0
        advanceToNextInstruction()
    }

    fun _SKP_Vx(o: Opcode) {
        // Skips the next instruction if the key stored in VX is pressed
        if (getKeyPress() == V(o.x)) {
            advanceAndSkipToNextInstruction()
        } else {
            advanceToNextInstruction()
        }
    }

    fun _SKNP_Vx(o: Opcode) {
        // Skips the next instruction if the key stored in VX is pressed
        if (getKeyPress() != V(o.x)) {
            advanceAndSkipToNextInstruction()
        } else {
            advanceToNextInstruction()
        }
    }

    fun _LD_Vx_DT(o: Opcode) {
        registers[o.x] = delayTimer
        advanceToNextInstruction()
    }

    fun _LD_Vx_K(o: Opcode) {
        registers[o.x] = getKeyPress(0)
        advanceToNextInstruction()

    }

    fun _LD_DT_Vx(o: Opcode) {
        delayTimer = V(o.x)
        advanceToNextInstruction()
    }

    fun _LD_ST_Vx(o: Opcode) {
        soundTimer = V(o.x)
        advanceToNextInstruction()
    }

    fun _ADD_I_Vx(o: Opcode) {
        indexRegister = indexRegister + V(o.x)
        advanceToNextInstruction()
    }

    fun _LD_F_Vx(o: Opcode) {
        indexRegister = getSpriteAddress(V(o.x))
        advanceToNextInstruction()
    }

    fun _LD_B_Vx(o: Opcode) {
        val bcd = getBcd(V(o.x))
        memory[indexRegister] = bcd[0]
        memory[indexRegister+1] = bcd[1]
        memory[indexRegister+2] = bcd[2]
        advanceToNextInstruction()
    }

    fun _LD_I_Vx(o: Opcode) {
        for (index in 0 .. o.x ) {
            memory[indexRegister + index] = V(index)
        }
        advanceToNextInstruction()
    }

    fun _LD_Vx_I(o: Opcode) {
        for (index in 0 .. o.x ) {
            registers[index] = memory[indexRegister + index]
        }
        advanceToNextInstruction()
    }

    fun draw(x: Int, y: Int, height: Int): Boolean {
        val sprite = (0 until height).map { memory[indexRegister + it].toUByte().toString(2).padStart(8, '0') }
        val spriteBits = sprite.map {
            it.map { c -> if (c=='1') 1 else 0 }.toTypedArray()
        }
        for (row in spriteBits) {
            println(row.contentToString())
        }

        var didUnset = false
        for (y_offset in 0 until height) {
            for (x_offset in 0 until 8) {
                val row = (y + y_offset) % DISPLAY_HEIGHT
                val col = (x + x_offset) % DISPLAY_WIDTH
                val pixelValue = spriteBits[y_offset][x_offset]
                didUnset = didUnset or (display[row][col]==1) and (pixelValue==0)
                display[row][col] =  display[row][col] xor pixelValue
            }
        }
        return didUnset
    }

    fun getKeyPress(timeout: Int = 1): Int {
        // TODO: Implement
        return -1
    }

    fun getSpriteAddress(address: Int): Int {
        // TODO
        return -1
    }

    fun getBcd(value: Int): List<Int> {
        return arrayListOf()
    }

}