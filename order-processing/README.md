# Order Processing

Spring Boot ecommerce order-processing API with product catalog, cart, address, order, payment, authentication, and role-based admin/seller workflows.

## Tech Stack

- Java 24
- Spring Boot 4.0.1
- Spring Web MVC
- Spring Data JPA
- Spring Security with JWT
- PostgreSQL
- Stripe Java SDK
- Springdoc OpenAPI / Swagger UI
- Maven Wrapper

## Prerequisites

- JDK 24
- PostgreSQL running locally
- Stripe secret key for payment-related calls

## Configuration

The default configuration is in `src/main/resources/application.properties`.

Default local database settings:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
spring.datasource.username=postgres
spring.datasource.password=admin@123
```

Create the local database before starting the app:

```bash
createdb ecommerce
```

Set the Stripe secret key in your shell:

```bash
export STRIPE_SECRET_KEY=sk_test_your_key_here
```

Other useful defaults:

- API server: `http://localhost:8080`
- Frontend URL allowed by config: `http://localhost:5173/`
- Image base URL: `http://localhost:8080/images`
- Uploaded product image folder: `images/`

## Run Locally

Use the Maven wrapper from the project root:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

## Test

```bash
./mvnw test
```

## Build

```bash
./mvnw clean package
```

The built artifact is created under `target/`.

## API Documentation

After the app starts, open:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

## Authentication and Roles

Authentication routes are available under:

```text
/api/auth
```

The app seeds these local users on startup if they do not already exist:

| Username | Email | Password | Roles |
| --- | --- | --- | --- |
| `user1` | `user1@example.com` | `password1` | `ROLE_USER` |
| `seller1` | `seller1@example.com` | `password2` | `ROLE_SELLER` |
| `admin` | `admin@example.com` | `adminPass` | `ROLE_USER`, `ROLE_SELLER`, `ROLE_ADMIN` |

Public endpoints are permitted without authentication. Admin endpoints require `ROLE_ADMIN`, and seller endpoints require `ROLE_ADMIN` or `ROLE_SELLER`.

## Main Endpoint Groups

- Auth: `/api/auth/signin`, `/api/auth/signup`, `/api/auth/signout`
- Categories: `/api/public/categories`, `/api/admin/categories`
- Products: `/api/public/products`, `/api/admin/products`, `/api/seller/products`
- Cart: `/api/cart/create`, `/api/carts`
- Addresses: `/api/addresses`, `/api/users/addresses`
- Orders: `/api/order/users/payments/{paymentMethod}`, `/api/admin/orders`, `/api/seller/orders`
- Payments: `/api/order/stripe-client-secret`
- Analytics: `/api/admin/app/analytics`
- Static product images: `/images/**`

## Project Structure

```text
src/main/java/com/ecommerce/project
├── config
├── controller
├── exceptions
├── model
├── payload
├── repositories
├── security
├── service
└── util
```

## Notes

- Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, so tables are updated automatically for local development.
- Do not commit real database passwords, JWT secrets, or Stripe keys. Prefer environment-specific configuration for shared environments.
