/*
 * Copyright (c) 2022 Archium Technology Pty Ltd. All rights reserved.
 */

package test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class Application {
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
