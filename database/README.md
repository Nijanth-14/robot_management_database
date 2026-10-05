# Robot monitoring database

MySQL database for the Multi-Robot Coordination Monitoring System in the project presentation. Requires **MySQL 8.0.16 or newer** so CHECK constraints are enforced. Use MySQL's default strict SQL mode to reject invalid or truncated input.

## Files

- `schema.sql`: creates the database, tables, constraints and indexes.
- `seed.sql`: optional demo robots and tasks, including the three robots shown in the presentation.
- `queries.sql`: CRUD templates for Java JDBC PreparedStatements. This is a reference file, not an executable setup script.

## Setup

Install and start MySQL Server if it is not already available. Open its command-line client using an account allowed to create this database:

```powershell
mysql -u root -p
```

At the `mysql>` prompt, run:

```sql
SOURCE D:/Mintu proj/database/schema.sql;
SOURCE D:/Mintu proj/database/seed.sql;
SHOW TABLES FROM robot_monitoring;
SELECT * FROM robot_monitoring.robot;
SELECT * FROM robot_monitoring.task;
```

Alternatively, open and execute `schema.sql`, followed by `seed.sql`, in MySQL Workbench. Successful setup produces two tables and three demo records in each table.

Run the schema on a fresh database and the seed only once. Neither script drops or overwrites existing records. Stop if a script reports an error; do not continue to the next script. If seed execution fails within an interactive session, run `ROLLBACK;` before retrying. MySQL DDL commits implicitly, so a partially failed schema setup is not automatically rolled back.

## Data model

One robot can have many tasks. Each task belongs to at most one robot and may be unassigned.

| Table | Columns |
| --- | --- |
| `robot` | `robot_id` (primary key), `robot_name`, `robot_type`, `location`, `status`, `battery_level`, creation/update timestamps |
| `task` | `task_id` (primary key), `task_name`, `description`, `priority`, `task_status`, nullable `robot_id` (foreign key), creation/update timestamps |

Robot IDs and task IDs are supplied by the application, such as `R001` and `T001`. They are unique within their respective tables. The selected collation compares IDs without case sensitivity, so `R001` and `r001` refer to the same ID.

Rules implemented in the database:

- Battery level is an integer from 0 to 100.
- IDs, robot name/type/location and task name cannot be blank.
- Robot statuses: `Available`, `Busy`, `Charging`, `Offline`, `Maintenance`.
- Task priorities: `Low`, `Medium`, `High`.
- Task statuses: `Pending`, `In Progress`, `Completed`, `Cancelled`.
- An `In Progress` task must have an assigned robot.
- An assigned robot must exist.
- Deleting a robot with task references fails. Reassign tasks, explicitly unassign them, or delete them first. Change an in-progress task's status before unassigning it, or update both fields in one statement.

The additional statuses, nullable assignment, timestamps and deletion rule are implementation choices extending the presentation. Location is a text label, as in its Zone A/B/C examples.

## Java integration

Use MySQL Connector/J and this JDBC URL for a local server:

```text
jdbc:mysql://localhost:3306/robot_monitoring
```

Keep the username and password in local configuration or environment variables. Use a separate application account with only `SELECT`, `INSERT`, `UPDATE` and `DELETE` permissions on `robot_monitoring`, rather than the setup account.

Use `PreparedStatement` and bind the parameters in `queries.sql`. For an unassigned task, use `setNull(parameterIndex, java.sql.Types.VARCHAR)`. Check the affected-row count for update/delete operations. The application can load robots and use Java Streams for the filtering and sorting requested in the presentation.

Robot status and task status are separate fields. The database does not automatically mark a robot Busy when a task starts or Available when one completes. Implement that coordination in a JDBC transaction, checking other active tasks before making the robot Available. This schema permits multiple tasks per robot and does not enforce one active task at a time.

This database stores reported state; receiving telemetry from physical robots will require a later integration.

## Validation status

The schema and seed scripts were successfully executed on an isolated MySQL 8.4.11 instance, using the separate database name `robot_monitoring_test`. JDBC integration tests verified CRUD operations, foreign keys, duplicate IDs and CHECK constraints. Your application database was not modified by these tests.
