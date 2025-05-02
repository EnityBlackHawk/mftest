# Migration Report
**Date:** Thu Apr 24 09:24:57 BRT 2025 
## Inputs:
| Parameter           |              Value |
|---------------------|               :--: |
| allowReferences     |               true |
| framework           |        SPRING_DATA |
| referenceOnly       |              false |
| useMarkdown         |               true |
| migrationPreference | PREFER_PERFORMANCE |

### Relational Schema
```sql
CREATE TABLE date ( 
	dateid SMALLINT PRIMARY KEY,
	caldate DATE,
	day CHAR,
	week SMALLINT,
	month CHAR,
	qtr CHAR,
	year SMALLINT,
	holiday BIT
);
CREATE TABLE event ( 
	eventid INTEGER PRIMARY KEY,
	venueid SMALLINT REFERENCES venue(venueid),
	catid SMALLINT REFERENCES eventcategory(catid),
	dateid SMALLINT REFERENCES date(dateid),
	eventname VARCHAR,
	starttime TIMESTAMP
);
CREATE TABLE eventcategory ( 
	catid SMALLINT PRIMARY KEY,
	catgroup VARCHAR,
	catname VARCHAR,
	catdesc VARCHAR
);
CREATE TABLE listing ( 
	listid INTEGER PRIMARY KEY,
	sellerid INTEGER REFERENCES users(userid),
	eventid INTEGER REFERENCES event(eventid),
	dateid SMALLINT REFERENCES date(dateid),
	numtickets SMALLINT,
	priceperticket NUMERIC,
	totalprice NUMERIC,
	listtime TIMESTAMP
);
CREATE TABLE sales ( 
	salesid INTEGER PRIMARY KEY,
	listid INTEGER,
	sellerid INTEGER,
	buyerid INTEGER,
	eventid INTEGER,
	dateid SMALLINT,
	qtysold SMALLINT,
	pricepaid NUMERIC,
	commission NUMERIC,
	saletime TIMESTAMP
);
CREATE TABLE users ( 
	userid INTEGER PRIMARY KEY,
	username CHAR,
	firstname VARCHAR,
	lastname VARCHAR,
	city VARCHAR,
	state CHAR,
	email VARCHAR,
	phone CHAR,
	likesports BIT,
	likeconcerts BIT,
	likecomedy BIT,
	liketheater BIT,
	likemovies BIT,
	likefestivals BIT,
	likegaming BIT,
	liketravel BIT,
	liketech BIT,
	likefood BIT
);
CREATE TABLE venue ( 
	venueid SMALLINT PRIMARY KEY,
	venuename VARCHAR,
	venuecity VARCHAR,
	venuestate CHAR,
	venueseats INTEGER
);

```

### Cardinality Information
```json
[{
  "source": "event",
  "target": "venue",
  "min": 1,
  "max": 1,
  "avg": 1.0
}, {
  "source": "event",
  "target": "eventcategory",
  "min": 1,
  "max": 8,
  "avg": 3.3333333333333335
}, {
  "source": "event",
  "target": "date",
  "min": 1,
  "max": 1,
  "avg": 1.0
}, {
  "source": "listing",
  "target": "users",
  "min": 1,
  "max": 1,
  "avg": 1.0
}, {
  "source": "listing",
  "target": "event",
  "min": 1,
  "max": 1,
  "avg": 1.0
}, {
  "source": "listing",
  "target": "date",
  "min": 1,
  "max": 1,
  "avg": 1.0
}]
```

