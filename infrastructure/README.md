# Infrastructure

## Profiles

| Profile | Purpose | Services |
|---------|---------|----------|
| `core` | Development essentials | Keycloak, Kafka, Zookeeper, Redis, PostgreSQL databases |
| `full` | Full observability stack | Zipkin, Elasticsearch, Logstash, Kibana, Prometheus, Grafana |

## Starting the stack

Start only core infrastructure (databases, messaging, auth):

```bash
docker compose --profile core up -d
```

Start everything including observability:

```bash
docker compose --profile core --profile full up -d
```

## Observability endpoints

| Service | URL |
|---------|-----|
| Zipkin | http://localhost:9411 |
| Kibana | http://localhost:5601 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |
| Elasticsearch | http://localhost:9200 |
