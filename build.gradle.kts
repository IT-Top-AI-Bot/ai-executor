plugins {
    java
    id("org.springframework.boot") version "3.5.11"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "com.aquadev"
version = "0.0.1-SNAPSHOT"
description = "it-top-ai-executor"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.1.2"
extra["awsSdkBomVersion"] = "3.4.2"

dependencies {
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.ai:spring-ai-tika-document-reader")
    implementation("com.github.jai-imageio:jai-imageio-jpeg2000:1.4.0")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.ai:spring-ai-starter-model-mistral-ai")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:${property("awsSdkBomVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
