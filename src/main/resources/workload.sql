SELECT
    f.number AS flight_number,
    f.departure_time_scheduled,
    f.departure_time_actual,
    f.arrival_time_scheduled,
    f.arrival_time_actual,
    a.name AS airline_name,
    ac.type AS aircraft_type,
    ac.registration AS aircraft_registration,
    ac.max_passengers,
    m.name AS manufacturer_name
FROM flight f
         JOIN aircraft ac ON f.aircraft = ac.id
         JOIN airline a ON ac.airline = a.id
         JOIN manufacturer m ON ac.manufacturer = m.id
WHERE f.airport_from = 'JFK' AND f.departure_time_scheduled BETWEEN '2024-01-01' AND '2024-12-31';
---
SELECT
    f.number AS flight_number,
    f.departure_time_scheduled,
    f.departure_time_actual,
    f.arrival_time_scheduled,
    f.arrival_time_actual,
    a.name AS airline_name,
    ac.type AS aircraft_type,
    ac.registration AS aircraft_registration,
    ac.max_passengers,
    m.name AS manufacturer_name
FROM flight f
         JOIN aircraft ac ON f.aircraft = ac.id
         JOIN airline a ON ac.airline = a.id
         JOIN manufacturer m ON ac.manufacturer = m.id
WHERE a.name = 'American Airlines';
---
SELECT
    f1.number AS flight_number,
    f1.departure_time_scheduled,
    f1.arrival_time_scheduled,
    af.name AS airport_from,
    at.name AS airport_to,
    f2.number AS connecting_flight,
    af2.name AS connecting_airport
FROM flight f1
         JOIN airport af ON f1.airport_from = af.id
         JOIN airport at ON f1.airport_to = at.id
    LEFT JOIN flight f2 ON f1.connects_to = f2.number
    LEFT JOIN airport af2 ON f2.airport_from = af2.id
WHERE f1.departure_time_scheduled > '2024-05-01' AND f1.aircraft = 1;
---
SELECT
    af.id AS airport_code,
    af.name AS airport_name,
    af.city AS airport_city,
    af.country AS airport_country,
    f.number AS flight_number,
    f.departure_time_scheduled,
    f.arrival_time_scheduled,
    f.connects_to AS connecting_flight
FROM airport af
         JOIN flight f ON af.id = f.airport_from
WHERE af.city = 'New York' OR af.country = 'USA';
---
SELECT
    p.first_name,
    p.last_name,
    p.passport_number,
    b.seat,
    f.number AS flight_number,
    f.departure_time_scheduled,
    af.name AS airport_from,
    at.name AS airport_to
FROM passenger p
         JOIN booking b ON p.id = b.passenger
         JOIN flight f ON b.flight = f.number
         JOIN airport af ON f.airport_from = af.id
         JOIN airport at ON f.airport_to = at.id
WHERE f.number = 'AA100' AND f.departure_time_scheduled = '2024-10-10 08:00:00';