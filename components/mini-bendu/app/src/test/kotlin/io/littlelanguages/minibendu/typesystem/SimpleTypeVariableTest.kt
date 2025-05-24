package io.littlelanguages.minibendu.typesystem

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Simple test to verify TypeVariable class accessibility
 */
class SimpleTypeVariableTest {
    
    @Test
    fun `can create type variable`() {
        val variable = TypeVariable.fresh()
        assertNotNull(variable)
    }
}
