curl -X POST -H "Content-Type: application/json" -d '{
  "algorithm": "Closeness",
  "graph": {
    "tag": "Person",
    "host": "localhost",
    "port": 7474
  }
}' http://localhost:9000/compute
