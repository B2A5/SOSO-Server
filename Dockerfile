# ---------- Build Stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Gradle 캐시 살리기: 래퍼/설정 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# 소스 복사 및 빌드
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ---------- Run Stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# 부트 JAR만 복사
COPY --from=build /workspace/build/libs/*SNAPSHOT.jar /app/app.jar

ENV TZ=Asia/Seoul \
    JAVA_OPTS="-Duser.timezone=Asia/Seoul"
EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
