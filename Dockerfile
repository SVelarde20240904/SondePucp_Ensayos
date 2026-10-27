# =====================================================================
#  Son de PUCP · Sistema de Ensayos — imagen para publicar en internet
#  Sirve para Railway, Render, Fly.io o cualquier VPS con Docker.
#  La app queda en la raíz del dominio (https://tu-dominio/), no en /sdp-ensayos
# =====================================================================

# ---- 1. Compilar con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

# ---- 2. Correr en Tomcat 10.1 ----
FROM tomcat:10.1-jre17-temurin

ENV TZ=America/Lima \
    SDP_ENTORNO=produccion \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/urandom -Dfile.encoding=UTF-8"

WORKDIR /usr/local/tomcat

# - Fuera las apps de ejemplo de Tomcat (manager, docs...): son un riesgo en producción
# - RemoteIpValve: el hosting pone HTTPS delante y avisa con X-Forwarded-Proto, así Tomcat sabe que es seguro
#   (cookie Secure + no redirige en bucle). Solo confía en IPs internas, que es por donde llega el proxy.
# - ErrorReportValve sin detalles: no mostrar versión de Tomcat ni stack traces
# - Puerto de apagado desactivado
RUN rm -rf webapps/* webapps.dist \
 && sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="X-Forwarded-For" protocolHeader="X-Forwarded-Proto"/>\n        <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/>\n      </Host>#' conf/server.xml \
 && sed -i 's#<Server port="8005"#<Server port="-1"#' conf/server.xml \
 && useradd -r -u 10001 -d /usr/local/tomcat sdp \
 && chown -R sdp:sdp /usr/local/tomcat

COPY --from=build --chown=sdp:sdp /app/target/sdp-ensayos.war webapps/ROOT.war

USER sdp
EXPOSE 8080
# Railway/Render dicen en qué puerto escuchar con la variable PORT
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" conf/server.xml && exec catalina.sh run"]
