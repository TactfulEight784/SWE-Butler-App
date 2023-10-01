CREATE TABLE Users (
    ID INTEGER PRIMARY KEY AUTOINCREMENT,
    Name TEXT NOT NULL,
    Email TEXT NOT NULL,
    Phone TEXT NOT NULL,
    CalendarReminders TEXT NOT NULL,
    CalendarDate TEXT NOT NULL
);
INSERT INTO Users (Name, Email, Phone, CalendarReminders, CalendarDate)
VALUES ('Dylan Brown', 'Dylan.Brown@gmail.com', '123-456-7890', 'Data Mining HW', '10-1-2023');

SELECT * FROM Users;

--Additional functions:
--ALTER TABLE Users
--ADD COLUMN CalendarDate TEXT;

--UPDATE Users
--SET CalendarDate = '2023-10-01' 
--WHERE ID = 1;
