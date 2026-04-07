# 베이스 이미지
FROM amazoncorretto:17

# 작업 디렉토리 설정
WORKDIR /app

# 프로젝트 파일 복사
COPY . /app

# 프로젝트 정보를 환경 변수로 설정
ENV PROJECT_NAME=discodeit
ENV PROJECT_VERSION=1.2-M8

# JVM 옵션을 환경 변수로 설정
ENV JVM_OPTS=""

# Gradle Wrapper를 사용하여 빌드
RUN chmod +x ./gradlew
RUN ./gradlew build --no-daemon

# 80 포트를 노출하도록 설정
EXPOSE 80

# 애플리케이션 실행 명령어 설정
CMD ["sh", "-c", "exec java ${JVM_OPTS} -jar /app/build/libs/${PROJECT_NAME}-${PROJECT_VERSION}.jar"]