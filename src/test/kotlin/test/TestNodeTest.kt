/*
 * Copyright (c) 2022 Archium Technology Pty Ltd. All rights reserved.
 */

package test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.neo4j.harness.Neo4j
import org.neo4j.harness.Neo4jBuilders
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.concurrent.thread

@SpringBootTest
class TestNodeTest {

    @Autowired
    private lateinit var testNodeRepository: TestNodeRepository

    // Passes with:
    //        <neo4j.version>4.4.0</neo4j.version>
    //        <neo4j.driver.version>4.3.1</neo4j.driver.version>
    // Fails with:
    //        <neo4j.version>4.4.0</neo4j.version>
    //        <neo4j.driver.version>4.3.2</neo4j.driver.version>
    @Test
    fun `raises DataIntegrityViolationException on constraint violation`() {
        neo4j!!.defaultDatabaseService().executeTransactionally(
            "CREATE CONSTRAINT TNC ON (tn:TestNode) ASSERT tn.name IS UNIQUE")
        testNodeRepository.save(TestNode("Bob"))
        try {
            testNodeRepository.save(TestNode("Bob"))
        } catch (e: Throwable) {
            assertThat(e::class.java).isEqualTo(DataIntegrityViolationException::class.java)
        }
    }

    // Passes with:
    //        <neo4j.version>4.1.9</neo4j.version>
    //        <neo4j.driver.version>4.4.5</neo4j.driver.version>
    // Fails with:
    //        <neo4j.version>4.2.0</neo4j.version>
    //        <neo4j.driver.version>4.4.5</neo4j.driver.version>
    @Test
    fun `raises TransientExceptions during concurrent modifications`() {

        createLotsOfNodes()
        getNodes()

        val errors = mutableListOf<Throwable>()

        val readOnlyTransactionCallingThread = thread(start = true) {
            var nodesExist = true
            while (nodesExist) {
                    try {
                        nodesExist = getNodes() != 0
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: Throwable) {
                        println("    Caught $e")
                        errors.add(e)
                        break
                    }
            }
        }
        try {
            deleteAllNodesOneAtATime(readOnlyTransactionCallingThread)
            readOnlyTransactionCallingThread.join()
        } catch (e: Exception) {
            readOnlyTransactionCallingThread.interrupt()
        }

        val errorTypes = errors.map { it::class.java.simpleName }.toSet()
        assertThat(errorTypes).isEqualTo(setOf("TransientException"))
    }

    private fun createLotsOfNodes() {
        println("Creating $NODE_COUNT nodes...")
        for (i in 1 until NODE_COUNT) {
            testNodeRepository.save(TestNode(nameForNode(i)))
        }
        println("Done")
    }

    private fun getNodes(): Int {
        println("                                Getting nodes...")
        var count = 0
        testNodeRepository.findAllInQueryResult().forEach { count++ }
        println("                                    Retrieved $count nodes...")
        return count
    }

    private fun deleteAllNodesOneAtATime(readOnlyTransactionCallingThread: Thread) {
        for (i in 1 until NODE_COUNT) {
            if (!readOnlyTransactionCallingThread.isAlive) {
                break
            }
            println("Deleting 1 node...")
            val node = testNodeRepository.findByName(nameForNode(i))
            testNodeRepository.delete(node)
            println("    Deleted 1 node")
        }
    }

    private fun nameForNode(i: Int) = "Node $i"

    companion object {
        private const val NODE_COUNT = 150

        @JvmStatic
        private var neo4j: Neo4j? = null

        @JvmStatic
        @BeforeAll
        fun initializeNeo4j() {
            if (neo4j == null) {
                println("Creating Neo4j instance for test")
                neo4j = Neo4jBuilders.newInProcessBuilder()
                    .withDisabledServer() // No need for http
                    .build()
                println("Neo4j Bolt URI: " + neo4j!!.boltURI())
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun neo4jProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.neo4j.uri") { neo4j!!.boltURI() }
            registry.add("spring.data.neo4j.password") { "" }
        }

        @JvmStatic
        @AfterAll
        fun closeNeo4j() {
            neo4j?.close()
        }
    }
}