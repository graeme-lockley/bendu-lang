package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*

/**
 * Result of pattern analysis containing exhaustiveness, reachability, and refinement information.
 */
data class PatternAnalysisResult(
    val isExhaustive: Boolean,
    val missingPatterns: List<Pattern>,
    val unreachablePatterns: List<MatchCase>,
    val contradictoryPatterns: List<Pair<MatchCase, MatchCase>>,
    val typeRefinements: Map<MatchCase, Map<String, Type>>
)

/**
 * Comprehensive pattern analysis for match expressions.
 * Implements Task 52 - Pattern Type Refinement Implementation.
 */
object PatternAnalyzer {
    
    /**
     * Perform complete pattern analysis on a match expression.
     */
    fun analyze(matchExpr: MatchExpr, scrutineeType: Type): PatternAnalysisResult {
        val exhaustivenessResult = ExhaustivenessChecker.check(matchExpr, scrutineeType)
        val reachabilityResult = ReachabilityAnalyzer.analyze(matchExpr, scrutineeType)
        val contradictionResult = ContradictionDetector.detect(matchExpr)
        val refinementResult = TypeRefinementAnalyzer.analyze(matchExpr, scrutineeType)
        
        return PatternAnalysisResult(
            isExhaustive = exhaustivenessResult.isExhaustive,
            missingPatterns = exhaustivenessResult.missingPatterns,
            unreachablePatterns = reachabilityResult.unreachablePatterns,
            contradictoryPatterns = contradictionResult.contradictoryPairs,
            typeRefinements = refinementResult.refinements
        )
    }
}

/**
 * Exhaustiveness checking for pattern matching.
 * Determines if all possible values of a type are covered by the patterns.
 */
object ExhaustivenessChecker {
    
    data class ExhaustivenessResult(
        val isExhaustive: Boolean,
        val missingPatterns: List<Pattern>
    )
    
    /**
     * Check if a match expression exhaustively covers all possible values of the scrutinee type.
     */
    fun check(matchExpr: MatchExpr, scrutineeType: Type): ExhaustivenessResult {
        val coveredValues = extractCoveredValues(matchExpr.cases.map { it.pattern })
        val hasWildcard = matchExpr.cases.any { hasWildcardPattern(it.pattern) }
        
        return when {
            hasWildcard -> ExhaustivenessResult(isExhaustive = true, missingPatterns = emptyList())
            else -> checkExhaustiveness(scrutineeType, coveredValues)
        }
    }
    
    /**
     * Check exhaustiveness for a specific type given covered values.
     */
    private fun checkExhaustiveness(type: Type, coveredValues: Set<Any>): ExhaustivenessResult {
        return when (type) {
            Types.Bool -> checkBoolExhaustiveness(coveredValues)
            is UnionType -> checkUnionExhaustiveness(type, coveredValues)
            is LiteralStringType -> checkLiteralStringExhaustiveness(type, coveredValues)
            else -> {
                // For other types (Int, String, etc.), we can't enumerate all values
                // so we consider them non-exhaustive unless there's a wildcard
                ExhaustivenessResult(isExhaustive = false, missingPatterns = emptyList())
            }
        }
    }
    
    /**
     * Check exhaustiveness for boolean types.
     */
    private fun checkBoolExhaustiveness(coveredValues: Set<Any>): ExhaustivenessResult {
        val coveredBools = coveredValues.filterIsInstance<Boolean>().toSet()
        val missing = setOf(true, false) - coveredBools
        
        val missingPatterns = missing.map { value ->
            LiteralBoolPattern(BoolLocation(value, createDummyLocation()))
        }
        
        return ExhaustivenessResult(
            isExhaustive = missing.isEmpty(),
            missingPatterns = missingPatterns
        )
    }
    
    /**
     * Check exhaustiveness for union types.
     */
    private fun checkUnionExhaustiveness(unionType: UnionType, coveredValues: Set<Any>): ExhaustivenessResult {
        val flattened = UnionTypeUtils.flatten(unionType)
        val missing = mutableListOf<Pattern>()
        
        for (alternative in flattened) {
            val altResult = checkExhaustiveness(alternative, coveredValues)
            if (!altResult.isExhaustive) {
                missing.addAll(altResult.missingPatterns)
            }
        }
        
        return ExhaustivenessResult(
            isExhaustive = missing.isEmpty(),
            missingPatterns = missing
        )
    }
    
