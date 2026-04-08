# Order Processing

Full-stack ecommerce order-processing project with a Spring Boot backend and a React/Vite frontend. The backend contains product catalog, category management, cart, address, order, payment, authentication, role-based admin/seller endpoints, Stripe payment intent creation, and Kafka-based order reconciliation scaffolding.

## Current Status

This repository is actively in transition. The current Java package is `com.ecommerce.project`, the backend Maven artifact is `sb-ecom`, and the main class is `OrderProcessingApplication`.

Important current notes:

- The backend uses Spring Boot `4.0.1` and Java `24` in `pom.xml`.
- The checked-in `application.yaml` currently points to MySQL defaults, while `pom.xml` currently contains the PostgreSQL runtime driver. Add the MySQL JDBC driver or switch the datasource back to PostgreSQL before expecting the app to connect cleanly.
- Kafka code is present and `spring-kafka` has been added to `pom.xml`, but the Docker Compose file does not currently start a Kafka broker.
- `sh ./mvnw -DskipTests dependency:go-offline` succeeds and resolves Spring Kafka, Stripe, Lombok, ModelMapper, Spring Security, Springdoc, and the rest of the Maven dependencies.
- `sh ./mvnw -q -DskipTests compile` currently fails on unrelated Lombok/accessor-style compile errors, for example missing generated getters/setters such as `getEmail()`, `getCartId()`, `setRoles(...)`, and `log` from `@Slf4j`.
- The local Maven wrapper script is not executable in this checkout. Use `sh ./mvnw ...` on macOS/Linux unless you restore execute permissions.

## Tech Stack

Backend:

- Java 24
- Spring Boot 4.0.1
- Spring Web MVC
- Spring Data JPA
- Spring Security with JWT cookies
- Spring Kafka
- Springdoc OpenAPI / Swagger UI
- Stripe Java SDK
- ModelMapper
- Lombok
- Maven Wrapper

Frontend:

- React 19
- Vite 7
- React Router 7
- Redux Toolkit
- Axios
- Material UI
- Stripe React SDK
- Tailwind CSS / PostCSS

Local infrastructure:

- Docker Compose
- MySQL container
- Redis container
- Kafka expected by backend code, but not yet included in the current Compose file

## Repository Layout

```text
.
|-- client/                         React/Vite frontend
|   |-- src/api                     Axios API client
|   |-- src/components              UI, auth, catalog, admin, checkout, cart
|   |-- src/store                   Redux actions and reducers
|   |-- src/hooks                   Filter hooks
|   `-- package.json
|-- docker/
|   |-- docker-compose.yml          MySQL and Redis local services
|   `-- data.sql                    Local seed SQL for sports_center
|-- src/main/java/com/ecommerce/project/
|   |-- config                      App constants, Swagger, MVC, ModelMapper
|   |-- controller                  REST controllers
|   |-- exceptions                  API exception handling
|   |-- kafka                       Kafka topic, consumer, producer, event types
|   |-- model                       JPA entities
|   |-- payload                     Request and response DTOs
|   |-- repositories                Spring Data repositories
|   |-- scheduler                   Scheduled order reconciliation trigger
|   |-- security                    JWT and Spring Security setup
|   |-- service                     Business services
|   `-- util                        Auth helper utilities
|-- src/main/resources/application.yaml
|-- pom.xml
|-- mvnw
`-- README.md
```

## Backend Configuration

The backend config file is:

```text
src/main/resources/application.yaml
```

Current checked-in server and datasource defaults:

```yaml
server:
  port: 8081

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/sports_center}
    username: ${SPRING_DATASOURCE_USERNAME:sports_user}
    password: ${SPRING_DATASOURCE_PASSWORD:sports_password}
  application:
    name: SportsCenter
  jpa:
    hibernate:
      ddl-auto: update
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  data:
    redis:
      host: localhost
      port: 6379
```

The Java code also reads these properties, which are not all present in the current `application.yaml` and should be added before a complete backend run:

```yaml
frontend:
  url: http://localhost:5173

spring:
  app:
    jwtSecret: replace-with-a-long-dev-secret
    jwtExpirationMs: 86400000
  ecom:
    app:
      jwtCookieName: ecommerceJwt
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: order-reconciliation-group

kafka:
  topic:
    order-reconciliation: order-reconciliation-trigger

reconciliation:
  kafka:
    topic: order-reconciliation-trigger

stripe:
  secret:
    key: sk_test_replace_me

project:
  image: images/

image:
  base:
    url: http://localhost:8081/images
```

