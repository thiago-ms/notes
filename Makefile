GRADLE_VERSION := 8.10.2
DC := docker compose
USERFLAG := --user $(shell id -u):$(shell id -g)
RUN := $(DC) run --rm $(USERFLAG) android

.PHONY: help image wrapper apk dist apk-release dist-release dist-all keystore test clean shell

help:
	@echo "Alvos disponíveis:"
	@echo "  make image        - constrói a imagem Docker de build (JDK + Android SDK + Gradle)"
	@echo "  make wrapper      - gera o Gradle wrapper (necessário na 1ª vez)"
	@echo "  make apk          - APK debug versionado em dist/ (alias de dist)"
	@echo "  make dist         - APK debug com a versão no nome em dist/notes-<versão>-debug.apk"
	@echo "  make apk-release  - APK release versionado em dist/ (alias de dist-release)"
	@echo "  make dist-release - APK release (assinado + R8) em dist/notes-<versão>-release.apk"
	@echo "  make dist-all     - gera debug e release em dist/"
	@echo "  make keystore     - gera uma keystore de release dedicada em keystore/ (opcional)"
	@echo "  make test         - roda os testes unitários de todos os módulos"
	@echo "  make shell        - abre um shell dentro do container de build"
	@echo "  make clean        - limpa artefatos de build do Gradle"

image:
	$(DC) build

# Gera o wrapper apenas se ainda não existir (arquivo gradlew no host).
wrapper: image gradlew

gradlew:
	$(RUN) gradle wrapper --gradle-version $(GRADLE_VERSION)

# Alvo padrão de APK: entrega em dist/ (versionado). O Gradle sempre escreve a
# cópia bruta em app/build/outputs/ primeiro; distApk copia para dist/.
apk: dist

dist: wrapper
	$(RUN) ./gradlew --no-daemon :app:distApk

apk-release: dist-release

dist-release: wrapper
	$(RUN) ./gradlew --no-daemon :app:distReleaseApk

# Debug + release de uma vez.
dist-all: wrapper
	$(RUN) ./gradlew --no-daemon :app:distApk :app:distReleaseApk

# Gera uma keystore de release dedicada (não interativa) e o keystore.properties.
# Ambos ficam em keystore/ e são ignorados pelo git. Rode uma vez; depois os
# builds de release passam a usar essa keystore automaticamente.
keystore: image
	$(RUN) sh -c 'mkdir -p keystore && \
	  keytool -genkeypair -v -keystore keystore/release.jks -alias notes \
	    -keyalg RSA -keysize 2048 -validity 10000 \
	    -storepass notes123 -keypass notes123 \
	    -dname "CN=Notes, OU=Dev, O=Notes, L=SP, S=SP, C=BR" && \
	  printf "storeFile=keystore/release.jks\nstorePassword=notes123\nkeyAlias=notes\nkeyPassword=notes123\n" > keystore/keystore.properties'
	@echo ">> keystore gerada em keystore/release.jks (troque as senhas em keystore/keystore.properties p/ algo real)"

test: wrapper
	$(RUN) ./gradlew --no-daemon testDebugUnitTest

shell: image
	$(RUN) bash

clean: wrapper
	$(RUN) ./gradlew --no-daemon clean
