plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.aquadev"
version = "0.0.1-SNAPSHOT"
description = "ai-executor"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.opentelemetry.proto") {
                useVersion("1.3.2-alpha")
                because("1.3.2 uses protobuf 4.x API, compatible with runtime")
            }
        }
    }
}

val commonLibsVersion by extra("1.2.0")

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/IT-Top-AI-Bot/common-libs")
        credentials {
            username = System.getenv("GPR_USER") ?: project.findProperty("gpr.user")?.toString()
            password = System.getenv("GPR_API_KEY") ?: project.findProperty("gpr.key")?.toString()
        }
    }
}

val springAiVersion by extra("2.0.0-M4")
val springCloudVersion by extra("2025.1.1")
val springCloudAwsVersion by extra("4.0.0")
val imageIoVersion by extra("1.4.0")
val resilience4jVersion by extra("2.4.0")
val opentelemetryVersion by extra("2.21.0-alpha")

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
    implementation("org.springframework.ai:spring-ai-tika-document-reader")
    implementation("org.springframework.ai:spring-ai-starter-model-mistral-ai")
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:$opentelemetryVersion")
    implementation("io.github.resilience4j:resilience4j-spring-boot4:$resilience4jVersion")
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
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:$springCloudAwsVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
