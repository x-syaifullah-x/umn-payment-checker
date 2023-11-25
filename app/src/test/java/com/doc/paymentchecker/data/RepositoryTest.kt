package com.doc.paymentchecker.data

import kotlinx.coroutines.runBlocking
import org.junit.Test

class RepositoryTest {

    @Test
    fun test() = runBlocking {
        Repository.getInstance().playerApi("054ad914998b", "22b5ff5")
            .collect {
                println(it)
            }
    }
}