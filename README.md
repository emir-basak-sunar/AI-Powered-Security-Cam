# SENTRY-AI Security Platform

A scalable, production-ready microservices system for autonomous security monitoring with AI-powered detection.

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        SENTRY-AI SYSTEM                          │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────┐    HTTP/JSON     ┌────────────────────┐    │
│  │   AI ENGINE     │ ───────────────► │  MANAGEMENT SERVER │    │
│  │   (Python)      │   API Key Auth   │   (Java Spring)    │    │
│  │                 │                   │                    │    │
│  │  • YOLOv11      │                   │  • JWT Auth        │    │
│  │  • Audio        │                   │  • PostgreSQL      │    │
│  │  • FastAPI      │                   │  • WebSocket       │    │
│  └────────┬────────┘                   └─────────┬──────────┘    │
│           │                                      │               │
│           │ Cameras                  WebSocket   │               │
│           ▼                          /topic/     ▼               │
│      ┌─────────┐                   live-alerts  ┌─────────┐     │
│      │ Streams │                         │      │ Clients │     │
│      └─────────┘                         └─────►└─────────┘     │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Git

### Running the System

1. **Clone and configure:**
   ```bash
   cd SecurityCAMAI
   cp .env.example .env  # Edit with your secrets
   ```

2. **Start all services:**
   ```bash
   docker-compose up --build
   ```

3. **Access services:**
   - AI Engine: `http://localhost:8000`
   - Management Server: `http://localhost:8080`
   - Health checks:
     - `http://localhost:8000/health`
     - `http://localhost:8080/actuator/health`

## API Endpoints

### Authentication
```bash
# Register
POST /api/v1/auth/register
{"username": "user1", "password": "password123"}

# Login
POST /api/v1/auth/login
{"username": "user1", "password": "password123"}
```

### Alerts (AI Engine → Management Server)
```bash
# Create alert (API Key auth)
POST /api/v1/alerts
X-API-KEY: your-api-key
{
  "cameraId": "cam-01",
  "alertType": "VISUAL",
  "description": "Person detected",
  "imageBase64": "base64-encoded-image",
  "timestamp": 1707436800000
}
```

### Cameras
```bash
# Create camera (JWT auth)
POST /api/v1/cameras
{"name": "Entrance Cam", "location": "Main Gate", "streamUrl": "rtsp://..."}

# List cameras
GET /api/v1/cameras
```

## WebSocket Connection

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    stompClient.subscribe('/topic/live-alerts', function(message) {
        console.log('New alert:', JSON.parse(message.body));
    });
});
```

## Default Credentials

- **Admin User:** `admin` / `admin123`

> ⚠️ **Change these in production!**

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_DB` | Database name | `sentry_db` |
| `POSTGRES_USER` | DB username | `sentry` |
| `POSTGRES_PASSWORD` | DB password | `sentry_password` |
| `JWT_SECRET` | JWT signing key | - |
| `AI_API_KEY` | AI service auth key | - |
| `DETECTION_CONFIDENCE` | YOLOv11 threshold | `0.6` |
| `AUDIO_THRESHOLD` | Audio alert threshold | `0.7` |

## Project Structure

```
SecurityCAMAI/
├── docker-compose.yml
├── .env
├── ai-engine/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── app/
│       ├── main.py
│       ├── config.py
│       ├── vision_engine.py
│       ├── audio_engine.py
│       └── service_connector.py
├── management-server/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/sentry/
│       ├── config/
│       ├── controller/
│       ├── entity/
│       ├── repository/
│       ├── service/
│       └── dto/
└── init-db/
    └── init.sql
```

## License

MIT
