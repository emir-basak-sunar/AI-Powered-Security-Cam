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

## Kurulum Rehberi (Step-by-Step)

### Ön Gereksinimler

| Araç | Versiyon | İndirme |
|------|----------|---------|
| Git | herhangi | https://git-scm.com |
| Docker Desktop | 4.x+ | https://www.docker.com/products/docker-desktop/ |
| Java JDK | 21+ | https://adoptium.net (Temurin) |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |
| Python | 3.11+ | https://www.python.org/downloads/ |
| PostgreSQL | 15+ | https://www.postgresql.org/download/ (sadece manuel kurulumda) |

> 💡 **Docker kurulumu yapıyorsanız** Java, Maven, Python ve PostgreSQL'i ayrıca kurmak zorunda değilsiniz. Docker her şeyi container içinde çalıştırır.

---

### Yol A: Docker ile Kurulum (Önerilen)

**Adım 1 — Projeyi klonlayın:**
```bash
git clone https://github.com/YOUR_USERNAME/AI-Powered-Security-Cam.git
cd AI-Powered-Security-Cam
```

**Adım 2 — `.env` dosyasını oluşturun:**
```bash
cp .env.example .env
```
Ardından `.env` dosyasını açın ve şu alanları doldurun:
```env
# Güçlü bir parola belirleyin
POSTGRES_PASSWORD=guclu_parola_buraya
DB_PASSWORD=guclu_parola_buraya

# JWT Secret oluşturun (minimum 32 byte, Base64):
#   Linux/Mac:  openssl rand -base64 32
#   PowerShell: [Convert]::ToBase64String((1..32 | % { Get-Random -Max 256 }) -as [byte[]])
JWT_SECRET=BURAYA_URETTIGINIZ_BASE64_KEY

# API Key oluşturun:
#   Linux/Mac:  openssl rand -hex 32
#   PowerShell: -join ((1..64) | % { '{0:x}' -f (Get-Random -Max 16) })
AI_API_KEY=BURAYA_URETTIGINIZ_HEX_KEY
```

**Adım 3 — Docker Compose ile başlatın:**
```bash
docker-compose up --build
```
> İlk çalıştırmada YOLO modelini indireceği ve Maven build yapacağı için **5-15 dakika** sürebilir.

**Adım 4 — Doğrulama:**
```bash
# AI Engine sağlık kontrolü
curl http://localhost:8000/health

# Management Server sağlık kontrolü
curl http://localhost:8080/actuator/health
```

Beklenen çıktılar:
- AI Engine: `{"status":"healthy","version":"1.0.0",...}`
- Management Server: `{"status":"UP"}`

---

### Yol B: Manuel Kurulum (Geliştirme İçin)

#### B.1 — PostgreSQL Kurulumu

**Windows (pgAdmin ile):**
1. https://www.postgresql.org/download/windows/ adresinden indirin ve kurun
2. Kurulum sırasında şifre belirleyin (örn: `sentry_password`)
3. pgAdmin veya `psql` ile veritabanı oluşturun:
```sql
CREATE DATABASE sentry_db;
CREATE USER sentry WITH PASSWORD 'sentry_password';
GRANT ALL PRIVILEGES ON DATABASE sentry_db TO sentry;
```

**Docker ile sadece PostgreSQL:**
```bash
docker run -d --name sentry-postgres \
  -e POSTGRES_DB=sentry_db \
  -e POSTGRES_USER=sentry \
  -e POSTGRES_PASSWORD=sentry_password \
  -p 5432:5432 \
  postgres:15-alpine
```

#### B.2 — Java Management Server

```bash
# 1. management-server dizinine gidin
cd management-server

# 2. Maven Wrapper yoksa Maven kullanın (mvn komutu PATH'te olmalı)
mvn clean package -DskipTests

# 3. Environment variable'ları ayarlayın ve çalıştırın
#    Windows PowerShell:
$env:DB_URL="jdbc:postgresql://localhost:5432/sentry_db"
$env:DB_USERNAME="sentry"
$env:DB_PASSWORD="sentry_password"
$env:JWT_SECRET="BURAYA_BASE64_KEY"
$env:AI_API_KEY="BURAYA_API_KEY"

java -jar target/management-server-1.0.0.jar

# Sunucu http://localhost:8080 adresinde çalışacak
```

#### B.3 — Python AI Engine

```bash
# 1. ai-engine dizinine gidin
cd ai-engine

# 2. Sanal ortam oluşturun
python -m venv .venv

# 3. Sanal ortamı aktif edin
#    Windows PowerShell:
.venv\Scripts\Activate.ps1
#    Linux/Mac:
#    source .venv/bin/activate

# 4. Bağımlılıkları yükleyin
pip install -r requirements.txt

# 5. Environment variable'ları ayarlayın
#    Windows PowerShell:
$env:BACKEND_URL="http://localhost:8080"
$env:AI_API_KEY="BURAYA_API_KEY"       # Management Server'dakiyle aynı!
$env:DEBUG="true"

# 6. Uygulamayı başlatın
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Sunucu http://localhost:8000 adresinde çalışacak
```

#### B.4 — Doğrulama

Her iki servisi de başlattıktan sonra:

| Test | Komut | Beklenen |
|------|-------|----------|
| AI Engine Health | `curl http://localhost:8000/health` | `{"status":"healthy",...}` |
| Management Health | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| Kullanıcı Kaydı | `curl -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d '{"username":"test","password":"test123"}'` | JWT token döner |
| Kamera Listesi | `curl http://localhost:8000/api/cameras` | `{"cameras":[]}` |

---

### Hızlı Referans — Portlar

| Servis | Port | URL |
|--------|------|-----|
| AI Engine | 8000 | http://localhost:8000 |
| Management Server | 8080 | http://localhost:8080 |
| PostgreSQL | 5432 | `jdbc:postgresql://localhost:5432/sentry_db` |

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
