package io.littlelanguages.minibendu.typesystem

import kotlin.test.Test
import kotlin.test.assertNotNull

class TypeEnvironmentTest {
    
    @Test
    fun `simple TypeEnvironment creation test`() {
        val env = TypeEnvironment.empty()
        assertNotNull(env, "Should be able to create empty environment")
    }
}
