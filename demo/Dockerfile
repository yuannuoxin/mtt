FROM eclipse-temurin:17-jdk-alpine

WORKDIR /build

# 安装 Maven
RUN apk add --no-cache maven

# 复制所有源代码
COPY . .

# 在根目录一次性构建所有项目
RUN mvn clean package -DskipTests -B

# --- 运行阶段 ---
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# 只复制打包好的 JAR 文件
COPY --from=0 /build/demo/target/demo-1.0.0-SNAPSHOT.jar ./app.jar

# 运行应用
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
