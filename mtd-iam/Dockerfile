# 构建阶段
FROM maven:3.9.5-sapmachine-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml
COPY pom.xml .

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 打包应用
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 创建日志目录
RUN mkdir -p /app/logs/mtd-iam

# 从构建阶段复制 JAR 包
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8081

# JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
