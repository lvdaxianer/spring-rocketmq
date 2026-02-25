#!/bin/bash

# RocketMQ Docker Startup Script

echo "Starting RocketMQ services..."

# Start docker-compose
cd "$(dirname "$0")"

docker-compose up -d

# Wait for services to be ready
echo "Waiting for NameServer to be ready..."
sleep 10

echo "Waiting for Broker to be ready..."
sleep 10

echo "Checking services status..."
docker ps

echo ""
echo "RocketMQ services started successfully!"
echo "- NameServer: localhost:9876"
echo "- Broker: localhost:10911"
echo "- Console: http://localhost:8080"
echo ""
echo "To stop services, run: docker-compose down"