### Workload
- **Used 25% of the time:**
```sql
SELECT e.eventname, e.starttime, v.venuecity, v.venuestate
FROM event e
JOIN venue v ON e.venueid = v.venueid
WHERE e.starttime > '2005-01-01 00:00'
  AND v.venuecity = 'San Francisco';
```

 
- **Used 15% of the time:**
```sql
SELECT ec.catname, e.eventname, e.starttime
FROM event e
JOIN eventcategory ec ON e.catid = ec.catid
WHERE ec.catname = 'Musicals';
```

 
- **Used 35% of the time:**
```sql
SELECT l.listid, u.username, l.numtickets, l.priceperticket, l.listtime
FROM listing l
JOIN users u ON l.sellerid = u.userid
WHERE l.eventid = 123
ORDER BY l.priceperticket ASC;
```

 
- **Used 20% of the time:**
```sql
SELECT d.month, d.year, SUM(s.qtysold) AS total_tickets, SUM(s.pricepaid) AS revenue
FROM sales s
JOIN date d ON s.dateid = d.dateid
GROUP BY d.month, d.year
ORDER BY d.year DESC, d.month DESC;
```

 
- **Used 15% of the time:**
```sql
SELECT DISTINCT e.eventname, e.starttime, ec.catname
FROM users u
JOIN listing l ON u.userid = l.sellerid
JOIN event e ON l.eventid = e.eventid
JOIN eventcategory ec ON e.catid = ec.catid
WHERE u.userid = 395
  AND (
    (u.likesports = true AND ec.catgroup = 'Sports') OR
    (u.likeconcerts = true AND ec.catgroup = 'Concerts') OR
    (u.liketheater = true AND ec.catgroup = 'Shows')
  )
  AND e.starttime > '2005-01-01 00:00';
```

 
- **Used 10% of the time:**
```sql
SELECT e.eventname, SUM(s.qtysold) AS total_tickets, 
       SUM(s.pricepaid) AS total_revenue,
       SUM(s.pricepaid) - SUM(s.commission) AS net_revenue
FROM sales s
JOIN event e ON s.eventid = e.eventid
GROUP BY e.eventname
ORDER BY net_revenue DESC;
```

 
## Model generation
**LLM Model:** gpt-4o-mini 
### LLM Explanation
The schema was designed to optimize for frequently accessed data while considering the cardinality of relationships. Embedded documents were used for entities that are often retrieved together, such as listings with user information, while references were used for entities with higher cardinality and less frequent access, like events and categories. 
### JSON Schema
```json
[
  {
    "type": "object",
    "isId": false,
    "title": "date",
    "description": "Collection of dates",
    "isAbstract": false,
    "reference": false,
    "properties": {
      "caldate": {
        "type": "string",
        "isId": false,
        "description": "Calendar date",
        "isAbstract": false,
        "column": "caldate",
        "table": "date",
        "reference": false,
        "projection": "*"
      },
      "week": {
        "type": "number",
        "isId": false,
        "description": "Week of the year",
        "isAbstract": false,
        "column": "week",
        "table": "date",
        "reference": false,
        "projection": "*"
      },
      "month": {
        "type": "string",
        "isId": false,
        "description": "Month",
        "isAbstract": false,
        "column": "month",
        "table": "date",
        "reference": false,
        "projection": "*"
      },
      "year": {
        "type": "number",
        "isId": false,
        "description": "Year",
        "isAbstract": false,
        "column": "year",
        "table": "date",
        "reference": false,
        "projection": "*"
      },
      "dateid": {
        "type": "number",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "dateid",
        "table": "date",
        "reference": false,
        "projection": "*"
      },
      "day": {
        "type": "string",
        "isId": false,
        "description": "Day of the week",
        "isAbstract": false,
        "column": "day",
        "table": "date",
        "reference": false,
        "projection": "*"
      },
      "holiday": {
        "type": "boolean",
        "isId": false,
        "description": "Is a holiday",
        "isAbstract": false,
        "column": "holiday",
        "table": "date",
        "reference": false,
        "projection": "*"
      },
      "qtr": {
        "type": "string",
        "isId": false,
        "description": "Quarter",
        "isAbstract": false,
        "column": "qtr",
        "table": "date",
        "reference": false,
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "event",
    "description": "Collection of events",
    "isAbstract": false,
    "reference": false,
    "properties": {
      "eventid": {
        "type": "number",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "eventid",
        "table": "event",
        "reference": false,
        "projection": "*"
      },
      "catid": {
        "type": "object",
        "isId": false,
        "description": "Reference to event category",
        "isAbstract": false,
        "column": "catid",
        "table": "event",
        "reference": true,
        "docReferenceTo": "Eventcategory",
        "properties": {},
        "referenceTo": {
          "targetTable": "eventcategory",
          "targetColumn": "catid"
        },
        "projection": "*"
      },
      "venueid": {
        "type": "object",
        "isId": false,
        "description": "Reference to venue",
        "isAbstract": false,
        "column": "venueid",
        "table": "event",
        "reference": true,
        "docReferenceTo": "Venue",
        "properties": {},
        "referenceTo": {
          "targetTable": "venue",
          "targetColumn": "venueid"
        },
        "projection": "*"
      },
      "dateid": {
        "type": "object",
        "isId": false,
        "description": "Reference to date",
        "isAbstract": false,
        "column": "dateid",
        "table": "event",
        "reference": true,
        "docReferenceTo": "Date",
        "referenceTo": {
          "targetTable": "date",
          "targetColumn": "dateid"
        },
        "projection": "*"
      },
      "starttime": {
        "type": "string",
        "isId": false,
        "description": "Start time of the event",
        "isAbstract": false,
        "column": "starttime",
        "table": "event",
        "reference": false,
        "projection": "*"
      },
      "eventname": {
        "type": "string",
        "isId": false,
        "description": "Name of the event",
        "isAbstract": false,
        "column": "eventname",
        "table": "event",
        "reference": false,
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "eventcategory",
    "description": "Collection of event categories",
    "isAbstract": false,
    "reference": false,
    "properties": {
      "catid": {
        "type": "number",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "catid",
        "table": "eventcategory",
        "reference": false,
        "projection": "*"
      },
      "catname": {
        "type": "string",
        "isId": false,
        "description": "Category name",
        "isAbstract": false,
        "column": "catname",
        "table": "eventcategory",
        "reference": false,
        "projection": "*"
      },
      "catdesc": {
        "type": "string",
        "isId": false,
        "description": "Category description",
        "isAbstract": false,
        "column": "catdesc",
        "table": "eventcategory",
        "reference": false,
        "projection": "*"
      },
      "catgroup": {
        "type": "string",
        "isId": false,
        "description": "Category group name",
        "isAbstract": false,
        "column": "catgroup",
        "table": "eventcategory",
        "reference": false,
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "listing",
    "description": "Collection of listings",
    "isAbstract": false,
    "reference": false,
    "properties": {
      "listid": {
        "type": "number",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "listid",
        "table": "listing",
        "reference": false,
        "projection": "*"
      },
      "eventid": {
        "type": "object",
        "isId": false,
        "description": "Reference to event",
        "isAbstract": false,
        "column": "eventid",
        "table": "listing",
        "reference": true,
        "docReferenceTo": "Event",
        "referenceTo": {
          "targetTable": "event",
          "targetColumn": "eventid"
        },
        "projection": "*"
      },
      "sellerid": {
        "type": "object",
        "isId": false,
        "description": "Reference to user who sells the listing",
        "isAbstract": false,
        "column": "sellerid",
        "table": "listing",
        "reference": true,
        "docReferenceTo": "Users",
        "properties": {},
        "referenceTo": {
          "targetTable": "users",
          "targetColumn": "userid"
        },
        "projection": "*"
      },
      "totalprice": {
        "type": "number",
        "isId": false,
        "description": "Total price of tickets",
        "isAbstract": false,
        "column": "totalprice",
        "table": "listing",
        "reference": false,
        "projection": "*"
      },
      "dateid": {
        "type": "object",
        "isId": false,
        "description": "Reference to date",
        "isAbstract": false,
        "column": "dateid",
        "table": "listing",
        "reference": true,
        "docReferenceTo": "Date",
        "referenceTo": {
          "targetTable": "date",
          "targetColumn": "dateid"
        },
        "projection": "*"
      },
      "listtime": {
        "type": "string",
        "isId": false,
        "description": "Time of listing",
        "isAbstract": false,
        "column": "listtime",
        "table": "listing",
        "reference": false,
        "projection": "*"
      },
      "numtickets": {
        "type": "number",
        "isId": false,
        "description": "Number of tickets listed",
        "isAbstract": false,
        "column": "numtickets",
        "table": "listing",
        "reference": false,
        "projection": "*"
      },
      "priceperticket": {
        "type": "number",
        "isId": false,
        "description": "Price per ticket",
        "isAbstract": false,
        "column": "priceperticket",
        "table": "listing",
        "reference": false,
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "sales",
    "description": "Collection of sales records",
    "isAbstract": false,
    "reference": false,
    "properties": {
      "listid": {
        "type": "object",
        "isId": false,
        "description": "Reference to listing",
        "isAbstract": false,
        "column": "listid",
        "table": "sales",
        "reference": true,
        "docReferenceTo": "Listing",
        "referenceTo": {
          "targetTable": "listing",
          "targetColumn": "listid"
        },
        "projection": "*"
      },
      "saletime": {
        "type": "string",
        "isId": false,
        "description": "Time of sale",
        "isAbstract": false,
        "column": "saletime",
        "table": "sales",
        "reference": false,
        "projection": "*"
      },
      "eventid": {
        "type": "object",
        "isId": false,
        "description": "Reference to event",
        "isAbstract": false,
        "column": "eventid",
        "table": "sales",
        "reference": true,
        "docReferenceTo": "Event",
        "referenceTo": {
          "targetTable": "event",
          "targetColumn": "eventid"
        },
        "projection": "*"
      },
      "salesid": {
        "type": "number",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "salesid",
        "table": "sales",
        "reference": false,
        "projection": "*"
      },
      "sellerid": {
        "type": "number",
        "isId": false,
        "description": "Seller ID",
        "isAbstract": false,
        "column": "sellerid",
        "table": "sales",
        "reference": false,
        "projection": "*"
      },
      "dateid": {
        "type": "object",
        "isId": false,
        "description": "Reference to date",
        "isAbstract": false,
        "column": "dateid",
        "table": "sales",
        "reference": true,
        "docReferenceTo": "Date",
        "referenceTo": {
          "targetTable": "date",
          "targetColumn": "dateid"
        },
        "projection": "*"
      },
      "commission": {
        "type": "number",
        "isId": false,
        "description": "Commission amount",
        "isAbstract": false,
        "column": "commission",
        "table": "sales",
        "reference": false,
        "projection": "*"
      },
      "qtysold": {
        "type": "number",
        "isId": false,
        "description": "Quantity sold",
        "isAbstract": false,
        "column": "qtysold",
        "table": "sales",
        "reference": false,
        "projection": "*"
      },
      "buyerid": {
        "type": "number",
        "isId": false,
        "description": "Buyer ID",
        "isAbstract": false,
        "column": "buyerid",
        "table": "sales",
        "reference": false,
        "projection": "*"
      },
      "pricepaid": {
        "type": "number",
        "isId": false,
        "description": "Price paid",
        "isAbstract": false,
        "column": "pricepaid",
        "table": "sales",
        "reference": false,
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "users",
    "description": "Collection of users",
    "isAbstract": false,
    "reference": false,
    "properties": {
      "firstname": {
        "type": "string",
        "isId": false,
        "description": "First name of the user",
        "isAbstract": false,
        "column": "firstname",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "liketech": {
        "type": "boolean",
        "isId": false,
        "description": "User likes tech",
        "isAbstract": false,
        "column": "liketech",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "city": {
        "type": "string",
        "isId": false,
        "description": "City of the user",
        "isAbstract": false,
        "column": "city",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "likefestivals": {
        "type": "boolean",
        "isId": false,
        "description": "User likes festivals",
        "isAbstract": false,
        "column": "likefestivals",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "liketheater": {
        "type": "boolean",
        "isId": false,
        "description": "User likes theater",
        "isAbstract": false,
        "column": "liketheater",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "likesports": {
        "type": "boolean",
        "isId": false,
        "description": "User likes sports",
        "isAbstract": false,
        "column": "likesports",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "likegaming": {
        "type": "boolean",
        "isId": false,
        "description": "User likes gaming",
        "isAbstract": false,
        "column": "likegaming",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "userid": {
        "type": "number",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "userid",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "likemovies": {
        "type": "boolean",
        "isId": false,
        "description": "User likes movies",
        "isAbstract": false,
        "column": "likemovies",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "lastname": {
        "type": "string",
        "isId": false,
        "description": "Last name of the user",
        "isAbstract": false,
        "column": "lastname",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "likecomedy": {
        "type": "boolean",
        "isId": false,
        "description": "User likes comedy",
        "isAbstract": false,
        "column": "likecomedy",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "phone": {
        "type": "string",
        "isId": false,
        "description": "Phone number of the user",
        "isAbstract": false,
        "column": "phone",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "likeconcerts": {
        "type": "boolean",
        "isId": false,
        "description": "User likes concerts",
        "isAbstract": false,
        "column": "likeconcerts",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "likefood": {
        "type": "boolean",
        "isId": false,
        "description": "User likes food",
        "isAbstract": false,
        "column": "likefood",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "liketravel": {
        "type": "boolean",
        "isId": false,
        "description": "User likes travel",
        "isAbstract": false,
        "column": "liketravel",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "state": {
        "type": "string",
        "isId": false,
        "description": "State of the user",
        "isAbstract": false,
        "column": "state",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "email": {
        "type": "string",
        "isId": false,
        "description": "Email of the user",
        "isAbstract": false,
        "column": "email",
        "table": "users",
        "reference": false,
        "projection": "*"
      },
      "username": {
        "type": "string",
        "isId": false,
        "description": "Username of the user",
        "isAbstract": false,
        "column": "username",
        "table": "users",
        "reference": false,
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "venue",
    "description": "Collection of venues",
    "isAbstract": false,
    "reference": false,
    "properties": {
      "venuename": {
        "type": "string",
        "isId": false,
        "description": "Name of the venue",
        "isAbstract": false,
        "column": "venuename",
        "table": "venue",
        "reference": false,
        "projection": "*"
      },
      "venueid": {
        "type": "number",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "venueid",
        "table": "venue",
        "reference": false,
        "projection": "*"
      },
      "venuestate": {
        "type": "string",
        "isId": false,
        "description": "State of the venue",
        "isAbstract": false,
        "column": "venuestate",
        "table": "venue",
        "reference": false,
        "projection": "*"
      },
      "venuecity": {
        "type": "string",
        "isId": false,
        "description": "City of the venue",
        "isAbstract": false,
        "column": "venuecity",
        "table": "venue",
        "reference": false,
        "projection": "*"
      },
      "venueseats": {
        "type": "number",
        "isId": false,
        "description": "Number of seats in the venue",
        "isAbstract": false,
        "column": "venueseats",
        "table": "venue",
        "reference": false,
        "projection": "*"
      }
    },
    "projection": "*"
  }
]
```

## Migration Database Report
### Tables Count
|         Class | Count |
|          :--: |  :--: |
|         Sales |    11 |
|         Venue |   205 |
|       Listing |    10 |
| Eventcategory |    11 |
|         Event |    10 |
|         Users |    10 |
|          Date |   365 |

# Analise:

Migrou corretamente porém não explicação não está de acordo com o JSON Schema