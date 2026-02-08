# SENTRY-AI GitHub Issues

Bu dosya, projenize GitHub Issues olarak ekleyebileceğiniz geliştirme önerilerini ve potansiyel hata düzeltmelerini içerir.

---

## 🔴 Priority: Critical

### Issue #1: JWT Secret Güvenliği
**Labels:** `security`, `critical`, `enhancement`

**Açıklama:**
Mevcut JWT secret hardcoded ve Base64 encoded olarak saklanıyor. Production ortamında bu ciddi bir güvenlik açığı oluşturur.

**Yapılması Gerekenler:**
- [ ] JWT secret'ı minimum 256-bit random key olarak generate et
- [ ] Secret'ı environment variable yerine Vault veya AWS Secrets Manager'a taşı
- [ ] Key rotation mekanizması ekle

**Kod Konumu:** `application.yml` satır 21

---

### Issue #2: API Key Brute Force Koruması
**Labels:** `security`, `critical`

**Açıklama:**
AI Engine'den gelen API key validasyonu rate limiting içermiyor. Brute force saldırılarına açık.

**Yapılması Gerekenler:**
- [ ] Rate limiting ekle (Spring Boot Bucket4j veya Resilience4j)
- [ ] Failed attempts logging ve alerting ekle
- [ ] IP-based blocking mekanizması

**Kod Konumu:** `ApiKeyAuthenticationFilter.java`

---

### Issue #3: Database Password Güvenliği
**Labels:** `security`, `critical`

**Açıklama:**
PostgreSQL credentials `.env` dosyasında plain text olarak saklanıyor.

**Yapılması Gerekenler:**
- [ ] Docker secrets kullan
- [ ] Production için managed database (AWS RDS, Azure PostgreSQL) kullan
- [ ] Connection encryption (SSL/TLS) aktif et

---

## 🟠 Priority: High

### Issue #4: Vision Engine Memory Leak Potansiyeli
**Labels:** `bug`, `performance`, `high-priority`

**Açıklama:**
`CameraHandler` sınıfında frame queue dolu olduğunda eski frame'ler drop edilmiyor, bu memory leak'e yol açabilir.

**Yapılması Gerekenler:**
- [ ] Queue overflow handling ekle
- [ ] Frame disposal mekanizması implement et
- [ ] Memory profiling ile test et

**Kod Konumu:** `vision_engine.py` satır 45-50

```python
# Öneri:
if self._frame_queue.full():
    try:
        self._frame_queue.get_nowait()  # Drop oldest frame
    except Empty:
        pass
```

---

### Issue #5: WebSocket Authentication Eksik
**Labels:** `security`, `high-priority`

**Açıklama:**
WebSocket endpoint `/ws` authentication olmadan erişime açık. Herkes `/topic/live-alerts`'e subscribe olabilir.

**Yapılması Gerekenler:**
- [ ] STOMP interceptor ile JWT validation ekle
- [ ] WebSocket handshake sırasında auth kontrol et
- [ ] Unauthorized subscription'ları engelle

**Kod Konumu:** `WebSocketConfig.java`

---

### Issue #6: Alert Image Data Boyut Limiti
**Labels:** `bug`, `database`, `high-priority`

**Açıklama:**
`imageData` TEXT column'da saklanıyor ancak boyut limiti yok. Büyük Base64 image'lar database'i şişirebilir.

**Yapılması Gerekenler:**
- [ ] Image compression ekle (JPEG quality 60-70)
- [ ] Maximum image size validation (örn. 500KB)
- [ ] Büyük image'lar için S3/MinIO storage kullan

**Kod Konumu:** `Alert.java`, `AlertService.java`

---

### Issue #7: Service Connector Retry Mekanizması
**Labels:** `enhancement`, `reliability`

**Açıklama:**
`ServiceConnector` sınıfında backend'e bağlantı başarısız olduğunda retry mekanizması yok.

**Yapılması Gerekenler:**
- [ ] Exponential backoff ile retry ekle
- [ ] Circuit breaker pattern implement et
- [ ] Failed alert'leri local queue'da tut ve retry et

**Kod Konumu:** `service_connector.py`

```python
# Öneri: tenacity library kullan
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=2, max=10))
async def send_alert(self, ...):
    ...
```

---

## 🟡 Priority: Medium

### Issue #8: Audio Engine Platform Bağımsızlığı
**Labels:** `enhancement`, `compatibility`

**Açıklama:**
PyAudio Windows'ta çalışırken Linux/Docker'da sorun çıkarabilir. Alternatif backend'ler desteklenmeli.

**Yapılması Gerekenler:**
- [ ] sounddevice library'yi alternatif olarak ekle
- [ ] Platform detection ile otomatik backend seçimi
- [ ] Docker container'da ALSA/PulseAudio config

---

### Issue #9: Health Check Detaylı Bilgi
**Labels:** `enhancement`, `monitoring`

**Açıklama:**
Health endpoint'leri sadece "healthy" dönüyor. Detaylı component durumu eksik.

**Yapılması Gerekenler:**
- [ ] Database connectivity check
- [ ] AI model loading status
- [ ] Memory/CPU usage bilgisi
- [ ] Active camera/listener sayısı

**Öneri Response:**
```json
{
  "status": "healthy",
  "components": {
    "database": "up",
    "aiModel": "loaded",
    "cameras": 3,
    "memoryUsage": "45%"
  }
}
```

---

### Issue #10: Logging ve Tracing Standardizasyonu
**Labels:** `enhancement`, `observability`

**Açıklama:**
Distributed tracing eksik. Service'ler arası request'ler takip edilemiyor.