    /**
     * Check exhaustiveness for literal string types.
     */
    private fun checkLiteralStringExhaustiveness(literalType: LiteralStringType, coveredValues: Set<Any>): ExhaustivenessResult {
        val isCovered = coveredValues.contains(literalType.value)
        
        val missingPattern = if (!isCovered) {
            LiteralStringPattern(StringLocation(literalType.value, createDummyLocation()))
        } else null
        
        return ExhaustivenessResult(
            isExhaustive = isCovered,
            missingPatterns = listOfNotNull(missingPattern)
        )
    }
    
    /**
     * Extract values that are covered by the given patterns.
     */
    private fun extractCoveredValues(patterns: List<Pattern>): Set<Any> {
        val covered = mutableSetOf<Any>()
        
        for (pattern in patterns) {
            when (pattern) {
                is LiteralIntPattern -> covered.add(pattern.value.value)
                is LiteralStringPattern -> covered.add(pattern.value.value)
                is LiteralBoolPattern -> covered.add(pattern.value.value)
                is TuplePattern -> {
                    // For tuples, we need to consider combinations
                    // This is simplified - full implementation would need cartesian products
                    val elementValues = pattern.elements.map { extractCoveredValues(listOf(it)) }
                    if (elementValues.all { it.isNotEmpty() }) {
                        covered.add(elementValues)
                    }
                }
                is RecordPattern -> {
                    // For records, extract field values
                    val fieldValues = pattern.fields.associate { field ->
                        field.id.value to extractCoveredValues(listOf(field.pattern))
                    }
                    covered.add(fieldValues)
                }
                is VarPattern -> {
                    // Variable patterns match all values - cannot extract specific values
                    // This would be handled at a higher level in exhaustiveness checking
                }
                is WildcardPattern -> {
                    // Wildcard patterns match all values - cannot extract specific values
                    // This would be handled at a higher level in exhaustiveness checking
                }
            }
        }
        
        return covered
    }
    
    /**
     * Check if a pattern contains a wildcard that matches everything.
     */
    private fun hasWildcardPattern(pattern: Pattern): Boolean {
        return when (pattern) {
            is WildcardPattern -> true
            is VarPattern -> true  // Variable patterns also match everything
            is TuplePattern -> pattern.elements.any { hasWildcardPattern(it) }
            is RecordPattern -> pattern.fields.any { hasWildcardPattern(it.pattern) }
            else -> false
        }
    }
    
    private fun createDummyLocation() = io.littlelanguages.scanpiler.LocationCoordinate(0, 1, 1)
}

/**
 * Pattern reachability analysis.
 * Detects unreachable patterns that can never be matched.
 */
object ReachabilityAnalyzer {
    
    data class ReachabilityResult(
        val unreachablePatterns: List<MatchCase>,
        val shadowingWarnings: List<String>
    )
    
    /**
     * Analyze pattern reachability in a match expression.
     */
    fun analyze(matchExpr: MatchExpr, scrutineeType: Type): ReachabilityResult {
        val unreachable = mutableListOf<MatchCase>()
        val warnings = mutableListOf<String>()
        val cases = matchExpr.cases
        
        for (i in cases.indices) {
            val currentCase = cases[i]
            val previousCases = cases.subList(0, i)
            
            if (isUnreachable(currentCase, previousCases, scrutineeType)) {
                unreachable.add(currentCase)
                
                // Generate specific warnings
                when {
                    previousCases.any { hasWildcardPattern(it.pattern) } -> {
                        warnings.add("Pattern is unreachable due to previous wildcard pattern")
                    }
                    else -> {
                        warnings.add("Pattern is unreachable due to previous patterns")
                    }
                }
            }
        }
        
        return ReachabilityResult(unreachable, warnings)
    }
    
    /**
     * Check if a pattern case is unreachable given previous cases.
     */
    private fun isUnreachable(currentCase: MatchCase, previousCases: List<MatchCase>, scrutineeType: Type): Boolean {
        // If any previous case has a wildcard or variable pattern, subsequent patterns are unreachable
        if (previousCases.any { hasWildcardPattern(it.pattern) }) {
            return true
        }
        
        // Check if the current pattern is already covered by previous literal patterns
        return when (val currentPattern = currentCase.pattern) {
            is LiteralIntPattern -> {
                previousCases.any { case ->
                    case.pattern is LiteralIntPattern && 
                    case.pattern.value.value == currentPattern.value.value
                }
            }
            is LiteralStringPattern -> {
                previousCases.any { case ->
                    case.pattern is LiteralStringPattern && 
                    case.pattern.value.value == currentPattern.value.value
                }
            }
            is LiteralBoolPattern -> {
                previousCases.any { case ->
                    case.pattern is LiteralBoolPattern && 
                    case.pattern.value.value == currentPattern.value.value
                }
            }
            is TuplePattern -> {
                // Check if tuple pattern is covered by previous tuple patterns
                previousCases.any { case ->
                    case.pattern is TuplePattern && 
                    patternsEquivalent(currentPattern, case.pattern)
                }
            }
            is RecordPattern -> {
                // Check if record pattern is covered by previous record patterns
                previousCases.any { case ->
                    case.pattern is RecordPattern && 
                    patternsEquivalent(currentPattern, case.pattern)
                }
            }
            else -> false
        }
    }
    
