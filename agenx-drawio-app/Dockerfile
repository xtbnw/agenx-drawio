FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=dev
ENV SERVER_PORT=8091
ENV MANAGEMENT_SERVER_PORT=9091

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY target/agenx-drawio-app.jar /app/app.jar

EXPOSE 8091
EXPOSE 9091

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE} --server.port=${SERVER_PORT} --management.server.port=${MANAGEMENT_SERVER_PORT}"]