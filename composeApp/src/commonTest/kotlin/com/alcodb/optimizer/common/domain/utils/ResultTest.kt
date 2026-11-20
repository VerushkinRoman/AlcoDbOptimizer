package com.alcodb.optimizer.common.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ResultTest {
    @Test
    fun mapTransformsSuccessData() {
        val result = Result.Success("42").map(String::toInt)

        assertEquals(Result.Success(42), result)
    }

    @Test
    fun mapPreservesErrorWithoutCallingTransform() {
        var called = false
        val result = Result.Error(TestError.Failed).map {
            called = true
            it
        }

        assertEquals(Result.Error(TestError.Failed), result)
        assertFalse(called)
    }

    @Test
    fun onSuccessCallsActionOnlyForSuccess() {
        var successValue: String? = null
        var errorCalled = false

        Result.Success("data").onSuccess { successValue = it }
        Result.Error(TestError.Failed).onSuccess { errorCalled = true }

        assertEquals("data", successValue)
        assertFalse(errorCalled)
    }

    @Test
    fun onErrorCallsActionOnlyForError() {
        var receivedError: TestError? = null
        var successCalled = false

        Result.Error(TestError.Failed).onError { receivedError = it }
        Result.Success("data").onError { successCalled = true }

        assertEquals(TestError.Failed, receivedError)
        assertFalse(successCalled)
    }

    @Test
    fun asEmptyDataResultDropsSuccessData() {
        val result = Result.Success("data").asEmptyDataResult()

        assertEquals(Result.Success(Unit), result)
    }

    @Test
    fun getOrNullReturnsDataOrNull() {
        assertEquals("data", Result.Success("data").getOrNull())
        assertNull(Result.Error(TestError.Failed).getOrNull())
    }

    @Test
    fun emptySuccessResultIsUnitSuccess() {
        assertEquals(Unit, EmptySuccessResult.data)
    }

    private enum class TestError : Error {
        Failed,
    }
}
