curl -X POST -H "Content-Type: application/json" -d '{
  "algorithm": "Closeness",
  "instances": 2,
  "graph": {
    "tag": "Person",
    "host": "host.com",
    "port": 7474,
    "user": "census",
    "password": "root"
  }
}' http://0.0.0.0:9595/compute
