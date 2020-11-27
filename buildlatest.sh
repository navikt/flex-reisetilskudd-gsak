echo "Bygger flex-reisetilskudd-gsak latest"

mvn clean install

docker build . -t flex-reisetilskudd-gsak:latest
