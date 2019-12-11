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

@ExperimentalUnsignedTypes
class Processor {
    companion object {
        const val NUMBER_OF_REGISTERS = 16
        const val MEMORY_SIZE = 4096
        const val STACK_SIZE = 16
        const val F_REGISTER = 15
    }

    //val memory : MutableList<UByte> = UByteArray(MEMORY_SIZE) { 0u }.toMutableList()
    //val registers: MutableList<UByte> = UByteArray(NUMBER_OF_REGISTERS) { 0u }.toMutableList()

    // it's really annoying to have to typecast UBytes or UShorts back and forth to Int,
    // so I'll just use Ints everywhere and make sure to properly handle overflow and underflow

    val memory = IntArray(MEMORY_SIZE) { 0 }
    val registers = IntArray(NUMBER_OF_REGISTERS) { 0 }
    var indexRegister: Int = 0
    val stack =  IntArray(STACK_SIZE) { 0 }
    var stackPointer: Int = 0       // easier to just make this an Int
    var delayTimer: Int = 0
    var soundTimer: Int = 0
    var programCounter: Int = 0

    fun reset() {
        memory.fill(0)
        registers.fill(0)
        indexRegister = 0
        stack.fill(0)
        stackPointer = 0
        delayTimer = 0
        soundTimer = 0
        programCounter = 0


    }

    fun debug() {
        reset()
        println("registers of size ${registers.size}: $registers")
        println("memory.size: ${memory.size}")
    }

    fun readOpcode() : Int {
        val pc = programCounter.toInt()
        return (memory[pc].toInt() shl 8) or memory[pc+1]
    }

    fun decodeInstruction() {
        val opcode = Opcode.create(readOpcode())
        if (opcode == Opcode(0, 0, 0xE, 0)) {

        }
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
        registers[o.x] == o.byte
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
        draw(V(o.x), V(o.y), o.d)
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
        // register dump to address starting at Index Register
        for (reg in registers.withIndex()) {
            memory[indexRegister + reg.index] = reg.value
        }
        advanceToNextInstruction()
    }

    fun _LD_Vx_I(o: Opcode) {
        // load into registers starting from memory at Index Register
        for (index in registers.indices) {
            registers[index] = memory[indexRegister + index]
        }
        advanceToNextInstruction()
    }

    fun draw(x: Int, y: Int, height: Int) {
        // TODO
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