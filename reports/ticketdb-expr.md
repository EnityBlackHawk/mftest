# Migration Report
**Date:** Thu Apr 24 22:28:39 BRT 2025 
## Inputs:
|           Parameter |              Value |
|                 :-- |               :--: |
|     allowReferences |              false |
|           framework |        SPRING_DATA |
|       referenceOnly |              false |
|         useMarkdown |               true |
| migrationPreference | PREFER_CONSISTENCY |

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
The schema is designed to optimize for frequent queries by embedding related data where appropriate and referencing where the relationships are more complex. For example, event categories are embedded within events due to their frequent access together, while listings reference users and events for clarity and maintainability. Additionally, sales data is aggregated by date, so it is structured to facilitate easy querying of sales by month and year. 

<details>
<summary> 

### JSON Schema 

</summary>

```json
[
  {
    "type": "object",
    "isId": false,
    "title": "Event",
    "description": "Collection of events with associated venue and category information",
    "isAbstract": false,
    "relationshipType": "none",
    "properties": {
      "date": {
        "type": "object",
        "isId": false,
        "description": "Event date information",
        "isAbstract": false,
        "column": "dateid",
        "table": "event",
        "relationshipType": "embedded",
        "properties": {
          "caldate": {
            "type": "string",
            "isId": false,
            "description": "Calendar date of the event",
            "isAbstract": false,
            "column": "caldate",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          },
          "month": {
            "type": "string",
            "isId": false,
            "description": "Month of the event",
            "isAbstract": false,
            "column": "month",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          },
          "year": {
            "type": "integer",
            "isId": false,
            "description": "Year of the event",
            "isAbstract": false,
            "column": "year",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          },
          "dateid": {
            "type": "integer",
            "isId": true,
            "description": "Primary key",
            "isAbstract": false,
            "column": "dateid",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          }
        },
        "referenceTo": {
          "targetTable": "date",
          "targetColumn": "$auto"
        },
        "projection": "*"
      },
      "eventid": {
        "type": "integer",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "eventid",
        "table": "event",
        "relationshipType": "none",
        "projection": "*"
      },
      "venue": {
        "type": "object",
        "isId": false,
        "description": "Venue information",
        "isAbstract": false,
        "column": "venueid",
        "table": "event",
        "relationshipType": "embedded",
        "properties": {
          "venuename": {
            "type": "string",
            "isId": false,
            "description": "Name of the venue",
            "isAbstract": false,
            "column": "venuename",
            "table": "venue",
            "relationshipType": "none",
            "projection": "*"
          },
          "venueid": {
            "type": "integer",
            "isId": true,
            "description": "Primary key",
            "isAbstract": false,
            "column": "venueid",
            "table": "venue",
            "relationshipType": "none",
            "projection": "*"
          },
          "venuestate": {
            "type": "string",
            "isId": false,
            "description": "State of the venue",
            "isAbstract": false,
            "column": "venuestate",
            "table": "venue",
            "relationshipType": "none",
            "projection": "*"
          },
          "venuecity": {
            "type": "string",
            "isId": false,
            "description": "City of the venue",
            "isAbstract": false,
            "column": "venuecity",
            "table": "venue",
            "relationshipType": "none",
            "projection": "*"
          }
        },
        "referenceTo": {
          "targetTable": "venue",
          "targetColumn": "$auto"
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
        "relationshipType": "none",
        "projection": "*"
      },
      "categories": {
        "type": "array",
        "isId": false,
        "description": "List of event categories",
        "isAbstract": false,
        "column": "catid",
        "table": "event",
        "relationshipType": "embedded",
        "projection": "*",
        "items": {
          "type": "object",
          "isId": false,
          "isAbstract": false,
          "relationshipType": "embedded",
          "properties": {
            "catid": {
              "type": "integer",
              "isId": true,
              "description": "Primary key",
              "isAbstract": false,
              "column": "catid",
              "table": "eventcategory",
              "relationshipType": "none",
              "projection": "*"
            },
            "catname": {
              "type": "string",
              "isId": false,
              "description": "Name of the category",
              "isAbstract": false,
              "column": "catname",
              "table": "eventcategory",
              "relationshipType": "none",
              "projection": "*"
            }
          },
          "projection": "*"
        }
      },
      "eventname": {
        "type": "string",
        "isId": false,
        "description": "Name of the event",
        "isAbstract": false,
        "column": "eventname",
        "table": "event",
        "relationshipType": "none",
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "Listing",
    "description": "Collection of event listings by users",
    "isAbstract": false,
    "relationshipType": "none",
    "properties": {
      "listid": {
        "type": "integer",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "listid",
        "table": "listing",
        "relationshipType": "none",
        "projection": "*"
      },
      "seller": {
        "type": "object",
        "isId": false,
        "description": "Seller information",
        "isAbstract": false,
        "column": "sellerid",
        "table": "listing",
        "relationshipType": "reference",
        "docReferenceTo": "Users",
        "properties": {},
        "referenceTo": {
          "targetTable": "users",
          "targetColumn": "userid"
        },
        "projection": "*"
      },
      "date": {
        "type": "object",
        "isId": false,
        "description": "Listing date information",
        "isAbstract": false,
        "column": "dateid",
        "table": "listing",
        "relationshipType": "embedded",
        "properties": {
          "month": {
            "type": "string",
            "isId": false,
            "description": "Month of the listing",
            "isAbstract": false,
            "column": "month",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          },
          "year": {
            "type": "integer",
            "isId": false,
            "description": "Year of the listing",
            "isAbstract": false,
            "column": "year",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          },
          "dateid": {
            "type": "integer",
            "isId": true,
            "description": "Primary key",
            "isAbstract": false,
            "column": "dateid",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          }
        },
        "referenceTo": {
          "targetTable": "date",
          "targetColumn": "$auto"
        },
        "projection": "*"
      },
      "totalprice": {
        "type": "number",
        "isId": false,
        "description": "Total price of the listing",
        "isAbstract": false,
        "column": "totalprice",
        "table": "listing",
        "relationshipType": "none",
        "projection": "*"
      },
      "listtime": {
        "type": "string",
        "isId": false,
        "description": "Time when the listing was created",
        "isAbstract": false,
        "column": "listtime",
        "table": "listing",
        "relationshipType": "none",
        "projection": "*"
      },
      "numtickets": {
        "type": "integer",
        "isId": false,
        "description": "Number of tickets available",
        "isAbstract": false,
        "column": "numtickets",
        "table": "listing",
        "relationshipType": "none",
        "projection": "*"
      },
      "event": {
        "type": "object",
        "isId": false,
        "description": "Event information",
        "isAbstract": false,
        "column": "eventid",
        "table": "listing",
        "relationshipType": "reference",
        "docReferenceTo": "Event",
        "referenceTo": {
          "targetTable": "event",
          "targetColumn": "eventid"
        },
        "projection": "*"
      },
      "priceperticket": {
        "type": "number",
        "isId": false,
        "description": "Price per ticket",
        "isAbstract": false,
        "column": "priceperticket",
        "table": "listing",
        "relationshipType": "none",
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "Sales",
    "description": "Collection of sales transactions",
    "isAbstract": false,
    "relationshipType": "none",
    "properties": {
      "date": {
        "type": "object",
        "isId": false,
        "description": "Sale date information",
        "isAbstract": false,
        "column": "dateid",
        "table": "sales",
        "relationshipType": "embedded",
        "properties": {
          "month": {
            "type": "string",
            "isId": false,
            "description": "Month of the sale",
            "isAbstract": false,
            "column": "month",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          },
          "year": {
            "type": "integer",
            "isId": false,
            "description": "Year of the sale",
            "isAbstract": false,
            "column": "year",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          },
          "dateid": {
            "type": "integer",
            "isId": true,
            "description": "Primary key",
            "isAbstract": false,
            "column": "dateid",
            "table": "date",
            "relationshipType": "none",
            "projection": "*"
          }
        },
        "referenceTo": {
          "targetTable": "date",
          "targetColumn": "$auto"
        },
        "projection": "*"
      },
      "saletime": {
        "type": "string",
        "isId": false,
        "description": "Time when the sale was made",
        "isAbstract": false,
        "column": "saletime",
        "table": "sales",
        "relationshipType": "none",
        "projection": "*"
      },
      "eventid": {
        "type": "integer",
        "isId": false,
        "description": "ID of the event sold",
        "isAbstract": false,
        "column": "eventid",
        "table": "sales",
        "relationshipType": "none",
        "projection": "*"
      },
      "salesid": {
        "type": "integer",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "salesid",
        "table": "sales",
        "relationshipType": "none",
        "projection": "*"
      },
      "sellerid": {
        "type": "integer",
        "isId": false,
        "description": "ID of the seller",
        "isAbstract": false,
        "column": "sellerid",
        "table": "sales",
        "relationshipType": "none",
        "projection": "*"
      },
      "commission": {
        "type": "number",
        "isId": false,
        "description": "Commission charged on the sale",
        "isAbstract": false,
        "column": "commission",
        "table": "sales",
        "relationshipType": "none",
        "projection": "*"
      },
      "qtysold": {
        "type": "integer",
        "isId": false,
        "description": "Quantity of tickets sold",
        "isAbstract": false,
        "column": "qtysold",
        "table": "sales",
        "relationshipType": "none",
        "projection": "*"
      },
      "listing": {
        "type": "object",
        "isId": false,
        "description": "Listing information",
        "isAbstract": false,
        "column": "listid",
        "table": "sales",
        "relationshipType": "reference",
        "docReferenceTo": "Listing",
        "referenceTo": {
          "targetTable": "listing",
          "targetColumn": "listid"
        },
        "projection": "*"
      },
      "buyerid": {
        "type": "integer",
        "isId": false,
        "description": "ID of the buyer",
        "isAbstract": false,
        "column": "buyerid",
        "table": "sales",
        "relationshipType": "none",
        "projection": "*"
      },
      "pricepaid": {
        "type": "number",
        "isId": false,
        "description": "Price paid for the tickets",
        "isAbstract": false,
        "column": "pricepaid",
        "table": "sales",
        "relationshipType": "none",
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "Users",
    "description": "Collection of users with their preferences",
    "isAbstract": false,
    "relationshipType": "none",
    "properties": {
      "firstname": {
        "type": "string",
        "isId": false,
        "description": "First name of the user",
        "isAbstract": false,
        "column": "firstname",
        "table": "users",
        "relationshipType": "none",
        "projection": "*"
      },
      "preferences": {
        "type": "object",
        "isId": false,
        "description": "User preferences for different event types",
        "isAbstract": false,
        "relationshipType": "embedded",
        "properties": {
          "liketech": {
            "type": "boolean",
            "isId": false,
            "description": "User likes technology",
            "isAbstract": false,
            "column": "liketech",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "likeconcerts": {
            "type": "boolean",
            "isId": false,
            "description": "User likes concerts",
            "isAbstract": false,
            "column": "likeconcerts",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "likefood": {
            "type": "boolean",
            "isId": false,
            "description": "User likes food",
            "isAbstract": false,
            "column": "likefood",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "liketravel": {
            "type": "boolean",
            "isId": false,
            "description": "User likes travel",
            "isAbstract": false,
            "column": "liketravel",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "likefestivals": {
            "type": "boolean",
            "isId": false,
            "description": "User likes festivals",
            "isAbstract": false,
            "column": "likefestivals",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "liketheater": {
            "type": "boolean",
            "isId": false,
            "description": "User likes theater",
            "isAbstract": false,
            "column": "liketheater",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "likesports": {
            "type": "boolean",
            "isId": false,
            "description": "User likes sports",
            "isAbstract": false,
            "column": "likesports",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "likegaming": {
            "type": "boolean",
            "isId": false,
            "description": "User likes gaming",
            "isAbstract": false,
            "column": "likegaming",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "likemovies": {
            "type": "boolean",
            "isId": false,
            "description": "User likes movies",
            "isAbstract": false,
            "column": "likemovies",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          },
          "likecomedy": {
            "type": "boolean",
            "isId": false,
            "description": "User likes comedy",
            "isAbstract": false,
            "column": "likecomedy",
            "table": "users",
            "relationshipType": "none",
            "projection": "*"
          }
        },
        "referenceTo": {
          "targetTable": "users",
          "targetColumn": "$auto"
        },
        "projection": "*"
      },
      "city": {
        "type": "string",
        "isId": false,
        "description": "City of the user",
        "isAbstract": false,
        "column": "city",
        "table": "users",
        "relationshipType": "none",
        "projection": "*"
      },
      "phone": {
        "type": "string",
        "isId": false,
        "description": "Phone number of the user",
        "isAbstract": false,
        "column": "phone",
        "table": "users",
        "relationshipType": "none",
        "projection": "*"
      },
      "state": {
        "type": "string",
        "isId": false,
        "description": "State of the user",
        "isAbstract": false,
        "column": "state",
        "table": "users",
        "relationshipType": "none",
        "projection": "*"
      },
      "userid": {
        "type": "integer",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "userid",
        "table": "users",
        "relationshipType": "none",
        "projection": "*"
      },
      "email": {
        "type": "string",
        "isId": false,
        "description": "Email address of the user",
        "isAbstract": false,
        "column": "email",
        "table": "users",
        "relationshipType": "none",
        "projection": "*"
      },
      "username": {
        "type": "string",
        "isId": false,
        "description": "Username of the user",
        "isAbstract": false,
        "column": "username",
        "table": "users",
        "relationshipType": "none",
        "projection": "*"
      },
      "lastname": {
        "type": "string",
        "isId": false,
        "description": "Last name of the user",
        "isAbstract": false,
        "column": "lastname",
        "table": "users",
        "relationshipType": "none",
        "projection": "*"
      }
    },
    "projection": "*"
  },
  {
    "type": "object",
    "isId": false,
    "title": "Venue",
    "description": "Collection of venues for events",
    "isAbstract": false,
    "relationshipType": "none",
    "properties": {
      "venuename": {
        "type": "string",
        "isId": false,
        "description": "Name of the venue",
        "isAbstract": false,
        "column": "venuename",
        "table": "venue",
        "relationshipType": "none",
        "projection": "*"
      },
      "venueid": {
        "type": "integer",
        "isId": true,
        "description": "Primary key",
        "isAbstract": false,
        "column": "venueid",
        "table": "venue",
        "relationshipType": "none",
        "projection": "*"
      },
      "venuestate": {
        "type": "string",
        "isId": false,
        "description": "State of the venue",
        "isAbstract": false,
        "column": "venuestate",
        "table": "venue",
        "relationshipType": "none",
        "projection": "*"
      },
      "venuecity": {
        "type": "string",
        "isId": false,
        "description": "City of the venue",
        "isAbstract": false,
        "column": "venuecity",
        "table": "venue",
        "relationshipType": "none",
        "projection": "*"
      },
      "venueseats": {
        "type": "integer",
        "isId": false,
        "description": "Number of seats in the venue",
        "isAbstract": false,
        "column": "venueseats",
        "table": "venue",
        "relationshipType": "none",
        "projection": "*"
      }
    },
    "projection": "*"
  }
]
```
</details> 

## Migration Database Report
### Tables Count
|   Class | Count |
|     :-- |  :--: |
|   Sales |    11 |
|   Venue |   205 |
| Listing |    10 |
|   Event |    10 |
|   Users |    10 |

