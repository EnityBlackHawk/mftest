SELECT * FROM flight WHERE number = 'FL0018';
---
select p.id, p.first_name, p.last_name, b.flight, b.seat from passenger p join booking b on b.passenger = p.id;
---
select f."number", p.first_name, p.last_name, p.passport_number, b.seat from flight f join booking b on b.flight = f.number join passenger p on b.passenger = p.id where f."number" = 'FL0805';
---
select f."number", a.name as airport_from, a.city as city_from, f.arrival_time_scheduled, f.arrival_time_actual
from flight f join airport a on a.id = f.airport_from
where f.airport_to = '890' order by f.departure_time_scheduled;
---
select count(*), f.airport_to from flight f group by f.airport_to order by count(*);