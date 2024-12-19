FROM eclipse-temurin:17-jre-alpine as build-stage


WORKDIR /openems
COPY ../../../  .
RUN ./gradlew buildBackend

FROM eclipse-temurin:17-jre-alpine

COPY --from=build-stage /openems/build/ .
COPY config.d /config.d
ENTRYPOINT ["java"]
CMD ["-XX:+ExitOnOutOfMemoryError", "-Dfelix.cm.dir=/config.d", "-Djava.util.concurrent.ForkJoinPool.common.parallelism=100" ,"-jar","openems-backend.jar"]
