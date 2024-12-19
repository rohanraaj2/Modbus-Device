FROM eclipse-temurin:17-jre-alpine as build-stage
WORKDIR /openems
COPY ../../../  .
RUN ./gradlew buildEdge


FROM eclipse-temurin:17-jre-alpine as run-stage

WORKDIR /etc/openems
COPY --from=build-stage /openems/build/ /etc/openems

CMD [ "java", "-jar", "-Dfelix.cm.dir=/etc/openems/config/" , "-Dopenems.data.dir=/etc/openems/data", "/openems-edge.jar" ]