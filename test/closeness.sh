curl -X POST -H "Content-Type: application/json" -d '{
  "algorithm": "Closeness",
  "graph": {
    "tag": "Person",
    "host": "census.sb02.stations.graphenedb.com",
    "port": 24789,
    "user": "census",
    "password": "rBX0DoF5iPbIhevAtGfx"
  }
}' http://107.178.218.117:9595/compute
