# =====================================================
# Imagem do Sentinel (versão web) para hospedagem
# Duas etapas: uma COMPILA (com JDK), a outra só RODA (com JRE, bem menor)
# =====================================================

# ---------- 1) compilar ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# primeiro só o Gradle: assim o download dele fica em cache entre um deploy e outro
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --version

# depois o código
COPY src src
RUN ./gradlew --no-daemon installDist -x test
# (os testes rodam no GitHub Actions a cada push; aqui só empacota)

# ---------- 2) rodar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/install/sentinel ./

# não roda como root (boa prática de segurança)
RUN useradd --system --no-create-home sentinel
USER sentinel

# a hospedagem informa a porta pela variável PORT; o Sentinel já lê ela
ENV PORT=8080
EXPOSE 8080

# usa até 75% da memória do servidor para o Java
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

CMD ["bin/sentinel", "--web"]
