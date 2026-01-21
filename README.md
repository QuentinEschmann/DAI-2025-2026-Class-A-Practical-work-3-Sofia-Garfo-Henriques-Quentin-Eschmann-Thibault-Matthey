# DAI-2025-2026-Class-A-Practical-work-3-Sofia-Garfo-Henriques-Quentin-Eschmann-Thibault-Matthey
This repository is for the third practical work 3 for the DAI course.

## Table of Contents

- [Project Description](#project-description)
- [Group Members](#group-members)
- [Installation and Deployment](#installation-and-deployment)
- [API Documentation](#api-documentation)
- [Usage examples](#usage-examples)
- [Cache implementation](#cache-implementation)
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

This application runs on a Virtual Machine. If you wish contact the team to get access to the machine or use your own virtual machine to deploy.

#### Setting up your own server 

To create your own virtual machine please follow these instructions: [Obtain a virtual machine on a cloud provider](https://github.com/heig-vd-dai-course/heig-vd-dai-course/tree/main/11.03-ssh-and-scp/01-course-material#obtain-a-virtual-machine-on-a-cloud-provider)

Once you've set up the virtual machine you'll need to get your own domain name!

To configure DNS for a new deployment you'll need two subdomains, one for the api and another for traefik
Go to Dynu.com (or your prefered DNS provider), create an account and configure two DNS records, they must point to your virtual machine.

For example we used Dynu as the DNS provider with the following records:

| Type | Name | Ip | Purpose |
|------|------|-------|---------|
| A | warehouse.ddnsfree.com | 20.250.19.128 | Main API and Swagger UI |
| A | traefik.warehouse.ddnsfree.com | 20.250.19.128 | Traefik Dashboard |

If you use Dynu, the default are A/AAAA records.  If you use another provider be sure to select A Record when adding your domain.

You can use `nslookup <yourdomain>>` to verify your DNS records are correctly configured.
Please note that DNS changes may take 5-10 minutes to propagate globally.

### Deploying on the virtual machines:

To connect to the virtual machine :
```bash
ssh ubuntu@20.250.19.128
```

If your using your own virtual machine, be sure to replace the ip address by the one of your machine


To start the API:

1. Start Traefik:

```bash
docker compose -f traefik/compose.yaml up -d
```

2. Start the API:

```bash
docker compose -f warehouse/compose.yaml up -d
```

Note : If your using your own virtual machine you can copy the Docker compose files to your machine using scp, don't forget to update the domain names!

3. Access the application at:
   - API: `https://warehouse.ddnsfree.com`
   - Swagger UI: `https://warehouse.ddnsfree.com/swagger`
   - Traefik Dashboard: `https://traefik.warehouse.ddnsfree.com`

   Or navigate to the domain names you have configured.

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
201 Item Created Sucessfully


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
200 Item retrieved sucesfully

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


## Cache implementation

The cache is currently implemented for the inventory and the user management system. When you list an item (or all items) or a user (or all users), you will get en Etag in your response. This Etag can be used as header in your next request. If the requested ressource has not been modified since your last call, the server will send a 304 response wich means that you can reuse the data queried earlier. This system reduces server response time and ressources usage on the server. 


## Sources

- DAI Course Materials
- Stack Overflow  
- GitHub Copilot 