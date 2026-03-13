FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# 安装 Maven
RUN apk add --no-cache maven

# 复制 pom.xml 文件以利用 Docker 缓存层
COPY pom.xml ./
COPY mtd-common/pom.xml ./mtd-common/
COPY mtd-common/mtd-common-bom/pom.xml ./mtd-common/mtd-common-bom/
COPY demo/pom.xml ./demo/

# 安装父 pom 和 mtd-common-bom 到本地 Maven 仓库
RUN mvn clean install -DskipTests -B -N -f pom.xml && \
    mvn clean install -DskipTests -B -N -f mtd-common/pom.xml && \
    mvn clean install -DskipTests -B -N -f mtd-common/mtd-common-bom/pom.xml

# 下载依赖
RUN mvn dependency:go-offline -B -f demo/pom.xml

# 复制源代码
COPY demo/src ./demo/src
COPY mtd-common/mtd-common-core/src ./mtd-common/mtd-common-core/src
COPY mtd-common/mtd-common-exception/src ./mtd-common/mtd-common-exception/src
COPY mtd-common/mtd-common-mybatis/src ./mtd-common/mtd-common-mybatis/src
COPY mtd-common/mtd-common-redis/src ./mtd-common/mtd-common-redis/src
COPY mtd-common/mtd-common-satoken/src ./mtd-common/mtd-common-satoken/src
COPY mtd-common/mtd-common-web/src ./mtd-common/mtd-common-web/src

# 安装所有 mtd-common 模块
RUN mvn clean install -DskipTests -B -f mtd-common/mtd-common-core/pom.xml && \
    mvn clean install -DskipTests -B -f mtd-common/mtd-common-exception/pom.xml && \
    mvn clean install -DskipTests -B -f mtd-common/mtd-common-mybatis/pom.xml && \
    mvn clean install -DskipTests -B -f mtd-common/mtd-common-redis/pom.xml && \
    mvn clean install -DskipTests -B -f mtd-common/mtd-common-satoken/pom.xml && \
    mvn clean install -DskipTests -B -f mtd-common/mtd-common-web/pom.xml

# 构建 demo 项目
RUN mvn clean package -DskipTests -B -f demo/pom.xml

# 运行应用
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/demo/target/demo-1.0.0-SNAPSHOT.jar"]
