package com.example.loom

import com.example.loom.data.local.entity.FoodSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodSearchTest {
    @Test fun wordsBecomePrefixTerms() = assertEquals("chicken* bre*", FoodSearch.ftsQuery("  chicken  bre "))

    @Test fun persianWordsWork() = assertEquals("مرغ* سینه*", FoodSearch.ftsQuery("مرغ سینه"))

    @Test fun ftsOperatorsAreStripped() = assertEquals("rice* beans*", FoodSearch.ftsQuery("\"rice\" -beans*"))

    @Test fun blankHasNoQuery() = assertNull(FoodSearch.ftsQuery("  - * "))
}
