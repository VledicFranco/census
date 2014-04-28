Census Control Docs
===================

Config
------
`max-instances`: The maximum number of instances that Census Control will create for computation.  
`ce-max-queue-size`: The maximum amount of requests that will be queued to each Census Control instance.  
`balancing-algo`: The algorithm used to balance requests to Census Engine instances.
> **Supported algorithms:** Round Robin

Features
--------
* Create Instances.
* Delete Instances.
    * Census Engine instances live at least 10 mins.
    * There are no more requests to compute.
* Requests queue.
* Instances pool.
* Round Robin load balancing.
    * Census Control queue sends requests to Census Engine instances only when an instance queue size is less than the maximum queue size configured in Census Control.

HTTP API
--------

### POST /hook
Registers the server information of an HTTP Hook, used by the `HTTPHook` object to send errors and success reports (HTTP messages) to a web server. (See HTTP API: Error and Success Reports)

#### Request data
`host`: The web server host name.  
`port`: The web server port.

```json
{
  host: "10.4.22.123",
  port: 1000
}
```

#### Response data
`status`: Value should be `success`, which means the hook server information is now registered, if json was invalid value should be `badrequest`.

Response status code `200`:
```json
{
  status: "success"
}
```
Response status code `400`:
```json
{
  status: "bad request",
  errors: [
    "'host' field missing.",
    "'port' field missing."
  ]
}
```
Response status code `500`:
```json
{
  status: "unreachable host"
}
```

### GET /hook
Returns the server information of the HTTP Hook server that is registered. 

#### Request data
> *No data needed.*

#### Response data
`host`: The web server host name.  
`port`: The web server port.

Response status code `200`:
```json
{
  host: "10.4.22.123",
  port: 1000
}
```

### POST /compute
_Note: This request is added to the main queue._

Enqueues a graph computation to be executed with a database graph. 

#### Request data
_Note: This json is considered a token, which follows the computation status of the request._

`algorithm`: The name of the algorithm that will be computed, must match the current graph format.
`graph`: An object with all the Neo4j database information.
`tag`: The Neo4j tag that will be used to import the nodes.
`host`: The Neo4j hostname.
`port`: The Neo4j port.
`user` _Optional_: The desired user for the Census Engine instance to use.
`password` _Optional_: The password of the Census Engine user.

```json
{
  algorithm: "Closeness"
  graph: {
    tag: "Person",
    host: "10.3.10.123",
    port: 7474,
    user: "root",
    password: "root"
  }
}
```

#### Response data
`status`: Value should be `acknowledged`, which only means the request was enqueued to the Census Engine instance. Future out requests may report errors.
`token`: A unique string id for the request, this is used to identify future reports.

Response status code 200:
```json
{
  status: "acknowledged",
  token: "AJD23JSA941",
}
```
Response status code `400`:
```json
{
  status: "bad request",
  errors: [
    "No such algorithm 'InexistentAlgo'",
    "'algorithm' field missing.",
    "'tag' field missing.",
    "'host' field missing.",
    "'port' field missing."
  ]
}
```

HTTP API: Error and Success Reports
-----------------------------------

