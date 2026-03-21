plugins {
    java
    id("org.springframework.boot") version "4.0.4"
    id("io.spring.dependency-management") version "1.1.7"
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

val commonLibsVersion by extra("1.1.0")

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/aquadev-pet-projects/common-libs")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user")?.toString()
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key")?.toString()
        }
    }
}

val springAiVersion by extra("2.0.0-M3")
val springCloudAwsVersion by extra("4.0.0")
val imageIoVersion by extra("1.4.0")

dependencies {
    implementation("com.aquadev:common-libs:$commonLibsVersion")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.ai:spring-ai-starter-model-mistral-ai")
    implementation("org.springframework.ai:spring-ai-tika-document-reader")
    implementation("com.github.jai-imageio:jai-imageio-jpeg2000:$imageIoVersion")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:$springCloudAwsVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