**Yapılması Gerekenler:**
- [ ] OpenTelemetry integration
- [ ] Correlation ID header'ı ekle
- [ ] Structured logging (JSON format)
- [ ] Grafana/Prometheus metrics

---

### Issue #11: Camera Stream Reconnection
**Labels:** `enhancement`, `reliability`

**Açıklama:**
RTSP stream koptuğunda otomatik reconnect yok. Camera offline olduğunda manual restart gerekiyor.

**Yapılması Gerekenler:**
- [ ] Automatic reconnection with backoff
- [ ] Connection status tracking
- [ ] Offline camera alerting

**Kod Konumu:** `vision_engine.py` `_capture_loop` method

---

### Issue #12: Input Validation Güçlendirme
**Labels:** `security`, `enhancement`

**Açıklama:**
Request body validation mevcut ama XSS ve SQL injection için ek kontroller gerekli.

**Yapılması Gerekenler:**
- [ ] Input sanitization ekle
- [ ] Max length validation
- [ ] HTML entity encoding
- [ ] Parameterized query kullanımını doğrula (JPA zaten yapıyor)

---

## 🟢 Priority: Low

### Issue #13: API Versioning
**Labels:** `enhancement`, `api`

**Açıklama:**
API v1 prefix var ama version negotiation veya deprecation stratejisi yok.

**Yapılması Gerekenler:**
- [ ] Version header support
- [ ] Deprecation warning mechanism
- [ ] API documentation (OpenAPI/Swagger)

---

### Issue #14: Test Coverage
**Labels:** `testing`, `quality`

**Açıklama:**
Unit ve integration test'ler eksik.

**Yapılması Gerekenler:**
- [ ] Python: pytest ile unit tests
- [ ] Java: JUnit 5 + Mockito
- [ ] Integration tests with Testcontainers
- [ ] CI/CD pipeline (GitHub Actions)

---

### Issue #15: Docker Image Optimization
**Labels:** `performance`, `devops`

**Açıklama:**
Docker image'lar optimize edilmemiş. AI Engine image'ı çok büyük olabilir.

**Yapılması Gerekenler:**
- [ ] Multi-stage build optimization
- [ ] Alpine base image kullan (PyTorch için dikkat)
- [ ] Layer caching optimize et
- [ ] .dockerignore dosyası ekle

---

### Issue #16: Alert Notification Channels
**Labels:** `feature`, `enhancement`

**Açıklama:**
Sadece WebSocket notification var. E-mail, SMS, Push notification desteklenmeli.

**Yapılması Gerekenler:**
- [ ] Email notification (SMTP/SendGrid)
- [ ] SMS integration (Twilio)
- [ ] Push notification (Firebase)
- [ ] Notification preferences per user

---

### Issue #17: Camera Grouping ve Zoning
**Labels:** `feature`

**Açıklama:**
Kameralar flat list halinde. Zone/Group bazlı organizasyon gerekli.

**Yapılması Gerekenler:**
- [ ] Zone entity ekle
- [ ] Camera-Zone relation
- [ ] Zone-based alert filtering
- [ ] Dashboard zone view

---

### Issue #18: Alert Deduplication
**Labels:** `enhancement`, `performance`

**Açıklama:**
Aynı kameradan kısa sürede gelen benzer alert'ler deduplicate edilmiyor.

**Yapılması Gerekenler:**
- [ ] Time-based deduplication (örn. 5 saniye cooldown)
- [ ] Image similarity check
- [ ] Alert grouping/clustering

---

### Issue #19: Graceful Shutdown
**Labels:** `reliability`, `enhancement`

**Açıklama:**
Service shutdown sırasında in-flight request'ler kaybolabilir.

**Yapılması Gerekenler:**
- [ ] SIGTERM handler ekle
- [ ] Pending alert'leri flush et
- [ ] Camera stream'leri düzgün kapat
- [ ] Database connection graceful close

---

### Issue #20: Configuration Validation
**Labels:** `enhancement`, `reliability`

**Açıklama:**
Startup'ta configuration validation eksik. Yanlış config ile service çöküyor.

**Yapılması Gerekenler:**
- [ ] Required config check
- [ ] Config value validation
- [ ] Startup fail-fast with clear error messages

---

## 📊 Issue Summary Matrix

| # | Title | Priority | Type | Effort |
|---|-------|----------|------|--------|
| 1 | JWT Secret Güvenliği | 🔴 Critical | Security | Medium |
| 2 | API Key Brute Force | 🔴 Critical | Security | Medium |
| 3 | Database Password | 🔴 Critical | Security | Low |
| 4 | Memory Leak | 🟠 High | Bug | Medium |
| 5 | WebSocket Auth | 🟠 High | Security | Medium |
| 6 | Image Size Limit | 🟠 High | Bug | Medium |
| 7 | Retry Mechanism | 🟠 High | Enhancement | Low |
| 8 | Audio Platform | 🟡 Medium | Compatibility | Medium |
| 9 | Health Check | 🟡 Medium | Monitoring | Low |
| 10 | Logging/Tracing | 🟡 Medium | Observability | High |
| 11 | Camera Reconnect | 🟡 Medium | Reliability | Medium |
| 12 | Input Validation | 🟡 Medium | Security | Low |
| 13 | API Versioning | 🟢 Low | API | Low |
| 14 | Test Coverage | 🟢 Low | Quality | High |
| 15 | Docker Optimization | 🟢 Low | DevOps | Low |
| 16 | Notification Channels | 🟢 Low | Feature | High |
| 17 | Camera Zoning | 🟢 Low | Feature | Medium |
| 18 | Alert Deduplication | 🟢 Low | Performance | Medium |
| 19 | Graceful Shutdown | 🟢 Low | Reliability | Low |
| 20 | Config Validation | 🟢 Low | Reliability | Low |