    /**
     * Check if two patterns are equivalent (match the same values).
     */
    private fun patternsEquivalent(pattern1: Pattern, pattern2: Pattern): Boolean {
        return when {
            pattern1 is LiteralIntPattern && pattern2 is LiteralIntPattern -> {
                pattern1.value.value == pattern2.value.value
            }
            pattern1 is LiteralStringPattern && pattern2 is LiteralStringPattern -> {
                pattern1.value.value == pattern2.value.value
            }
            pattern1 is LiteralBoolPattern && pattern2 is LiteralBoolPattern -> {
                pattern1.value.value == pattern2.value.value
            }
            pattern1 is TuplePattern && pattern2 is TuplePattern -> {
                pattern1.elements.size == pattern2.elements.size &&
                pattern1.elements.zip(pattern2.elements).all { (p1, p2) ->
                    patternsEquivalent(p1, p2)
                }
            }
            pattern1 is RecordPattern && pattern2 is RecordPattern -> {
                val fields1 = pattern1.fields.associate { it.id.value to it.pattern }
                val fields2 = pattern2.fields.associate { it.id.value to it.pattern }
                
                fields1.keys == fields2.keys &&
                fields1.all { (name, p1) ->
                    val p2 = fields2[name]
                    p2 != null && patternsEquivalent(p1, p2)
                }
            }
            else -> false
        }
    }
    
    private fun hasWildcardPattern(pattern: Pattern): Boolean {
        return when (pattern) {
            is WildcardPattern -> true
            is VarPattern -> true
            else -> false
        }
    }
}

/**
 * Contradiction detection in patterns.
 * Finds patterns that can never both match the same value.
 */
object ContradictionDetector {
    
    data class ContradictionResult(
        val contradictoryPairs: List<Pair<MatchCase, MatchCase>>
    )
    
    /**
     * Detect contradictory patterns in a match expression.
     */
    fun detect(matchExpr: MatchExpr): ContradictionResult {
        val contradictions = mutableListOf<Pair<MatchCase, MatchCase>>()
        val cases = matchExpr.cases
        
        for (i in cases.indices) {
            for (j in i + 1 until cases.size) {
                val case1 = cases[i]
                val case2 = cases[j]
                
                if (areContradictory(case1.pattern, case2.pattern)) {
                    contradictions.add(Pair(case1, case2))
                }
            }
        }
        
        return ContradictionResult(contradictions)
    }
    
    /**
     * Check if two patterns are contradictory (can never both match).
     */
    private fun areContradictory(pattern1: Pattern, pattern2: Pattern): Boolean {
        return when {
            // Identical literal patterns are contradictory (duplicate)
            pattern1 is LiteralIntPattern && pattern2 is LiteralIntPattern -> {
                pattern1.value.value == pattern2.value.value
            }
            pattern1 is LiteralStringPattern && pattern2 is LiteralStringPattern -> {
                pattern1.value.value == pattern2.value.value
            }
            pattern1 is LiteralBoolPattern && pattern2 is LiteralBoolPattern -> {
                pattern1.value.value == pattern2.value.value
            }
            
            // Different literal patterns of the same type are not contradictory
            // (they just match different values)
            
            // Tuple patterns are contradictory if they have contradictory elements
            pattern1 is TuplePattern && pattern2 is TuplePattern -> {
                pattern1.elements.size == pattern2.elements.size &&
                pattern1.elements.zip(pattern2.elements).all { (p1, p2) ->
                    areContradictory(p1, p2)
                }
            }
            
            // Record patterns are contradictory if they have contradictory fields
            pattern1 is RecordPattern && pattern2 is RecordPattern -> {
                val fields1 = pattern1.fields.associate { it.id.value to it.pattern }
                val fields2 = pattern2.fields.associate { it.id.value to it.pattern }
                
                val commonFields = fields1.keys intersect fields2.keys
                commonFields.isNotEmpty() && commonFields.all { fieldName ->
                    val p1 = fields1[fieldName]!!
                    val p2 = fields2[fieldName]!!
                    areContradictory(p1, p2)
                }
            }
            
            else -> false
        }
    }
}

