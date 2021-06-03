
clean:
	./gradlew clean

run:
	./gradlew asciidoctor
	cd build/docs/asciidoc ; python -m http.server
