CREATE TABLE manufacturer (
                              id INT PRIMARY KEY,
                              name TEXT
);

CREATE TABLE airline(
                        id INT PRIMARY KEY,
                        name TEXT
);

CREATE TABLE aircraft (
                          id INT PRIMARY KEY,
                          type TEXT UNIQUE,
                          airline INT REFERENCES airline(id),
                          manufacturer INT REFERENCES manufacturer(id),
                          registration TEXT UNIQUE,
                          max_passengers INT
);

CREATE TABLE airport (
                         id VARCHAR(3) PRIMARY KEY,
                         name TEXT,
                         city TEXT,
                         country TEXT
);

CREATE TABLE flight (
                        number TEXT PRIMARY KEY,
                        airport_from varchar(3) REFERENCES airport(id),
                        airport_to varchar(3) REFERENCES airport(id),
                        departure_time_scheduled TIMESTAMP,
                        departure_time_actual TIMESTAMP,
                        arrival_time_scheduled TIMESTAMP,
                        arrival_time_actual TIMESTAMP,
                        gate INT,
                        aircraft INT REFERENCES aircraft(id),
                        connects_to TEXT REFERENCES flight(number)
);

CREATE TABLE passenger (
                           id INT PRIMARY KEY,
                           first_name TEXT,
                           last_name TEXT,
                           passport_number TEXT UNIQUE
);

CREATE TABLE booking (
                         id INT PRIMARY KEY,
                         flight TEXT REFERENCES flight(number),
                         passenger INT REFERENCES passenger(id),
                         seat TEXT
);

-- SELECT p.first_name, p.passport_number, f.number FROM passenger AS p JOIN booking AS b on passenger.id = b.passenger JOIN flight AS f on b.flight = f.number; JOIN airport AS a on f.airport_to = a.id;