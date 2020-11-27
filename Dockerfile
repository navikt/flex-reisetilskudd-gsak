FROM navikt/java:14
COPY target/app.jar /app/

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
