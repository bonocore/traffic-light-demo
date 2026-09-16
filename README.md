# 🚦 Traffic Light Controller

A modern, cloud-native **Traffic Light Controller** microservice built with **Quarkus 3.x (Java 21)**. Features a realistic 3D photorealistic display, secured REST APIs, multi-tenant API key management, and real-time Server-Sent Events (SSE).

---

## ✨ Features

* **Dual User Interfaces**:
  * **Display Screen (`/`)**: A clean, distraction-free visual display featuring a photorealistic 3-aspect (Red, Amber, Green) traffic light with curved 3D sun visors, Fresnel LED dot-matrix lenses, dynamic bloom glow, and countdown HUD.
  * **Operator Console (`/control.html`)**: Mission control deck with manual phase overrides, mode toggles (`AUTO`, `MANUAL`, `EMERGENCY`), multi-key administration, live cURL generator, and streaming HTTP traffic logs.
* **Real-Time Synchronization (SSE)**:
  * State changes made by external REST clients (`curl`, Postman, IoT devices) are instantly pushed to the browser over `/api/traffic-light/events` with zero lag.
## 🔐 Authentication & Security

The system enforces authentication across both web consoles and REST APIs:

### 1. HTTP Basic Auth (User & Password)
Used to log into the **Backend Operator Console** (`/control.html`), **Swagger UI** (`/q/swagger-ui/`), or call REST APIs directly:

| Username | Default Password | Role | Permissions |
| :--- | :--- | :--- | :--- |
| `admin` | `admin123` | `ADMIN` | Full control: Access Console, Swagger UI, control lights, manage API keys |
| `operator` | `operator123` | `OPERATOR` | Access Console, Swagger UI, control lights |

*Passwords can be customized via environment variables `ADMIN_PASSWORD` and `OPERATOR_PASSWORD`.*

### 2. Multi-Tenant API Keys
External REST clients can also authenticate using the `X-API-KEY` header (or `Authorization: Bearer <key>`):
* Default keys: `admin-key-2026`, `operator-key-2026`, `dispatch-key-2026`.
* Dynamic keys can be generated and revoked at `/api/keys` or from the Operator Console.

### 3. Public vs. Protected Endpoints
* **Public**: Frontend Display Screen (`/`), CSS/JS assets, `GET /api/traffic-light/state`, `GET /api/traffic-light/events` (SSE).
* **Protected by User & Password**: Operator Console (`/control.html`), Swagger UI (`/q/swagger-ui*`), OpenAPI Spec (`/q/openapi*`).
* **Protected by User/Pass OR API Key**: All mutation REST endpoints (`POST /api/traffic-light/*`, `POST /api/keys`, `DELETE /api/keys/*`).

---

## 🚀 Getting Started Locally

### Prerequisites
* **Java 21**
* **Maven 3.9+**

### Run in Development Mode
```bash
mvn quarkus:dev
```

Open your browser:
* **Display Screen (FE)**: [http://localhost:7860/](http://localhost:7860/)
* **Operator Console (BE)**: [http://localhost:7860/control.html](http://localhost:7860/control.html)
* **OpenAPI / Swagger UI**: [http://localhost:7860/q/swagger-ui/](http://localhost:7860/q/swagger-ui/)

### Run Automated Tests
```bash
mvn clean test
```

---

## 📡 REST API Reference

All mutating endpoints require the header:
```http
X-API-KEY: admin-key-2026
```

| Method | Endpoint | Security | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/traffic-light/state` | Public | Get active light state, mode, and countdown |
| `GET` | `/api/traffic-light/events` | Public | Real-time Server-Sent Events (SSE) stream |
| `POST` | `/api/traffic-light/state` | **Secured** | Set manual state (`RED`, `AMBER`, `GREEN`, `FLASHING_AMBER`, `OFF`) |
| `POST` | `/api/traffic-light/mode` | **Secured** | Set mode (`AUTO`, `MANUAL`, `EMERGENCY`) |
| `POST` | `/api/traffic-light/next` | **Secured** | Advance to next logical phase |
| `GET` | `/api/keys` | **Secured** | List all registered API keys |
| `POST` | `/api/keys` | **Secured** | Generate a new API key with name and role |
| `DELETE` | `/api/keys/{id}` | **Secured** | Revoke an API key immediately |

### Quick cURL Examples

```bash
# 1. Read current state
curl http://localhost:7860/api/traffic-light/state

# 2. Change light to GREEN (Authorized)
curl -X POST http://localhost:7860/api/traffic-light/state \
  -H "X-API-KEY: admin-key-2026" \
  -H "Content-Type: application/json" \
  -d '{"state": "GREEN"}'

# 3. Trigger Emergency All-Stop
curl -X POST http://localhost:7860/api/traffic-light/mode \
  -H "X-API-KEY: dispatch-key-2026" \
  -H "Content-Type: application/json" \
  -d '{"mode": "EMERGENCY"}'

# 4. Generate a new API key
curl -X POST http://localhost:7860/api/keys \
  -H "X-API-KEY: admin-key-2026" \
  -H "Content-Type: application/json" \
  -d '{"name": "Highway Camera 4", "role": "OPERATOR"}'
```

---

## ☁️ Google Cloud Run Deployment

Deploy directly from your repository in Google Cloud Shell or using `gcloud`:

```bash
gcloud run deploy traffic-light-demo \
  --source . \
  --region europe-west1 \
  --allow-unauthenticated \
  --memory 256Mi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 1 \
  --timeout 300s
```

---

## 📄 License
MIT
