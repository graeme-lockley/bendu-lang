package io.littlelanguages.bendu.compiler

sealed class NatureOfBinding {
    open fun patch(byteBuilder: ByteBuilder) {}
}

data class IdentifierBinding(val frameOffset: Int) : NatureOfBinding()
data class FunctionBinding(
    var codeOffset: Int = 0,
    val frameOffset: Int? = null,
    val patches: MutableList<Int> = mutableListOf()
) : NatureOfBinding() {
    fun addPatch(patch: Int) =
        patches.add(patch)

    override fun patch(byteBuilder: ByteBuilder) {
        patches.forEach { byteBuilder.writeIntAtPosition(it, codeOffset) }
    }
}

class SymbolTable(private val byteBuilder: ByteBuilder) {
    private var scope = SymbolScope()
    private var depth = 0

    fun openScope() {
        scope = SymbolScope(scope)
        depth += 1
    }

    fun closeScope() {
        scope.bindings.forEach { (_, binding) -> binding.patch(byteBuilder) }

        if (scope.parent != null) {
            scope = scope.parent!!
            depth -= 1
        }
    }

    fun find(name: String): NatureOfBinding? {
        var currentScope: SymbolScope? = scope

        while (currentScope != null) {
            val binding = currentScope.bindings[name]

            if (binding != null) {
                return binding
            }

            currentScope = currentScope.parent
        }

        return null
    }

    fun findIndexed(name: String): Pair<NatureOfBinding, Int>? {
        var currentScope: SymbolScope? = scope
        var depth = 0

        while (currentScope != null) {
            val binding = currentScope.bindings[name]

            if (binding != null) {
                return Pair(binding, depth)
            }

            currentScope = currentScope.parent
            depth += 1
        }

        return null
    }

    fun bindIdentifier(name: String): IdentifierBinding {
        val binding = IdentifierBinding(scope.offset++)
        scope.bindings[name] = binding
        return binding
    }

    fun bindFunction(name: String, mutable: Boolean) {
        val binding = if (mutable)
            FunctionBinding(frameOffset = scope.offset++)
        else
            FunctionBinding()

        scope.bindings[name] = binding
    }
}

private class SymbolScope(val parent: SymbolScope? = null) {
    val bindings = mutableMapOf<String, NatureOfBinding>()
    var offset = 0
}
