How to use Census
=================

Census is a service with a RESTful API, so to compute an algorithm all you need
to do is send a POST with a json to `http://localhost:9000/control/compute`
(change the host to the host where Census is installed). Lets see different uses:

_Note: All the examples uses `curl` to send the http request._

Compute on a local host using the same Census Control server
------------------------------------------------------------

```json
curl -X POST -H "Content-Type: application/json" -d '{
  "algorithm": "SSCloseness",
  "bulk": "all-sources",
  "engines": [
    {"server-ip": "127.0.0.1", "server-port": 9000}
  ],
  "graph": {
    "tag": "Profile",
    "host": "http://example.com/",
    "port": 24789,
    "user": "admin",
    "password": "root"
  }
}' http://localhost:9000/control/compute
```

The `algorithm` field tells census what algorithm to compute.  
The `bulk` field tells census for how many vertices the computation should be done,
some options are: `all-sources`, `all-pair`, `singlet`.  
The `engines` field tells census where are the Census Engine servers it can use.
The `graph` field has all the information of the Neo4j database to use.

Compute using static servers and sending variables
--------------------------------------------------

```json
curl -X POST -H "Content-Type: application/json" -d '{
  "algorithm": "SSCloseness",
  "bulk": "singlet",
  "vars": ["idnode1"],
  "engines": [
    {"server-ip": "10.42.265.1", "server-port": 9000},
    {"server-ip": "10.42.265.2", "server-port": 9000},
    {"server-ip": "10.42.265.3", "server-port": 9000}
  ],
  "graph": {
    "tag": "Profile",
    "host": "http://example.com/",
    "port": 24789,
    "user": "admin",
    "password": "root"
  }
}' http://localhost:9000/control/compute
```

Here the `vars` array is sent to the computation, in this case it means the
source vertex to use. We will use `singlet` as our `bulk` option to compute the
SSCloseness for only one node.

On this request we are also using 3 servers with Census listening on port 9000.

Compute with a Google Compute Engine installation
-------------------------------------------------

```json
curl -X POST -H "Content-Type: application/json" -d '{
  "algorithm": "SSCloseness",
  "bulk": "all-sources",
  "instances": 100,
  "graph": {
    "tag": "Profile",
    "host": "http://example.com/",
    "port": 24789,
    "user": "admin",
    "password": "root"
  }
}' http://10.234.254.20:9000/control/compute
```

Here we are supposing that the ip `10.234.254.20` is a Google Compute Engine's
public ip with a Census instance listening.

The `instances` field will tell Census to try use the GCE api and create 100
servers with Census for the computation of SSCloseness for each source.
