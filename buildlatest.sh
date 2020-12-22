echo "Bygger flex-reisetilskudd-gsak latest"

./gradlew bootJar

docker build . -t flex-reisetilskudd-gsak:latest