/**
 * Type refinement analysis for patterns.
 * Determines how types are narrowed within match case bodies.
 */
object TypeRefinementAnalyzer {
    
    data class RefinementResult(
        val refinements: Map<MatchCase, Map<String, Type>>
    )
    
    /**
     * Analyze type refinements in a match expression.
     */
    fun analyze(matchExpr: MatchExpr, scrutineeType: Type): RefinementResult {
        val refinements = mutableMapOf<MatchCase, Map<String, Type>>()
        
        for (case in matchExpr.cases) {
            val caseRefinements = analyzePatternRefinement(case.pattern, scrutineeType)
            refinements[case] = caseRefinements
        }
        
        return RefinementResult(refinements)
    }
    
    /**
     * Analyze how a pattern refines the type of variables it binds.
     */
    private fun analyzePatternRefinement(pattern: Pattern, expectedType: Type): Map<String, Type> {
        val refinements = mutableMapOf<String, Type>()
        
        when (pattern) {
            is LiteralIntPattern -> {
                // Literal patterns refine the scrutinee type
                if (expectedType is UnionType) {
                    // Find the specific type in the union that matches this literal
                    val refinedType = findMatchingTypeInUnion(expectedType, Types.Int)
                    if (refinedType != null) {
                        // Note: We can't directly refine the scrutinee variable here
                        // This would need to be handled at a higher level
                    }
                }
            }
            
            is LiteralStringPattern -> {
                if (expectedType is UnionType) {
                    val literalType = Types.literal(pattern.value.value)
                    val refinedType = findMatchingTypeInUnion(expectedType, literalType)
                    // Similar to above - scrutinee refinement would be handled externally
                }
            }
            
            is LiteralBoolPattern -> {
                if (expectedType is UnionType) {
                    val refinedType = findMatchingTypeInUnion(expectedType, Types.Bool)
                    // Similar to above
                }
            }
            
            is VarPattern -> {
                // Variable patterns bind the expected type
                refinements[pattern.id.value] = expectedType
            }
            
            is TuplePattern -> {
                if (expectedType is TupleType && pattern.elements.size == expectedType.elements.size) {
                    for ((elementPattern, elementType) in pattern.elements.zip(expectedType.elements)) {
                        val elementRefinements = analyzePatternRefinement(elementPattern, elementType)
                        refinements.putAll(elementRefinements)
                    }
                }
            }
            
            is RecordPattern -> {
                when (expectedType) {
                    is RecordType -> {
                        for (fieldPattern in pattern.fields) {
                            val fieldName = fieldPattern.id.value
                            val fieldType = expectedType.fields[fieldName]
                            if (fieldType != null) {
                                val fieldRefinements = analyzePatternRefinement(fieldPattern.pattern, fieldType)
                                refinements.putAll(fieldRefinements)
                            }
                        }
                    }
                    is UnionType -> {
                        // For union types, we need to find which alternative this record pattern matches
                        val matchingAlternatives = expectedType.alternatives.filterIsInstance<RecordType>()
                            .filter { recordType ->
                                pattern.fields.all { fieldPattern ->
                                    recordType.fields.containsKey(fieldPattern.id.value)
                                }
                            }
                        
                        // If there's exactly one matching alternative, use that for refinement
                        if (matchingAlternatives.size == 1) {
                            val matchingType = matchingAlternatives.first()
                            for (fieldPattern in pattern.fields) {
                                val fieldName = fieldPattern.id.value
                                val fieldType = matchingType.fields[fieldName]
                                if (fieldType != null) {
                                    val fieldRefinements = analyzePatternRefinement(fieldPattern.pattern, fieldType)
                                    refinements.putAll(fieldRefinements)
                                }
                            }
                        }
                    }
                    is PrimitiveType, is LiteralStringType, is FunctionType, is TupleType, 
                    is IntersectionType, is TypeAlias, is TypeVariable -> {
                        // Record patterns cannot match against these types
                        // This would be a type error that should be caught during type checking
                    }
                }
            }
            
            is WildcardPattern -> {
                // Wildcard patterns don't bind any variables
            }
        }
        
        return refinements
    }
    
    /**
     * Find a type in a union that is compatible with the given type.
     */
    private fun findMatchingTypeInUnion(unionType: UnionType, targetType: Type): Type? {
        return unionType.alternatives.find { alternative ->
            alternative.structurallyEquivalent(targetType)
        }
    }
} 