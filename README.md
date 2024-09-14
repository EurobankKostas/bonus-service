# Bonus Microservice README

## Overview

The Bonus Microservice is designed to handle bonus allocation for players upon their login. It listens for player login events, calculates the appropriate bonus, and updates the player's bonus balance. This document covers the architecture, usage, and deployment of the Bonus Microservice.

## Features

- **Event Subscription:** Listens to the `player-login-events` Kafka topic to receive notifications of player logins.
- **Bonus Calculation:** Consults the `bonus` table to decide the bonus amount based on predefined criteria.
- **Bonus Distribution:** Sends updates to the `player-bonus-updates` topic to notify the Login Service to apply the bonus.
- **Idempotency:** Ensures that each bonus is processed only once to avoid duplication.

## Architecture

- **Database:** Utilizes a relational database (specific choice to be determined by the implementer) with at least the following schema:
    - `player_bonus` table:
        - `player_id`: Identifier for the player.
        - `total_bonus`: Cumulative bonus amount for the player.
- **Services Communication:** Uses Apache Kafka for messaging between services.

### Idempotency in Producers and Consumers

Our project ensures idempotency in both Kafka producers and consumers, which is crucial for maintaining data integrity and avoiding duplicate processing of messages:

#### Idempotent Producers:
- **Configuration:** Our producers are configured to be idempotent, which prevents the duplication of messages in Kafka, even in the case of message retries. This is achieved by enabling the idempotence feature in the Kafka producer settings.

#### Idempotent Consumers:
- **Correlation IDs:** We implement idempotency on the consumer side through the business-specific implementation of correlation IDs. Each message processed by our system includes a unique correlation ID which consumers use to recognize and discard any duplicate messages. This ensures that each message is processed only once, even if it is received multiple times.

These measures are critical for avoiding data inconsistencies and ensuring that our system handles message processing reliably and efficiently.


## Prerequisites

- Docker
- Kafka running and accessible for event messaging.
- Access to a relational database.

## Setup

1. **Database Setup:**
    - Set up your chosen relational database.
    - Ensure the `player_bonus` table is created with the necessary schema.

2. **Kafka Topics:**
    - Ensure the `player-login-events` and `player-bonus-updates` topics are created in Kafka.

3. **Docker Compose:**
    - Navigate to your Docker Compose file located at `C:\Users\kosta\IdeaProjects\login\login\src\main\docker\docker-compose.yml`.
    - Run the following command to start the services:
      ```
      docker-compose up
      ```

4. **Entry:**
    - The service entry point is `http://localhost:8084/v1/login`. Make sure login service is running
    - A sample request JSON for testing:
      ```json
      {
        "userId": "123e4567-e89b-12d3-a456-426614174000"
      }
      ```

## Pending Optimization

- **Shared Library Implementation:** To optimize the microservice architecture, common models and utilities will be moved to a shared library. This reduces code duplication, ensures consistency across services, and simplifies updates or changes to shared components.

## Bonus Challenges

To enable the Bonus Microservice to efficiently manage high volumes of login events, the following scalability strategies can be implemented:

### Scalability
1. **Kafka Topic Partitioning:** Increase the number of partitions for the `player-login-events` Kafka topic. This will help distribute the event load more evenly across multiple instances of the Bonus Microservice.
2. **Microservices Scaling:** Utilize Docker to deploy multiple instances of the Bonus Microservice based on the load. This can be managed through Docker Compose scaling or using an orchestrator like Kubernetes, which can automatically scale services based on demand.

#### Security Measures for Kafka
1. **Encryption in Transit:**
    - Enable SSL/TLS for all data transmitted between Kafka brokers and clients to prevent unauthorized access to data in transit.
    - Configure SSL on both the broker and client sides by setting appropriate properties in the Kafka configuration files (`server.properties` for Kafka brokers and producer/consumer configuration for clients).

2. **Authentication:**
    - Implement client authentication using SSL certificates. This involves setting up a certificate authority, generating certificates for each client and broker, and configuring Kafka to require and validate these certificates.
    - Alternatively, use SASL (Simple Authentication and Security Layer) for Kafka clients and brokers. SASL/SCRAM (Salted Challenge Response Authentication Mechanism) or SASL/PLAIN can be used depending on the level of security required.

3. **Authorization:**
    - Use Kafka’s ACL (Access Control Lists) for fine-grained control over which principals (users or applications) are allowed to read or write to specific Kafka topics. This prevents unauthorized access to sensitive data.
    - Configure ACLs on a per-topic or per-resource basis to ensure that only authorized services can produce or consume messages from specific topics.

