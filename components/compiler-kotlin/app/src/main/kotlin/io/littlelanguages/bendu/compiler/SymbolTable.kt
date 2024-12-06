package io.littlelanguages.bendu.compiler

sealed class NatureOfBinding {
    open fun patch(byteBuilder: ByteBuilder) {}
}

data class PackageBinding(val offset: Int) : NatureOfBinding()
data class ParameterBinding(val offset: Int) : NatureOfBinding()
data class PackageFunction(var offset: Int = 0, val patches: MutableList<Int> = mutableListOf()) :
    NatureOfBinding() {
    fun addPatch(patch: Int) =
        patches.add(patch)

    override fun patch(byteBuilder: ByteBuilder) {
        patches.forEach { byteBuilder.writeIntAtPosition(it, offset) }
    }
}

class SymbolTable(val byteBuilder: ByteBuilder) {
    private var scope = SymbolScope()
    private var depth = 0

    fun openScope() {
        scope = SymbolScope(scope)
        depth += 1
    }

    fun closeScope() {
        scope.bindings.forEach { _, binding -> binding.patch(byteBuilder) }

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

    fun bind(name: String, binding: NatureOfBinding) {
        scope.bindings[name] = binding
    }

    fun bindPackageBinding(name: String): PackageBinding {
        val binding = PackageBinding(scope.offset++)
        scope.bindings[name] = binding
        return binding
    }
}

private class SymbolScope(val parent: SymbolScope? = null) {
    val bindings = mutableMapOf<String, NatureOfBinding>()
    var offset = 0
}
