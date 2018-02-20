from jmxquery import JMXConnection, JMXQuery, MetricType

def test_query():
    jmxConnection = JMXConnection("service:jmx:rmi://127.0.0.1/stub/rO0ABXNyAC5qYXZheC5tYW5hZ2VtZW50LnJlbW90ZS5ybWkuUk1JU2VydmVySW1wbF9TdHViAAAAAAAAAAICAAB4cgAaamF2YS5ybWkuc2VydmVyLlJlbW90ZVN0dWLp/tzJi+FlGgIAAHhyABxqYXZhLnJtaS5zZXJ2ZXIuUmVtb3RlT2JqZWN002G0kQxhMx4DAAB4cHc5AAtVbmljYXN0UmVmMgAADjE2OS4yNTQuOTUuMjU0AADGy3vgbYsh9TmwdO1JZwAAAWGwaRo5gAEAeA==")

    jmxQuery = [JMXQuery("*:*")]

    metrics = jmxConnection.query(jmxQuery)

    for metric in metrics:
        metricName = metric.to_query_string()
        print(f"{metricName} ({metric.value_type}) = {metric.value}")
