-- JDBC PreparedStatement templates: ? marks a bound parameter.
-- Reference file only; do not execute this file in the mysql console.

-- CREATE robot: six parameters in column order.
INSERT INTO robot (robot_id, robot_name, robot_type, location, status, battery_level)
VALUES (?, ?, ?, ?, ?, ?);

-- READ robots for the JavaFX TableView and Java Streams processing.
SELECT robot_id, robot_name, robot_type, location, status, battery_level
FROM robot
ORDER BY robot_id;

-- READ one robot.
SELECT robot_id, robot_name, robot_type, location, status, battery_level
FROM robot
WHERE robot_id = ?;

-- UPDATE robot; robot ID is the final parameter.
UPDATE robot
SET robot_name = ?, robot_type = ?, location = ?, status = ?, battery_level = ?
WHERE robot_id = ?;

-- DELETE robot. Fails if any task still references the robot.
DELETE FROM robot WHERE robot_id = ?;

-- CREATE a task. Bind SQL NULL for an unassigned robot.
INSERT INTO task (task_id, task_name, description, priority, task_status, robot_id)
VALUES (?, ?, ?, ?, ?, ?);

-- READ tasks with the assigned robot's name, including unassigned tasks.
SELECT t.task_id, t.task_name, t.description, t.priority, t.task_status,
       t.robot_id, r.robot_name
FROM task AS t
LEFT JOIN robot AS r ON r.robot_id = t.robot_id
ORDER BY FIELD(t.priority, 'High', 'Medium', 'Low'), t.task_id;

-- UPDATE task, including assignment and progress.
UPDATE task
SET task_name = ?, description = ?, priority = ?, task_status = ?, robot_id = ?
WHERE task_id = ?;

-- DELETE task.
DELETE FROM task WHERE task_id = ?;

-- READ assigned tasks for a selected robot.
SELECT task_id, task_name, priority, task_status
FROM task
WHERE robot_id = ?
ORDER BY task_id;
