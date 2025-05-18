
- taichubackend-starter: 启动器
- taichubackend-gateway: controller
- taichubackend-application: 服务层，业务逻辑
- taichubackend-domain: 领域层，包含领域模型和领域能力接口
- taichubackend-infra: 基础设施层，调用DB、文件服务等外部服务依赖


# 开发记录
## 创建docker中的数据库
step1: 创建 docker-compose.yml 文件，内容如下
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8
    container_name: mysql_demo
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: demo_db_taichu
      MYSQL_USER: demo
      MYSQL_PASSWORD: demo123
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:

```

step2: 启动 MySQL 容器
```bash
docker-compose up -d
```

step3: 测试数据库连接..."
``` bash
docker exec -it mysql_demo mysql -udemo -pdemo123 -e "SHOW DATABASES;" 2>/dev/null
```

step4: 调整 application.yml 数据源
```properties
# database
spring.datasource.url=jdbc:mysql://localhost:3306/demo_db_taichu?useSSL=false&serverTimezone=UTC
spring.datasource.username=demo
spring.datasource.password=demo123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

## 清空数据库

### 1. **删除 MySQL 容器和卷**

最简单的方法是删除 MySQL 容器和对应的 Docker 卷，这样可以完全清空数据库数据。

#### 步骤：

1. 停止并删除容器：

   ```bash
   docker-compose down
   ```

2. 删除 MySQL 卷 `mysql_data`：

   ```bash
   docker volume rm <volume_name>
   ```

   在你的例子中，卷名是 `mysql_data`，所以执行：

   ```bash
   docker volume rm mysql_data
   ```

3. 重新启动容器：

   ```bash
   docker-compose up -d
   ```

这样会删除原来存储的数据并重新创建数据库。

### 2. **通过 MySQL 命令清空数据库**

你也可以通过 MySQL 命令来清空数据库中的所有数据，而不是删除整个容器。

#### 步骤：

1. 进入 MySQL 容器：

   ```bash
   docker exec -it mysql_demo mysql -udemo -pdemo123
   ```

2. 选择要清空的数据库：

   ```sql
   USE demo_db_taichu;
   ```

3. 删除所有表的数据：

   ```sql
   DELETE FROM <table_name>;
   ```

   如果你想清空所有表的数据，可以使用以下命令删除所有表：

   ```sql
   SET FOREIGN_KEY_CHECKS = 0;
   -- 删除所有表
   DROP DATABASE demo_db_taichu;
   CREATE DATABASE demo_db_taichu;
   SET FOREIGN_KEY_CHECKS = 1;
   ```

   这会删除整个数据库然后重新创建它，从而清空所有表。

### 3. **删除数据库中的所有表**

如果你不想删除数据库本身，只想清空所有表的数据，你可以使用以下 SQL 语句：

1. 连接到 MySQL 容器：

   ```bash
   docker exec -it mysql_demo mysql -udemo -pdemo123
   ```

2. 选择要操作的数据库：

   ```sql
   USE demo_db_taichu;
   ```

3. 生成删除所有表的命令（这会删除所有表，但保留数据库）：

   ```sql
   SET FOREIGN_KEY_CHECKS = 0;
   -- 删除所有表
   SET @tables = (SELECT GROUP_CONCAT(table_name) FROM information_schema.tables WHERE table_schema = 'demo_db_taichu');
   SET @query = CONCAT('DROP TABLE ', @tables);
   PREPARE stmt FROM @query;
   EXECUTE stmt;
   DEALLOCATE PREPARE stmt;
   SET FOREIGN_KEY_CHECKS = 1;
   ```

这种方法会删除数据库中的所有表，但不删除数据库本身。
