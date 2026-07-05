# Imagem self-contained para compilar o app Android sem poluir o host.
# JDK 17 + Gradle + Android SDK (command-line tools).
FROM eclipse-temurin:17-jdk

# Versões fixas para builds reproduzíveis.
ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    GRADLE_VERSION=8.10.2 \
    CMDLINE_TOOLS_VERSION=11076708 \
    ANDROID_PLATFORM=android-35 \
    ANDROID_BUILD_TOOLS=35.0.0 \
    DEBIAN_FRONTEND=noninteractive

# Dependências básicas.
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl unzip ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Gradle (usado para gerar o wrapper na 1ª vez).
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    rm /tmp/gradle.zip
ENV PATH="/opt/gradle-${GRADLE_VERSION}/bin:${PATH}"

# Android SDK command-line tools.
RUN mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools" && \
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -o /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d "${ANDROID_SDK_ROOT}/cmdline-tools" && \
    mv "${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest" && \
    rm /tmp/cmdline-tools.zip
ENV PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

# Aceita licenças e instala plataforma + build-tools.
RUN yes | sdkmanager --licenses >/dev/null && \
    sdkmanager --install "platform-tools" "platforms;${ANDROID_PLATFORM}" "build-tools;${ANDROID_BUILD_TOOLS}" >/dev/null && \
    chmod -R a+rwX "${ANDROID_SDK_ROOT}"

WORKDIR /workspace
