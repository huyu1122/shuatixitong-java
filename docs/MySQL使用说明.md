# MySQL 本地开发环境说明

本项目的 MySQL 为**免安装版**（ZIP 解压 + 手动初始化），无需管理员权限。

---

## 安装位置

| 项目 | 路径 |
| --- | --- |
| 安装目录 | `%LOCALAPPDATA%\Programs\mysql\mysql-8.0.28-winx64` |
| 数据目录 | `%LOCALAPPDATA%\Programs\mysql\data` |
| 配置文件 | `%LOCALAPPDATA%\Programs\mysql\my.ini` |
| 版本 | MySQL 8.0.28 |
| 端口 | 3306 |

---

## 启动 / 停止

MySQL **未注册为 Windows 服务**，需要手动启动。

### 启动

双击项目根目录的 **`start-mysql.cmd`**，或在命令行执行：

```cmd
"%LOCALAPPDATA%\Programs\mysql\mysql-8.0.28-winx64\bin\mysqld.exe" --defaults-file="%LOCALAPPDATA%\Programs\mysql\my.ini"
```

启动后窗口会占用前台，关闭窗口即停止服务。

### 停止

在启动窗口按 `Ctrl + C`，或执行：

```cmd
"%LOCALAPPDATA%\Programs\mysql\mysql-8.0.28-winx64\bin\mysqladmin.exe" -u root -proot shutdown
```

---

## 账号信息

| 项 | 值 |
| --- | --- |
| 用户名 | `root` |
| 密码 | `root` |
| 数据库名 | `quiz_system` |

> **注意**：这是本地开发用的弱密码。若部署到服务器，请务必修改。

---

## 连接

### 命令行

```cmd
"%LOCALAPPDATA%\Programs\mysql\mysql-8.0.28-winx64\bin\mysql.exe" -u root -proot quiz_system
```

### 图形化工具

用 Navicat / DBeaver / MySQL Workbench 连接：

- 主机：`127.0.0.1`
- 端口：`3306`
- 用户名：`root`
- 密码：`root`
- 数据库：`quiz_system`

---

## 重新导入数据库

若数据被改乱，想恢复初始状态：

```cmd
set MYSQL=%LOCALAPPDATA%\Programs\mysql\mysql-8.0.28-winx64\bin\mysql.exe
"%MYSQL%" -u root -proot --default-character-set=utf8mb4 -e "source docs/sql/schema.sql"
"%MYSQL%" -u root -proot --default-character-set=utf8mb4 -e "source docs/sql/data-questions.sql"
```

> ⚠️ **不要用 PowerShell 管道导入**（如 `Get-Content xxx.sql | mysql`）。
> PowerShell 会把 UTF-8 中文转成乱码，导致 SQL 语法错误。
> 必须用 `source` 命令让 mysql 客户端自己读文件。

`schema.sql` 开头有 `DROP DATABASE IF EXISTS quiz_system`，
所以重复执行会**清空所有数据**（用户、答题记录、错题本全部丢失）。

---

## 常见问题

**Q：端口 3306 被占用？**

可能已启动了另一个 MySQL 实例。检查：

```cmd
netstat -ano | findstr :3306
```

结束占用进程，或修改 `my.ini` 中的 `port` 并同步修改
`application.yml` 的 `jdbc:mysql://localhost:某端口/...`。

**Q：启动报「另一个进程正在使用数据目录」？**

说明已有 mysqld 在运行。先停止旧进程：

```cmd
taskkill /IM mysqld.exe /F
```

**Q：忘记 root 密码？**

1. 停止 MySQL
2. 用跳过权限检查方式启动：
   ```cmd
   mysqld.exe --defaults-file="...\my.ini" --skip-grant-tables
   ```
3. 另开窗口执行：
   ```cmd
   mysql.exe -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root'; FLUSH PRIVILEGES;"
   ```
4. 重启 MySQL

**Q：中文显示为乱码？**

连接时加 `--default-character-set=utf8mb4`。建库语句已指定
`utf8mb4` 字符集，数据本身没问题，通常是客户端字符集不对。

**Q：数据目录损坏怎么办？**

删除 `%LOCALAPPDATA%\Programs\mysql\data` 目录后重新初始化：

```cmd
"%LOCALAPPDATA%\Programs\mysql\mysql-8.0.28-winx64\bin\mysqld.exe" ^
  --defaults-file="%LOCALAPPDATA%\Programs\mysql\my.ini" --initialize-insecure
```

然后重新导入 `schema.sql` 与 `data-questions.sql`。
**注意：这会丢失所有用户数据。**