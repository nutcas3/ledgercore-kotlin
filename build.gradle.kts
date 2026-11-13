plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	war
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.graalvm.buildtools.native") version "1.1.14"
	id("org.jetbrains.kotlinx.kover") version "0.9.11"
	id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

extra["springModulithVersion"] = "2.1.1"
extra["exposedVersion"] = "1.6.1"
extra["kotestVersion"] = "5.9.1"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webclient")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	// Persistence: Exposed DSL + Flyway + PostgreSQL
	implementation("org.jetbrains.exposed:exposed-core:${property("exposedVersion")}")
	implementation("org.jetbrains.exposed:exposed-jdbc:${property("exposedVersion")}")
	implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${property("exposedVersion")}")
	implementation("org.postgresql:postgresql")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")
	implementation("org.springframework.modulith:spring-modulith-starter-core")
	implementation("tools.jackson.module:jackson-module-kotlin")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat-runtime")

	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testImplementation("org.springframework.modulith:spring-modulith-starter-test")
	testImplementation("io.kotest:kotest-runner-junit5:${property("kotestVersion")}")
	testImplementation("io.kotest:kotest-assertions-core:${property("kotestVersion")}")
	testImplementation("io.kotest:kotest-property:${property("kotestVersion")}")
	testImplementation("io.mockk:mockk:1.14.3")
	testImplementation("app.cash.turbine:turbine:1.2.0")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

kover {
	reports {
		verify {
			rule {
				minBound(80)
			}
		}
	}
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
	ignoreFailures = true
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootWar>("bootWar") {
	archiveFileName.set("ledgercore.war")
}