Do not commit real JWT or Stripe secrets.

## Database Setup

The current Docker Compose file starts MySQL and Redis:

```bash
cd docker
docker compose up -d
```

MySQL container settings:

```text
Container: sports-center-mysql
Host: localhost
Port: 3306
Database: sports_center
App user: sports_user
App password: sports_password
Root user: root
Root password: root
```

Redis container settings:

```text
Container: sports-center-redis-container
Host: localhost
Port: 6379
```

The seed SQL file is mounted at:

```text
docker/data.sql -> /docker-entrypoint-initdb.d/data.sql
```

Docker only runs init scripts the first time a MySQL volume is initialized. If the volume already exists, import the seed manually:

```bash
docker exec -i sports-center-mysql mysql -uroot -proot < docker/data.sql
```

Check tables:

```bash
docker exec -it sports-center-mysql mysql -usports_user -psports_password sports_center
```

Then:

```sql
SHOW TABLES;
SELECT * FROM Product LIMIT 10;
```

## MySQL Workbench Connection

Use:

```text
Connection Method: Standard TCP/IP
Hostname: 127.0.0.1
Port: 3306
Username: sports_user
Password: sports_password
Default Schema: sports_center
```

Fallback root login:

```text
Username: root
Password: root
Default Schema: sports_center
```

## IntelliJ Database Connection

The project contains IntelliJ datasource metadata for:

```text
sports_center@localhost
jdbc:mysql://localhost:3306/sports_center
user: sports_user
```

IntelliJ may still prompt for the password. Use:

```text
sports_password
```

## Kafka Setup

Kafka classes are under:

```text
src/main/java/com/ecommerce/project/kafka
```

Main Kafka pieces:

- `KafkaTopicConfig` creates the reconciliation topic.
- `KafkaConsumerConfig` configures `JsonDeserializer<ReconciliationTriggerEvent>`.
- `ReconciliationEventProducer` publishes reconciliation trigger events.
- `ReconciliationEventConsumer` consumes events and calls `OrderService.promotePendingToProcessing()`.
- `OrderReconciliationScheduler` publishes scheduled reconciliation events every 5 minutes.

The Kafka dependency is:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

Resolved Kafka artifacts include:

```text
spring-kafka-4.0.1.jar
kafka-clients-4.1.1.jar
spring-messaging-7.0.2.jar
```

The current Docker Compose file does not start Kafka. Add a Kafka broker service or run Kafka separately on `localhost:9092` before enabling Kafka flows.

## Stripe Setup

Stripe code is in:

```text
src/main/java/com/ecommerce/project/service/StripeService.java
src/main/java/com/ecommerce/project/service/StripeServiceImpl.java
```

The Stripe dependency is:

```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>29.3.0</version>
</dependency>
```

The following imports are valid when Maven is loaded:

```java
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
```

Required property:

```yaml
stripe:
  secret:
    key: sk_test_replace_me
```

## Security And Seed Users

Authentication is handled with Spring Security and JWT cookies. Security configuration is in:

```text
src/main/java/com/ecommerce/project/security/WebSecurityConfig.java
```

Public routes:

