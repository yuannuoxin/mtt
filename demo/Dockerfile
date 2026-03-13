FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# 安装 Maven
RUN apk add --no-cache maven

# 复制 pom.xml 文件以利用 Docker 缓存层
COPY pom.xml ./
COPY demo/pom.xml ./demo/
COPY mtd-common/pom.xml ./mtd-common/
COPY mtd-common/mtd-common-bom/pom.xml ./mtd-common/mtd-common-bom/
COPY mtd-common/mtd-common-core/pom.xml ./mtd-common/mtd-common-core/
COPY mtd-common/mtd-common-exception/pom.xml ./mtd-common/mtd-common-exception/
COPY mtd-common/mtd-common-mybatis/pom.xml ./mtd-common/mtd-common-mybatis/
COPY mtd-common/mtd-common-redis/pom.xml ./mtd-common/mtd-common-redis/
COPY mtd-common/mtd-common-satoken/pom.xml ./mtd-common/mtd-common-satoken/
COPY mtd-common/mtd-common-web/pom.xml ./mtd-common/mtd-common-web/

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY demo/src ./demo/src
COPY mtd-common/mtd-common-core/src ./mtd-common/mtd-common-core/src
COPY mtd-common/mtd-common-exception/src ./mtd-common/mtd-common-exception/src
COPY mtd-common/mtd-common-mybatis/src ./mtd-common/mtd-common-mybatis/src
COPY mtd-common/mtd-common-redis/src ./mtd-common/mtd-common-redis/src
COPY mtd-common/mtd-common-satoken/src ./mtd-common/mtd-common-satoken/src
COPY mtd-common/mtd-common-web/src ./mtd-common/mtd-common-web/src

# 构建项目
RUN mvn clean package -DskipTests -B -f demo/pom.xml

# 运行应用
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/demo/target/demo-1.0.0-SNAPSHOT.jar"]
