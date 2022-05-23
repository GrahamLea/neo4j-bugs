/*
 * Copyright (c) 2022 Archium Technology Pty Ltd. All rights reserved.
 */


package test

import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity
import org.springframework.data.neo4j.annotation.Query
import org.springframework.data.neo4j.annotation.QueryResult
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

@NodeEntity
class TestNode(val name: String) {

//    constructor() : this("")

    @Id
    @GeneratedValue
    var id: Long? = null
}

@Repository
interface TestNodeRepository : Neo4jRepository<TestNode, Long> {
    fun findByName(name: String): TestNode
    @Query("MATCH (testNode:TestNode) RETURN testNode")
    fun findAllInQueryResult(): List<TestNodeQueryResult>
}


@QueryResult
class TestNodeQueryResult(val testNode: TestNode)
