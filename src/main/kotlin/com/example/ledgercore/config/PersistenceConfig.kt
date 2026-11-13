package com.example.ledgercore.config

import javax.sql.DataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PersistenceConfig {

    @Bean
    fun database(dataSource: DataSource): Database = Database.connect(dataSource)
}
