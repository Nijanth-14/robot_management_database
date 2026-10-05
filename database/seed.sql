-- Optional demonstration data. Run once after schema.sql.
USE robot_monitoring;

START TRANSACTION;

INSERT INTO robot (robot_id, robot_name, robot_type, location, status, battery_level)
VALUES
    ('R001', 'Robo-1', 'Transport', 'Zone A', 'Available', 90),
    ('R002', 'Robo-2', 'Inspection', 'Zone B', 'Busy', 65),
    ('R003', 'Robo-3', 'Cleaning', 'Zone C', 'Charging', 30);

INSERT INTO task (task_id, task_name, description, priority, task_status, robot_id)
VALUES
    ('T001', 'Deliver supplies', 'Move supplies from Zone A to Zone B.',
        'Medium', 'Pending', 'R001'),
    ('T002', 'Inspect Zone B', 'Check the assigned inspection route.',
        'High', 'In Progress', 'R002'),
    ('T003', 'Clean Zone C', 'Clean the floor after a robot becomes available.',
        'Low', 'Pending', NULL);

COMMIT;
