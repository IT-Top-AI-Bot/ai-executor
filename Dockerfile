# syntax=docker/dockerfile:1.7

############################
# Stage 1: build JAR
############################
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Сначала копируем только файлы, влияющие на зависимости (лучше кэшируется)
COPY gradlew settings.gradle* build.gradle* gradle.properties* /workspace/
COPY gradle /workspace/gradle

RUN chmod +x gradlew

# Прогреваем зависимости (ускоряет последующие сборки)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies || true

# Теперь копируем исходники
COPY . /workspace

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean build -x test


############################
# Stage 2: extract layers
############################
FROM eclipse-temurin:25-jre AS layers
WORKDIR /workspace
COPY --from=builder /workspace/build/libs/*.jar app.jar

# Spring Boot layertools (для слоёв)
RUN java -Djarmode=tools -jar app.jar extract --layers --destination /workspace/extracted


############################
# Stage 3: runtime
############################
FROM eclipse-temurin:25-jre

# K8s пишет/читает /tmp, Spring тоже любит /tmp (embedded Tomcat)
VOLUME /tmp

# Не root
RUN useradd -r -u 10001 -g root -m -d /home/spring spring
USER 10001

WORKDIR /application

# Копируем слои (лучшее кэширование/пересборки)
COPY --from=layers /workspace/extracted/dependencies/ ./
COPY --from=layers /workspace/extracted/snapshot-dependencies/ ./
COPY --from=layers /workspace/extracted/spring-boot-loader/ ./
COPY --from=layers /workspace/extracted/application/ ./

# (Опционально) CDS:
# В k8s реально помогает, но иногда “шумит” на старте/при нестандартной конфигурации.
# Можно отключать сборку CDS аргументом:
#   --build-arg ENABLE_CDS=false
ARG ENABLE_CDS=true
RUN if [ "$ENABLE_CDS" = "true" ]; then \
      java -XX:ArchiveClassesAtExit=/tmp/app.jsa -Dspring.context.exit=onRefresh -jar app.jar || true ; \
    fi

# JVM: container-aware тюнинг лучше делать через проценты, а не фиксированные -Xmx
# MaxRAMPercentage + Metaspace/Direct memory при желании можно ограничить.
ENV JAVA_TOOL_OPTIONS="\
  -XX:MaxRAMPercentage=75 \
  -XX:InitialRAMPercentage=25 \
  -XX:+ExitOnOutOfMemoryError \
  -XX:ErrorFile=/tmp/java_error.log \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp \
  -Xlog:gc*,safepoint:file=/tmp/gc.log:time,uptime,level,tags:filecount=5,filesize=20M \
"

# Если включил CDS, добавим его автоматически (и без падений, если файла нет)
ENV JAVA_CDS_OPTS="-XX:SharedArchiveFile=/tmp/app.jsa"

# Старт скриптом, чтобы мягко добавить CDS и не городить shell-ENTRYPOINT
COPY --chown=10001:0 entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Для MVC обычно 8080
EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]