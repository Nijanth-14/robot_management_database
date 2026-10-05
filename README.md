# Multi-Robot Coordination Monitoring System

Java 17+ desktop application for the project presentation. JavaFX provides the dashboard, MySQL stores robot/task records, JDBC repositories implement CRUD, and Java Streams support filtering and sorting.

## Run the desktop application

Your existing MySQL service must be running, with `database/schema.sql` and optionally `database/seed.sql` already applied. Do not rerun those scripts if setup has succeeded.

Open a terminal in `D:\Mintu proj` and run:

```powershell
.\build.cmd
.\run.cmd
```

These commands also work in Command Prompt. `build.cmd` compiles the Java source, runs tests, and packages the application, JavaFX and JDBC driver into `target/robot-monitoring.jar`. `run.cmd` opens the desktop login window. Enter your MySQL password and click **Connect to database**. With the original sample data, expect three robots and three tasks. The current packaged JAR includes Windows JavaFX native libraries; rebuild on another operating system for its native libraries.

### Use the dashboard

- **Robots:** enter a unique ID, name, type, location, status and battery level, then click **Add robot**. Select a table row to edit it, then click **Save changes**. IDs stay fixed while editing.
- **Tasks:** enter the task details, priority and status. Choose a robot or leave it Unassigned. In-progress tasks require a robot. Select a row to update it.
- **Clear / New** resets a form so you can add another record. If lower fields are out of view, scroll within the form; action buttons stay visible at the bottom.
- **Delete** asks for confirmation. Robots with referenced tasks cannot be deleted until you reassign or remove those tasks.
- Search and status filters narrow each table. Click column headings to sort. Hover a shortened cell to read the full text.
- **Refresh** loads current database values. The optional **Refresh every 10s** checkbox reloads automatically and preserves in-progress form edits. Saving an edit replaces that record's editable fields, so clear and reselect a row if you want the latest external changes before editing.
- Summary totals cover the entire database, independently of table filters. Low battery means 20% or less; active tasks are In Progress.

Robot status is manually managed on the Robots tab. Changing task status does not automatically change robot status. Monitoring shows stored database values; physical robot telemetry and autonomous movement are outside this project's supplied scope.

The previous read-only terminal check remains available through `run.cmd --console`.

The scripts use `JAVA_HOME` or Java on PATH. On this machine they can also use the Java 21 JDK bundled with Android Studio. Maven is available locally under `.tools/apache-maven-3.9.11`; downloaded dependencies stay in `.m2`. These folders are ignored by Git. On another machine, install JDK 17+ and Maven, then run `mvn package` and `java -jar target/robot-monitoring.jar`.

Rebuild after editing Java source. For `--console`, use a terminal rather than an IDE Output pane, because hidden password input requires an interactive console.

## Local configuration

Defaults are `jdbc:mysql://localhost:3306/robot_monitoring` and user `root`, matching the initial local setup. Edit the connection fields on the login screen, or copy `config/database.properties.example` to `config/database.properties` and edit it. This local file is ignored by Git. The desktop login does not save your password to disk.

Environment variables `DB_URL`, `DB_USER` and `DB_PASSWORD` override the file. Do not put passwords in source code or command-line arguments. Java `.properties` files treat backslashes as escapes; prefer the prompt for passwords. Use an application account with only SELECT/INSERT/UPDATE/DELETE privileges when moving beyond initial local setup.

Connections require TLS by default. For deployment to another computer, configure a trusted server certificate and `sslMode=VERIFY_IDENTITY` in the JDBC URL. Connection and socket timeouts are 5 and 10 seconds respectively.

## Backend API

| Component | Purpose |
| --- | --- |
| `model.Robot`, `model.Task` | Immutable Java records with validation matching the schema |
| `model.RobotStatus`, `model.TaskStatus`, `model.Priority` | Typed values mapped to MySQL enum strings |
| `db.DatabaseConfig`, `db.Database` | Local credentials and JDBC connections |
| `repository.RobotRepository` | `create`, `findAll`, `findById`, `update`, `delete` |
| `repository.TaskRepository` | Same CRUD methods plus `findByRobotId` |
| `service.RobotSearch` | Search by ID/name/type/location, filter by status, sort by name |
| `ui.MonitorApp`, `ui.Dashboard` | Connection screen and dashboard with background database operations |
| `ui.RobotPane`, `ui.TaskPane` | Forms, tables, validation, search and CRUD controls |
| `ConsoleApp` | Optional read-only connection and record listing check |

Example of backend usage (after collecting the password):

```java
Database database = new Database(config);
RobotRepository robots = new RobotRepository(database);
robots.create(new Robot("R004", "Robo-4", "Transport", "Zone D", RobotStatus.AVAILABLE, 100));
List<Robot> available = RobotSearch.filter(robots.findAll(), "Zone", RobotStatus.AVAILABLE);
```

Every repository operation opens and closes its own connection. SQL parameters use PreparedStatements. `findById` returns Optional, and update/delete return false when no row matches. IDs cannot be changed through update. The UI maps database exceptions to actionable messages. Robot status and task status remain independently editable, matching the schema.

## Tests

`build.cmd` runs model/config/search tests. MySQL integration tests skip unless `DB_TEST_URL` is set. They require a separate schema named **robot_monitoring_test** and will refuse other schema names.

To prepare a dedicated test database, use a copy of `database/schema.sql` with the database name replaced by `robot_monitoring_test`. Never point tests at your application database. Integration tests create unique temporary records and clean up only those records.

PowerShell example after preparing the test schema:

```powershell
$env:DB_TEST_URL = 'jdbc:mysql://localhost:3306/robot_monitoring_test'
$env:DB_TEST_USER = 'your_test_user'
# Set DB_TEST_PASSWORD privately in your local environment.
.\build.cmd
```

Tests cover CRUD persistence, assignments/unassignments, missing records, duplicate IDs, referenced-robot deletion, parameterized text containing SQL punctuation, and MySQL CHECK constraints.

The desktop workflow test is opt-in with `RUN_UI_TESTS=true` and requires a graphical Windows session and an isolated test server with the test root account's empty password. It opens a test window, connects, exercises robot/task creation, updates, validation, filtering and confirmed deletion, and renders screenshots in `target`. Keep this temporary test server restricted to localhost. Backend integration tests support separate test credentials as described above.

Validated locally with Java 21 targeting Java 17, Maven 3.9.11 and an isolated MySQL 8.4.11 server. The application database and its credentials were not used for testing.

## Troubleshooting

- Error 1045: incorrect username/password or account permissions.
- Error 1049: database missing or database name incorrect.
- Error 1146: tables missing; check that schema setup completed.
- Connection failure / SQL state starting with 08: verify MySQL service, port and TLS configuration.
- Password prompt unavailable: use `run.cmd` in the terminal, or supply local configuration.
- First build cannot download dependencies: check internet access to Maven Central.

Build dependencies: [Apache Maven](https://maven.apache.org/) and [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/en/).
