# DAI-2025-2026-Class-A-Practical-work-3-Sofia-Garfo-Henriques-Quentin-Eschmann-Thibault-Matthey

This repository is for the third practical work 3 for the DAI course.

## Table of Contents

- [Project Description](#project-description)
- [Group Members](#group-members)
- [Installation and Deployment](#installation-and-deployment)
- [API Documentation](#api-documentation)
- [Usage examples](#usage-examples)
- [Sources](#sources)

## Project Description

An HTTP-based client-server inventory management system enabling multiple users to concurrently view and modify shared warehouse inventory.
The server maintains inventory data in-memory using ConcurrentHashMap structures for thread-safe, concurrent access. 

The production deployment uses Traefik as a reverse proxy to handle HTTPS traffic with automatic SSL certificate provisioning via Let's Encrypt, ensuring secure communication.

This application is ideal for temporary inventory management at short-term events, conferences, festivals, or pop-ups where data persistence beyond the event duration is not required.

## Group Members 

This project was done by:

- Thibault Matthey
- Sofia Henriques Garfo
- Quentin Eschmann

## Installation and Deployment

### Local Deployment

To run locally and contribute, first clone the project, then compile the project:

```bash
./mvnw clean package
```

Run the application locally:

```bash
java -jar target/project3-1.0-SNAPSHOT.jar
```

Access the application at:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger`


### Docker Deployment

Build the Docker image:

```bash
docker build -t warehouse-api:latest .
```

Run the Docker container locally:

```bash
docker run -p 8080:8080 warehouse-api:latest
```

Access the application at:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger`


Publish your contribution to the container registry:

```bash
docker tag warehouse-api:latest ghcr.io/<username>/warehouse-api:latest
docker push ghcr.io/<username>/warehouse-api:<tag>
```

### Server Deployment

This application runs on a Virtual Machine. First contact the team to get access to the machine.

To start the API:

1. Start Traefik:

```bash
docker compose -f traefik/compose.yaml up -d
```

2. Start the API:

```bash
docker compose -f warehouse/compose.yaml up -d
```

3. Access the application at:
   - API: `https://warehouse.ddnsfree.com`
   - Swagger UI: `https://warehouse.ddnsfree.com/swagger`
   - Traefik Dashboard: `https://traefik.warehouse.ddnsfree.com`

## API Documentation

The application provides an HTTP API with CRUD operations for the following resources:

- **Authentication**: `/auth/login` - User authentication and session management
- **Users**: `/users/create`, `/users/list`, `/users/update`, `/users/delete` - User management
- **Inventory**: `/inventory/create`, `/inventory/list`, `/inventory/update`, `/inventory/delete` - Inventory item management

- **Default Admin Credentials**:
Email: admin@example.com
Password: admin

The complete API specification can be accessed at:
- Local: `http://localhost:8080/swagger`
- Production: `https://warehouse.ddnsfree.com/swagger`

## Usage Examples

### Authentication

Login with default admin credentials:

```bash
curl -X 'POST' \
  'https://warehouse.ddnsfree.com/auth/login' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "admin@example.com",
  "password": "admin"
}'
```

Output: 
200 Login Sucessfull

```bash
 content-length: 0 
 content-type: text/plain 
 date: Sat,17 Jan 2026 15:30:36 GMT 
 expires: Thu,01 Jan 1970 00:00:00 GMT 
```


### Inventory Operations

Create a few items:

```bash

curl -X 'POST' \
  'https://warehouse.ddnsfree.com/create' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "name":"chair",
  "num": 10
}'
```


Output:
201 Item Created Sucefully


```json
{
  "id": 1,
  "name": "chair",
  "num": 1
}
```


Get the list of all items:

```bash
curl -X 'GET' \
  'https://warehouse.ddnsfree.com/inventory/list' \
  -H 'accept: application/json'
```

Output:
200 Item retrieved succesfully

```json
[
  {
    "id": 1,
    "name": "chair",
    "num": 10
  }
]
```

Get an Item by id that doesn't exist:

```bash
curl -X 'GET' \
  'https://warehouse.ddnsfree.com/list/3' \
  -H 'accept: application/json'
```

Output:
404 - Item not found

```json
{
  "title": "Item not found.",
  "status": 404,
  "type": "https://javalin.io/documentation#notfoundresponse",
  "details": {}
}
```

Update an item:

```bash
curl -X 'PUT' \
  'https://warehouse.ddnsfree.com/inventory/update/1' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "chairs",
  "num": 14
}'
```

Output:
200 Item updated Succesfully

```json
{
  "id": 1,
  "name": "chairs",
  "num": 14
}
```

Adding another item with the same name:

```bash
curl -X 'POST' \
  'https://warehouse.ddnsfree.com/inventory/create' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "chairs",
  "num": 15
}'
```

Output:
409 - Item with the same name already exists

```json
{
  "title": "Item with the same name already exists.",
  "status": 409,
  "type": "https://javalin.io/documentation#conflictresponse",
  "details": {}
}
```


Delete an item:

```bash
curl -X 'DELETE' \
  'https://warehouse.ddnsfree.com/inventory/remove/1' \
  -H 'accept: */*'
```

Output:
200 Item deleted Sucessfully

```bash
 content-length: 0 
 content-type: text/plain 
 date: Sat,17 Jan 2026 16:00:41 GMT 
```

### User management

Create a user:

```bash
curl -X 'POST' \
  'https://warehouse.ddnsfree.com/users/create' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "password",
  "role": "1"
}'
```

Output:
2001 - User created sucessfully
```bash
 content-length: 0 
 content-type: text/plain 
 date: Sat,17 Jan 2026 15:45:51 GMT 
```

List users:

```bash
curl -X 'GET' \
  'https://warehouse.ddnsfree.com/users/list' \
  -H 'accept: application/json'
```

Output:
200 - Users retrieved succesfully

```json
[
  {
    "id": 0,
    "firstName": "Admin",
    "lastName": "User",
    "email": "admin@example.com",
    "role": "ADMIN"
  },
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "role": "READ"
  }
]
```

Update a user:

```bash
curl -X 'PUT' \
  'https://warehouse.ddnsfree.com/users/update/1' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "firstName": "Martin",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "password",
  "role": "1"
}'
```

Output:
Code 200 - User updated sucessfully

```json
 content-length: 0 
 content-type: text/plain 
 date: Sat,17 Jan 2026 16:07:32 GMT 
```

Delete a user that doesn't exist: 

```bash
curl -X 'DELETE' \
  'https://warehouse.ddnsfree.com/users/remove/2' \
  -H 'accept: */*'
```

Output:
200 - User not found

```json
User not found.
```

## Sources

- DAI Course Materials
- Stack Overflow  
- GitHub Copilot 