- `/api/auth/**`
- `/api/public/**`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/images/**`
- `OPTIONS /**`

Role-protected routes:

- `/api/admin/**` requires `ROLE_ADMIN`
- `/api/seller/**` requires `ROLE_ADMIN` or `ROLE_SELLER`
- all other routes require authentication

The backend seeds these users through a `CommandLineRunner` if they do not exist:

| Username | Email | Password | Roles |
| --- | --- | --- | --- |
| `user1` | `user1@example.com` | `password1` | `ROLE_USER` |
| `seller1` | `seller1@example.com` | `password2` | `ROLE_SELLER` |
| `admin` | `admin@example.com` | `adminPass` | `ROLE_USER`, `ROLE_SELLER`, `ROLE_ADMIN` |

## Backend REST API

Base URL:

```text
http://localhost:8081
```

Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8081/v3/api-docs
```

### Auth

Sign in:

```http
POST /api/auth/signin
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "adminPass"
}
```

Sign up:

```http
POST /api/auth/signup
Content-Type: application/json
```

```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "password123",
  "role": ["user"]
}
```

Get current username:

```http
GET /api/auth/username
```

Get current user:

```http
GET /api/auth/user
```

Sign out:

```http
POST /api/auth/signout
```

Get sellers:

```http
GET /api/auth/sellers?pageNumber=0&pageSize=10&sortBy=userId&sortOrder=asc
```

### Categories

List public categories:

```http
GET /api/public/categories?pageNumber=0&pageSize=10&sortBy=categoryId&sortOrder=asc
```

Create category:

```http
POST /api/admin/categories
Content-Type: application/json
```

```json
{
  "categoryName": "Shoes"
}
```

Update category:

```http
PUT /api/admin/categories/{categoryId}
Content-Type: application/json
```

```json
{
  "categoryName": "Running Shoes"
}
```

Delete category:

```http
DELETE /api/admin/categories/{categoryId}
```

### Products

Create product as admin:

```http
POST /api/admin/categories/{categoryId}/product
Content-Type: application/json
```

```json
{
  "productName": "Yonex Badminton Racket",
  "description": "Lightweight racket for indoor play",
  "quantity": 20,
  "price": 120.0,
  "discount": 10.0,
  "specialPrice": 108.0
}
```

Create product as seller:

```http
POST /api/seller/categories/{categoryId}/product
Content-Type: application/json
```

Get public products:

```http
GET /api/public/products?pageNumber=0&pageSize=10&sortBy=productId&sortOrder=asc
```

Get products by category:

```http
GET /api/public/categories/{categoryId}/products?pageNumber=0&pageSize=10&sortBy=productId&sortOrder=asc
```

Search by keyword:

```http
GET /api/public/products/keyword/{keyword}?pageNumber=0&pageSize=10&sortBy=productId&sortOrder=asc
```

Update product:

```http
PUT /api/admin/products/{productId}
Content-Type: application/json
```

```json
{
  "productName": "Yonex Badminton Racket Pro",
  "description": "Updated description",
  "quantity": 15,
  "price": 150.0,
  "discount": 5.0,
  "specialPrice": 142.5
}
```

Delete product:

```http
DELETE /api/admin/products/{productId}
```

Upload product image:

```http
PUT /api/admin/products/{productId}/image
Content-Type: multipart/form-data
```

Form field:

```text
image=<file>
```

Seller product management routes:

```http
GET /api/seller/products?pageNumber=0&pageSize=10&sortBy=productId&sortOrder=asc
PUT /api/seller/products/{productId}
DELETE /api/seller/products/{productId}
PUT /api/seller/products/{productId}/image
```

### Cart

Create cart:

```http
POST /api/cart/create
```

Add product to cart:

```http
POST /api/carts/products/{productId}/quantity/{quantity}
```

Get all carts:

```http
GET /api/carts
```

Get current user's cart:

```http
GET /api/carts/users/cart
```

Update product quantity in cart:

```http
PUT /api/cart/products/{productId}/quantity/{operation}
```

`operation` is interpreted by service code, for example values such as `increment` or `decrement` depending on implementation.

Remove a product from a cart:

```http
DELETE /api/carts/{cartId}/product/{productId}
```

### Addresses

Create address:

```http
POST /api/addresses
Content-Type: application/json
```

```json
{
  "street": "123 MG Road",
  "buildingName": "Apt 4B",
  "city": "Bengaluru",
  "state": "Karnataka",
  "country": "India",
  "pincode": "560001"
}
```

Get all addresses:

```http
GET /api/addresses
```

Get address by ID:

```http
GET /api/addresses/{addressId}
```

Get current user's addresses:

```http
GET /api/users/addresses
```

Update address:

```http
PUT /api/addresses/{addressId}
Content-Type: application/json
```

Delete address:

```http
DELETE /api/addresses/{addressId}
```

### Orders

Place order:

```http
POST /api/order/users/payments/{paymentMethod}
Content-Type: application/json
```

```json
{
  "addressId": 1,
  "pgName": "stripe",
  "pgPaymentId": "pi_example",
  "pgStatus": "succeeded",
  "pgResponseMessage": "Payment succeeded"
}
```

Create Stripe client secret:

```http
POST /api/order/stripe-client-secret
Content-Type: application/json
```

```json
{
  "amount": 12000,
  "currency": "usd",
  "description": "Order payment",
  "email": "user1@example.com",
  "name": "User One",
  "address": {
    "street": "123 MG Road",
    "buildingName": "Apt 4B",
    "city": "Bengaluru",
    "state": "Karnataka",
    "country": "India",
    "pincode": "560001"
  }
}
```

Admin orders:

```http
GET /api/admin/orders?pageNumber=0&pageSize=10&sortBy=totalAmount&sortOrder=asc
PUT /api/admin/orders/{orderId}/status
```

Status update body:

```json
{
  "status": "PROCESSING"
}
```

Seller orders:

```http
GET /api/seller/orders?pageNumber=0&pageSize=10&sortBy=totalAmount&sortOrder=asc
PUT /api/seller/orders/{orderId}/status
```

### Analytics

Admin analytics:

```http
GET /api/admin/app/analytics
```

## Frontend Setup

Frontend directory:

```bash
cd client
```

Install dependencies:

```bash
npm install
```

Create `client/.env`:

```env
VITE_BACK_END_URL=http://localhost:8081
VITE_FRONTEND_URL=http://localhost:5173
```

Start the Vite dev server:

```bash
npm run dev
```

Common frontend commands:

```bash
npm run dev
npm run build
npm run lint
npm run preview
```

## Backend Commands

Resolve dependencies:

```bash
sh ./mvnw -DskipTests dependency:go-offline
```

Compile:

```bash
sh ./mvnw -q -DskipTests compile
```

Run:

```bash
sh ./mvnw spring-boot:run
```

Run tests:

```bash
sh ./mvnw test
```

Package:

```bash
sh ./mvnw clean package
```

Windows:

```cmd
mvnw.cmd spring-boot:run
```

## Local Startup Order

1. Start Docker infrastructure:

```bash
cd docker
docker compose up -d
cd ..
```

2. Import MySQL seed data if the Docker volume already existed:

```bash
docker exec -i sports-center-mysql mysql -uroot -proot < docker/data.sql
```

3. Add the missing runtime properties in `application.yaml` or environment variables.

4. Ensure the backend database driver matches the configured datasource:

- MySQL config requires `mysql-connector-j`.
- PostgreSQL config requires `postgresql`.

5. Start Kafka separately if Kafka reconciliation should run.

6. Start the backend:

```bash
sh ./mvnw spring-boot:run
```

7. Start the frontend:

```bash
cd client
npm run dev
```

## Known Issues To Resolve

These are current codebase issues discovered during local verification:

- `pom.xml` currently has `postgresql` but `application.yaml` defaults to a MySQL JDBC URL.
- Kafka dependency is present, but Docker Compose does not provide a Kafka broker.
- The main app class does not currently show `@EnableScheduling`, while `OrderReconciliationScheduler` uses `@Scheduled`.
- `ReconciliationEventProducer.publishTrigger` currently accepts a `String` parameter but calls methods like `getEventId()` on it. That method signature should likely accept `ReconciliationTriggerEvent`.
- `KafkaConsumerConfig` uses `@Slf4j`, but compile currently reports `cannot find symbol: variable log`, indicating Lombok annotation processing is not active or not being honored.
- The project has many compile errors for Lombok-generated getters, setters, and constructors. Confirm Lombok annotation processing in Maven/IntelliJ and verify the model/DTO classes match service expectations.

## Troubleshooting

If IntelliJ shows dependency imports as red but Maven resolves them:

1. Open the Maven tool window.
2. Click Reload All Maven Projects.
3. Rebuild the project.
4. If still red, use File > Invalidate Caches... > Invalidate and Restart.

If `./mvnw` gives permission denied:

```bash
sh ./mvnw spring-boot:run
```

or restore executable permission:

```bash
chmod +x mvnw
```

If MySQL rejects credentials:

- Check whether an old Docker volume was already initialized.
- MySQL environment variables only apply on first container initialization.
- Use `docker exec` to inspect users and databases.
- Re-import `docker/data.sql` manually if the schema is empty.

If Stripe imports are red:

```bash
sh ./mvnw -DskipTests dependency:go-offline
```

The expected jar is:

```text
~/.m2/repository/com/stripe/stripe-java/29.3.0/stripe-java-29.3.0.jar
```

If Kafka imports are red:

```bash
sh ./mvnw -DskipTests dependency:go-offline
```

The expected jar is:

```text
~/.m2/repository/org/springframework/kafka/spring-kafka/4.0.1/spring-kafka-4.0.1.jar
```

## Security Notes

- Do not commit real `stripe.secret.key`.
- Do not commit production JWT secrets.
- Do not reuse the documented local database passwords outside local development.
- Treat seeded local users as development-only accounts.
