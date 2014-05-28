Census Framework Documentation
==============================

System Architecture
-------------------

The system is setup on GCE ([Google Compute Engine](https://cloud.google.com/products/compute-engine)), Census Framework is made of two sub projects, Census Control and Census Engine.

Census Control initializes on a single GCE virtual machine, it is the service which receives computation petitions from the client, then it creates and orchestrates as many instances of Census Engine as needed to compute those petitions.

Census Engine initializes on GCE virtual machines, it is the service which imports the graph (from a [Neo4j](http://www.neo4j.org/) database) and starts an algorithm computation on it, when done it reinserts the generated data on the same database.

The framework used to do the actual graph computation is [Signal Collect](http://uzh.github.io/signal-collect/) v2.0, and the framework used to create the RESTful API is [Play Framework](http://www.playframework.com/) v2.1.5.

_Note: The Akka versions for Signal Collect and Play Framework can cause dependency conflicts, Play Framework v2.1.5 and Signal Collect v2.0 share a compatible version of Akka._

![Census Framework Architecture](https://raw.githubusercontent.com/FrancoAra/census-control/master/docs/images/Census_Framework_Architecture.jpeg)

1) The client sends requests to Census Control through the RESTful API, (including registering a web hook ([HTTPHook](https://github.com/FrancoAra/census-control/blob/master/app/controllers/HTTPHook.scala)) to receive reports). The client can receive string tokens back, which are used to reference pending petitions.

2) The Play Framework controller [InReqeusts](https://github.com/FrancoAra/census-control/blob/master/app/controllers/InReports.scala) initializes a [ComputationRequest](https://github.com/FrancoAra/census-control/blob/master/app/controllers/requests/ComputationRequest.scala) object, which saves the request metadata, including the [N4j](https://github.com/FrancoAra/census-control/blob/master/app/controllers/N4j.scala) (Neo4j) service instance, and through the [Receiver](https://github.com/FrancoAra/census-control/blob/master/app/compute/Receiver.scala) trait, it can initiate an [EngineRequest](https://github.com/FrancoAra/census-control/blob/master/app/compute/EngineRequest.scala).

3) In case of an all pair algorithm (an algorithm which needs to be computed for every pair of nodes on the graph) or an algorithm which has to be computed individually for every node (like a single source shortest path) a [MultiNodeRequest](https://github.com/FrancoAra/census-control/blob/master/app/compute/MultiNodeRequest.scala) algorithm is created, which creates a [SingleNodeRequest](https://github.com/FrancoAra/census-control/blob/master/app/compute/SingleNodeRequest.scala) for every node on the graph.

_Note: Here a query from Census Control to the Neo4j service (this interaction doesn't appear on the architecture diagram) is required to retrieve the nodes id and possible extra data._

The `MultiNodeRequest` also creates an [Orchestrator](https://github.com/FrancoAra/census-control/blob/master/app/instances/Orchestrator.scala), which creates as many Census Engine [Instances](https://github.com/FrancoAra/census-control/blob/master/app/instances/Instance.scala) as needed to distribute the `SingleNodeRequests` through them.

_Note: The `MultiNodeRequest` implements the [Receiver](https://github.com/FrancoAra/census-control/blob/master/app/compute/Receiver.scala) trait so that the `ComputationRequest` can initialize it, and each `SingleNodeRequest` implements the [Sender](https://github.com/FrancoAra/census-control/blob/master/app/compute/Sender.scala) trait so that the `Orchestrator's` `Instances` can send the actual requests to the multiple Census Engine services._

4) **Not yet implemented:** In case of a single graph request (an algorithm which can be computed on a single instance, on a single run, like page rank) a [SingleGraphRequest]() is created, which creates a single [Instance](https://github.com/FrancoAra/census-control/blob/master/app/instances/Instance.scala) to start the computation.

_Note: The setup of the algorithms on Census Control is in the [library](https://github.com/FrancoAra/census-control/tree/master/app/compute/library) package, and the actual implementation of the algorithms on Census Engine is in the [library](https://github.com/FrancoAra/census-engine/tree/master/app/compute/library) package._

5) Through the `Sender` trait, the `Instances` can send http requests to the Census Engine [InRequests](https://github.com/FrancoAra/census-engine/blob/master/app/controllers/InRequests.scala) module.

6) Through Census Engine's [CensusControl](https://github.com/FrancoAra/census-engine/blob/master/app/controllers/CensusControl.scala) module, reports of success or error are sent back to Census Control's [InReports](https://github.com/FrancoAra/census-control/blob/master/app/controllers/InReports.scala) module.

7) Before making a graph computation on Census Engine a [GraphImportRequest](https://github.com/FrancoAra/census-engine/blob/master/app/controllers/requests/GraphImportRequest.scala) must be done. After that a [ComputationRequest](https://github.com/FrancoAra/census-engine/blob/master/app/controllers/requests/ComputationRequest.scala) can be done for all the requests that uses the same graph for the same algorithm.

8) The [N4j](https://github.com/FrancoAra/census-engine/blob/master/app/controllers/N4j.scala) module is used to query the client's graph database for the graph importation.

9) When the computation is finished, the generated data is reinserted on the client's Neo4j databse, and Census Control is notified.

Google Compute Engine Setup
---------------------------

To setup the project on Google Compute Engine the following must be done:

0) Setup `gcutil`
1) Create the Census Framework network.
2) Install Census Control on a booteable disk.
3) Install Census Engine on a booteable disk.
4) Create a disk snapshot of the Census Engine disk.
5) Add the Census Engine startup script to Google Cloud Storage.
6) Check the configuration in the Census Control `instance` package.
7) Start the Census Control service.

### 0) Setup gcutil

Check [this](https://developers.google.com/compute/docs/gcutil/) link to install and setup `gcutil`.

_Note: it is recommended to set the default project so that the next commands doesn't need the `--project=<project_id>` flag._

```
gcloud auth login
gcloud config set project <project_id>
```

### 1) Create the Census Framework network

Create the network.
```
gcutil addnetwork census-framework
```
Open port 9595 for external communication with Census Control.
```
gcutil addfirewall census-framework-default --network=census-framework --allowed="tcp:9595"
```
Open port 22 for ssh connections to Census Control.
```
gcutil addfirewall census-framework-ssh --network=census-framework --allowed="tcp:22"
```
Allow communication between Census instances inside the GCE virtual network.
```
gcutil addfirewall census-framework-allow-internal --network=census-framework --allowed_ip_sources=10.0.0.0/8 --allowed="tcp:1-65535,udp:1-65535,icmp"
```

### 2) Install Census Control on a booteable disk

Create the Census Control bootable disk with Debian 7 and 10gb of space. 
```
gcutil adddisk census-control-disk --size_gb=10 --zone=us-central1-a --source_image=debian-7
```
Create a temporal Census Control instance to install the service.
```
gcutil addinstance census-control --disk=census-control-disk,boot  --network=census-framework --zone=us-central1-a --machine_type=n1-highcpu-2
```
Install the service.
```
# Connect to the instance.
gcutil ssh census-control

# Install necessary software.
sudo -s
apt-get update
apt-get install default-jdk
apt-get install git 
apt-get install unzip

cd /usr/share
# Clone the project.
git clone https://github.com/FrancoAra/census-control.git
# Install Play Framework 2.1.5
wget http://downloads.typesafe.com/play/2.1.5/play-2.1.5.zip
unzip play-2.1.5.zip

exit
exit
```
Delete the instance if you want.
```
gcutil deleteinstance census-control
```

### 3) Install Census Engine on a booteable disk

Create the Census Engine bootable disk with Debian 7 and 10gb of space. 
```
gcutil adddisk census-engine-disk --size_gb=10 --zone=us-central1-a --source_image=debian-7
```
Create a temporal Census Engine instance to install the service.
```
gcutil addinstance census-engine --disk=census-engine-disk,boot  --network=census-framework --zone=us-central1-a --machine_type=n1-highcpu-2
```
Install the service.
```
# Connect to the instance.
gcutil ssh census-engine

# Install necessary software.
sudo -s
apt-get update
apt-get install default-jdk
apt-get install git 
apt-get install unzip

cd /usr/share
# Clone the project or download the precompiled version here 
# so that Census Engine instances do not need to compile the 
# code when created.
git clone https://github.com/FrancoAra/census-engine.git
# Install Play Framework 2.1.5
wget http://downloads.typesafe.com/play/2.1.5/play-2.1.5.zip
unzip play-2.1.5.zip

exit
exit
```
Delete the instance (you wont need this instance anymore).
```
gcutil deleteinstance census-engine
```

### 4) Create a disk snapshot of the Census Engine disk

```
gcutil addsnapshot census-engine-snapshot --source_disk=census-engine-disk
```

### 5) Add the Census Engine startup script to Google Cloud Storage

[Upload](https://developers.google.com/storage/docs/json_api/v1/how-tos/upload) a startup script to Google Cloud Storage for the Census Engine future instances.

Here is a possible script (stored in: `gs://census-framework/engine-startup.sh`):
```
#!/bin/sh

cd /usr/share/census-engine
/usr/share/play-2.1.5/play "start 9000"
```

_Note: You will need to change the startup script url in the Census Control instances configuration._

### 6) Check the configuration in the Census Control instance package

In the census-control instance, inside the Census Control project `/usr/share/census-control/app/instances/conf.scala` change the desired configuration.

### 7) Start the Census Control service

```
gcutil ssh census-control

sudo -s
cd /usr/share/census-control
../play-2.1.5/play "start 9595"
```

_Note: You can use a program like [screen](http://www.gnu.org/software/screen/) to demonize the service, or create the census-control instance with a startup script like you did with the census-engine instances._

Census Control RESTful API: In Requests
---------------------------------------

_Note: Computation requests are may take several minutes, so they just return an `acknowledged` status with a string token for future reference, they are processed in a "first come first served" queue. Future error or success reports may follow the `acknowledged` response in the form of a web hook report, which is an HTTP request from Census Control to an external web server._

_Note: If Census Control can't send messages to the web hook every out request will just log `WARNING: Unreachable HTTP hook server.`_

### POST /hook
Registers the server information of aa Web Hook, used by the `HTTPHook` module to send errors and success reports (HTTP messages) to a web server. (See HTTP API: Error and Success Reports)

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
Returns the server information of the web hook server that is registered. 

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
Enqueues a graph computation to be executed with a database graph. 

#### Request data
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
`status`: Value should be `acknowledged`, which only means the request was enqueued. Future out requests may report errors.
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

Census Control RESTful API: Error and Success Reports
-----------------------------------------------------
After registering a web hook, reports will be sent to the following paths:

### POST /censusengine/report
**Computation finished:**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "success"
}
```
### POST /censusengine/error

**Unreachable Neo4j server:**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "error",
  error: "unreachable-neo4j"
}
```
**Invalid Neo4j format:**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "error",
  error: "invalid-neo4j-format"
}
```

Census Engine RESTful API: In Requests
--------------------------------------

_Note: Some requests are added to a queue, this kind of requests may take longer so they just return an `acknowledged` status with a string token for future reference, they are processed in a "first come first served" queue. Future error or success reports may follow the `acknowledged` response in the form of an "out request" report, which is an HTTP request from Census Engine to Census Control. One of the main reasons of doing this is that many operations must be made sequentially and Census Control should not wait for them to be done, just receive a report when the request is finally done._

_Note: If Census Engine can't send messages to Census Control every out request will just log `WARNING: Unreachable Census Control server.`_

### POST /control
Registers the server information of the Census Control master, used by the `CensusControl` module to send HTTP messages to Census Control. This is the first thing that Census Control must request before anything else.

#### Request data
`host`: The Census Control host name.  
`port`: The Census Control port.

```json
{
  host: "census-control",
  port: 9595
}
```

#### Response data
`status`: Value should be `success`, which means the Census Control server information is now registered to this Census Engine instance, if json was invalid value should be `badrequest`.

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

### GET /control
Returns the server information of the Census Control server that is registered on this instance. 

#### Request data
> *No data needed.*

#### Response data
`host`: The Census Control host name.  
`port`: The Census Control port.

Response status code `200`:
```json
{
  host: "census-control",
  port: 9595
}
```

### POST /graph
_Note: This request is added to the main queue._  
_Note: Every imported node needs an `id` attribute with a string value in Neo4j._

Imports a graph from a Neo4j database, has to be formated for a specific algorithm. This action will automatically delete any previous graph imported to Census Engine.

#### Request data
`token`: A unique string id for the request, this is used to identify future reports.
`algorithm`: The algorithm for which will be formated the graph. 
`tag`: The Neo4j tag that will be used to import the nodes.
`host`: The Neo4j hostname.
`port`: The Neo4j port.
`user` _Optional_: The desired user for the Census Engine instance to use.
`password` _Optional_: The password of the Census Engine user.

```json
{
  token: "AJD23JS391",
  algorithm: "SSCloseness",
  tag: "Person",
  host: "10.3.10.123",
  port: 7474,
  user: "root",
  password: "root"
}
```

#### Response data
`status`: Value should be `acknowledged`, which only means the request was enqueued to the Census Engine instance. Future out requests may report errors.

Response status code `200`:
```json
{
  status: "acknowledged"
}
```
Response status code `400`:
```json
{
  status: "bad request",
  errors: [
    "'token' field missing.",
    "No such algorithm 'InexistentAlgo'",
    "'algorithm' field missing.",
    "'tag' field missing.",
    "'host' field missing.",
    "'port' field missing."
  ]
}
```

### POST /compute
_Note: This request is added to the main queue._

Enqueues a graph computation, must match the current graph format. The request needs a string token generated by Census Control, which is used to reference the request on reports of errors or success.

#### Request data

`token`: A unique string id for the request, this is used to identify future reports.
`algorithm`: The name of the algorithm that will be computed, must match the current graph format.
`timeCreation`: The Unix milliseconds time stamp when the request was created.
`vars`: All the variables needed for the computation of the algorithm, this depend on each algorithm needs.

```json
{
  token: "AJD23JSA941",
  algorithm: "SSCloseness"
  creationTime: 1394231356274,
  vars: {
    source: 91
  }
}
```

#### Response data
`status`: Value should be `acknowledged`, which only means the request was enqueued to the Census Engine instance. Future out requests may report errors.

Response status code 200:
```json
{
  status: "acknowledged"
}
```
Response status code `400`:
```json
{
  status: "bad request",
  errors: [
    "'token' field missing.",
    "No such algorithm 'InexistentAlgo'",
    "'algorithm' field missing.",
    "'timeCreation' field missing.",
  ]
}
```

Census Engine RESTful API: Error and Success Reports
----------------------------------------------------
After registering a Census Control web hook, reports will be sent to the following paths:

### POST /censusengine/report
**Graph import finished:**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "finished"
}
```
**Computation finished:**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "finished"
}
```
### POST /censusengine/error

#### Error reports on a POST /graph
**Unreachable Neo4j server (When importing the graph):**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "error",
  error: "unreachable-neo4j",
  on: "graph-import"
}
```
**Invalid Neo4j format:**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "error",
  error: "invalid-neo4j-format",
  on: "graph-import"
}
```
#### Error reports on a POST /compute
**Unreachable Neo4j server (When inserting back the result of a computation):**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "error",
  error: "unreachable-neo4j",
  on: "compute"
}
```
**Computation not ready (No graph was imported):**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "error",
  error: "missing-graph",
  on: "compute"
}
```
**Computation bad request (Invalid variables for the computation):**
```json
{
  token: "asd124-asdf-12351af-214",
  status: "error",
  error: "invalid-variables",
  on: "compute"
}
```
