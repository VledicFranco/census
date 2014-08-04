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