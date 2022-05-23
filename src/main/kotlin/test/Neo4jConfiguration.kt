/*
 * Copyright (c) 2022 Archium Technology Pty Ltd. All rights reserved.
 */


package test

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories

@Configuration
@EnableNeo4jRepositories(basePackages = ["test"])
@EntityScan(basePackages = ["test"])
class Neo4jConfiguration
