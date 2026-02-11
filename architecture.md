# 시스템 아키텍처 설계 (Architecture Design)

## 1. 시스템 개요

### 1.1 아키텍처 패턴
- **MSA (Micro Service Architecture)**: 쿠폰 서비스와 알림 서비스 분리
- **Event-Driven Architecture**: 메시지 큐 기반 비동기 통신
- **CQRS 패턴**: 읽기/쓰기 분리로 성능 최적화

### 1.2 설계 원칙
- **확장성**: 수평 확장 가능한 무상태 서비스
- **가용성**: 장애 격리 및 빠른 복구
- **성능**: 캐싱과 비동기 처리
- **보안**: 인증/인가 및 데이터 암호화

---

## 2. 전체 아키텍처

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         Client Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Web App    │  │  Mobile App  │  │  Admin Panel │      │
│  │   (React)    │  │   (Flutter)  │  │   (React)    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      Gateway Layer                           │
│                   ┌──────────────┐                           │
│                   │  Nginx / ALB │                           │
│                   │ Load Balancer│                           │
│                   └──────┬───────┘                           │
└──────────────────────────┼─────────────────────────────────┘
                           │
          ┌────────────────┴────────────────┐
          ▼                                 ▼
┌─────────────────────┐          ┌─────────────────────┐
│  Coupon Service     │          │ Notification Service│
│  (Java Spring Boot) │◄────────►│    (Node.js)        │
│                     │          │                     │
│  ┌──────────────┐   │          │  ┌──────────────┐  │
│  │ REST API     │   │          │  │  WebSocket   │  │
│  │ Controller   │   │          │  │   Server     │  │
│  └──────┬───────┘   │          │  └──────────────┘  │
│         │           │          │                     │
│  ┌──────▼───────┐   │          └──────────┬──────────┘
│  │   Business   │   │                     │
│  │    Logic     │   │                     │
│  └──────┬───────┘   │                     │
│         │           │                     │
│  ┌──────▼───────┐   │                     │
│  │ Redis Lock   │   │                     │
│  │ + Cache      │   │                     │
│  └──────┬───────┘   │                     │
└─────────┼───────────┘                     │
          │                                 │
          ▼                                 ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ PostgreSQL   │  │    Redis     │  │  RabbitMQ    │      │
│  │   (Master)   │  │  (Cache +    │  │  (Message    │      │
│  │              │  │   Lock)      │  │   Queue)     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
          │                   │                  │
          ▼                   ▼                  ▼
┌─────────────────────────────────────────────────────────────┐
│                   Monitoring Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Prometheus   │  │   Grafana    │  │     Logs     │      │
│  │  (Metrics)   │  │ (Dashboard)  │  │  (ELK/Cloud) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 서비스 별 상세 설계

### 3.1 Coupon Service (Java Spring Boot)

#### 3.1.1 레이어 구조
```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database (PostgreSQL + Redis)
```

#### 3.1.2 주요 컴포넌트

**Controller:**
```java
@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    - POST /issue          // 쿠폰 발급
    - GET  /my             // 내 쿠폰 조회
    - GET  /events         // 이벤트 목록
    - GET  /events/{id}    // 이벤트 상세
}
```

**Service:**
```java
@Service
public class CouponService {
    - issueCoupon()         // 쿠폰 발급 (동시성 제어)
    - checkUserLimit()      // 사용자 한도 체크
    - generateCouponCode()  // 쿠폰 코드 생성
    - publishEvent()        // RabbitMQ 이벤트 발행
}
```

**Redis Service:**
```java
@Service
public class RedisLockService {
    - acquireLock()      // 분산 락 획득
    - releaseLock()      // 분산 락 해제
    - incrementCounter() // 원자적 카운터 증가
    - decrementStock()   // 재고 감소
}
```

#### 3.1.3 쿠폰 발급 플로우

```
[사용자 요청]
    ↓
1. JWT 인증 검증
    ↓
2. Redis에서 이벤트 상태 조회 (캐시)
    ↓
3. 기본 검증 (시작/종료 시간, 재고)
    ↓
4. Redis 분산 락 획득 (event:{eventId}:lock)
    ↓
5. Redis에서 재고 감소 (DECR)
    ↓  ↓
    ↓  └─→ [실패] 품절 → 락 해제 → 에러 응답
    ↓
6. DB에 발급 내역 저장 (Transaction)
    ↓
7. 쿠폰 코드 생성
    ↓
8. Redis 캐시 업데이트
    ↓
9. RabbitMQ에 이벤트 발행
    ↓
10. Redis 락 해제
    ↓
[성공 응답 반환]
```

---

### 3.2 Notification Service (Node.js)

#### 3.2.1 구조
```
WebSocket Server (Socket.io)
    ↓
Event Handler (RabbitMQ Consumer)
    ↓
Connection Manager
    ↓
Client Connections
```

#### 3.2.2 주요 컴포넌트

**WebSocket Server:**
```javascript
io.on('connection', (socket) => {
    // JWT 검증
    // 연결 등록
    // 이벤트 구독 처리
    // Heartbeat 처리
});
```

**RabbitMQ Consumer:**
```javascript
channel.consume('coupon.issued', (msg) => {
    // 메시지 파싱
    // 해당 사용자 소켓 찾기
    // 알림 전송
    // ACK 처리
});
```

**Connection Manager:**
```javascript
class ConnectionManager {
    connections: Map<userId, Socket>
    
    addConnection(userId, socket)
    removeConnection(userId)
    getConnection(userId)
    broadcast(eventId, message)
}
```

#### 3.2.3 알림 전송 플로우

```
[쿠폰 발급 완료 (Coupon Service)]
    ↓
1. RabbitMQ에 메시지 발행
   { type: 'coupon.issued', userId, couponData }
    ↓
2. Notification Service가 메시지 수신
    ↓
3. Connection Manager에서 사용자 소켓 조회
    ↓
4. WebSocket으로 실시간 알림 전송
    ↓
5. 클라이언트가 알림 수신 및 UI 업데이트
```

---

## 4. 데이터 설계

### 4.1 PostgreSQL 스키마

#### users
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
```

#### coupon_events
```sql
CREATE TABLE coupon_events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    total_count INTEGER NOT NULL,
    issued_count INTEGER DEFAULT 0,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    max_per_user INTEGER DEFAULT 1,
    discount_type VARCHAR(20) NOT NULL, -- 'percentage', 'fixed'
    discount_value DECIMAL(10,2) NOT NULL,
    min_purchase_amount DECIMAL(10,2) DEFAULT 0,
    valid_days INTEGER DEFAULT 30,
    status VARCHAR(20) DEFAULT 'scheduled', -- 'scheduled', 'active', 'ended'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_status ON coupon_events(status);
CREATE INDEX idx_events_time ON coupon_events(start_time, end_time);
```

#### user_coupons
```sql
CREATE TABLE user_coupons (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    event_id BIGINT NOT NULL REFERENCES coupon_events(id),
    coupon_code VARCHAR(50) UNIQUE NOT NULL,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'active', -- 'active', 'used', 'expired'
    UNIQUE(user_id, event_id, status) -- 중복 발급 방지
);

CREATE INDEX idx_coupons_user ON user_coupons(user_id);
CREATE INDEX idx_coupons_event ON user_coupons(event_id);
CREATE INDEX idx_coupons_code ON user_coupons(coupon_code);
CREATE INDEX idx_coupons_status ON user_coupons(status);
```

### 4.2 Redis 데이터 구조

#### 쿠폰 재고 관리
```
Key: event:{eventId}:stock
Type: String (Integer)
Value: 남은 쿠폰 수량
TTL: 이벤트 종료 시간 + 1일
```

#### 분산 락
```
Key: event:{eventId}:lock
Type: String
Value: 락 소유자 ID
TTL: 5초 (자동 해제)
```

#### 사용자 발급 카운터
```
Key: event:{eventId}:user:{userId}:count
Type: String (Integer)
Value: 발급받은 수량
TTL: 이벤트 종료 시간 + 1일
```

#### 이벤트 캐시
```
Key: event:{eventId}:info
Type: Hash
Fields: title, totalCount, issuedCount, status, etc.
TTL: 1시간
```

#### 세션 관리
```
Key: session:{token}
Type: Hash
Fields: userId, email, expiresAt
TTL: 1시간
```

---

## 5. 동시성 제어 전략

### 5.1 Redis 분산 락 (Redlock)

```java
public boolean acquireLock(String eventId, String requestId) {
    String lockKey = "event:" + eventId + ":lock";
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, requestId, 5, TimeUnit.SECONDS);
    return Boolean.TRUE.equals(acquired);
}

public void releaseLock(String eventId, String requestId) {
    String lockKey = "event:" + eventId + ":lock";
    String script = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";
    redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(lockKey),
        requestId
    );
}
```

### 5.2 낙관적 락 (Optimistic Lock)

```java
@Entity
public class CouponEvent {
    @Version
    private Long version;  // JPA 낙관적 락
    
    // ...
}
```

### 5.3 재고 감소 (Atomic Operation)

```java
public boolean decrementStock(Long eventId) {
    String stockKey = "event:" + eventId + ":stock";
    Long remaining = redisTemplate.opsForValue()
        .decrement(stockKey);
    
    if (remaining != null && remaining >= 0) {
        return true;  // 성공
    } else {
        // 재고 부족 → 원복
        redisTemplate.opsForValue().increment(stockKey);
        return false;
    }
}
```

---

## 6. 메시징 아키텍처

### 6.1 RabbitMQ Exchange & Queue 구조

```
┌──────────────────────────────────────────┐
│        Topic Exchange                     │
│        coupon.exchange                    │
└───────┬──────────────┬───────────────────┘
        │              │
        │              │
   Routing Key    Routing Key
   coupon.issued  coupon.soldout
        │              │
        ▼              ▼
┌────────────────┐  ┌──────────────┐
│ Queue:         │  │ Queue:       │
│ coupon.issued  │  │ coupon.soldout│
└────────┬───────┘  └──────┬───────┘
         │                 │
         ▼                 ▼
  ┌──────────────────────────────┐
  │  Notification Service        │
  │  (Consumer)                  │
  └──────────────────────────────┘
```

### 6.2 메시지 포맷

**쿠폰 발급 이벤트:**
```json
{
  "type": "coupon.issued",
  "timestamp": "2026-02-11T10:30:00Z",
  "data": {
    "userId": 123,
    "couponId": 12345,
    "eventId": 1,
    "couponCode": "WELCOME-A1B2C3D4",
    "discountValue": 10
  }
}
```

**품절 이벤트:**
```json
{
  "type": "coupon.soldout",
  "timestamp": "2026-02-11T10:35:00Z",
  "data": {
    "eventId": 1,
    "eventTitle": "신규 가입 환영 쿠폰"
  }
}
```

---

## 7. 캐싱 전략

### 7.1 캐시 레이어

```
Application
    ↓
Local Cache (Caffeine) - 1분 TTL
    ↓
Redis Cache - 1시간 TTL
    ↓
PostgreSQL
```

### 7.2 캐시 무효화 (Cache Invalidation)

**Write-Through 패턴:**
```java
public void updateEvent(CouponEvent event) {
    // 1. DB 업데이트
    eventRepository.save(event);
    
    // 2. Redis 캐시 업데이트
    String cacheKey = "event:" + event.getId() + ":info";
    redisTemplate.opsForHash().putAll(cacheKey, eventToMap(event));
    
    // 3. Local 캐시 무효화
    localCache.invalidate(event.getId());
}
```

---

## 8. 모니터링 & 로깅

### 8.1 메트릭 수집 (Prometheus)

**Spring Boot Actuator:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**수집 메트릭:**
- API 요청 수 (Counter)
- 응답 시간 (Histogram)
- 동시 접속자 수 (Gauge)
- 에러 발생 수 (Counter)
- 쿠폰 발급 성공률 (Gauge)

### 8.2 로깅 구조

**구조화된 로그 (JSON):**
```json
{
  "timestamp": "2026-02-11T10:30:00Z",
  "level": "INFO",
  "service": "coupon-service",
  "traceId": "abc123",
  "spanId": "def456",
  "userId": 123,
  "action": "COUPON_ISSUED",
  "eventId": 1,
  "duration": 250,
  "success": true
}
```

---

## 9. 보안 설계

### 9.1 인증/인가

**JWT 토큰 구조:**
```json
{
  "sub": "user@example.com",
  "userId": 123,
  "role": "USER",
  "iat": 1707649800,
  "exp": 1707653400
}
```

**인증 플로우:**
```
1. 로그인 → JWT 발급 (Access + Refresh)
2. API 요청 시 Header에 Access Token 포함
3. Spring Security Filter에서 검증
4. 만료 시 Refresh Token으로 재발급
```

### 9.2 Rate Limiting

**Bucket4j (Token Bucket):**
```java
@RateLimiter(name = "couponIssue", fallbackMethod = "rateLimitFallback")
public CouponResponse issueCoupon(Long eventId, Long userId) {
    // 쿠폰 발급 로직
}
```

---

## 10. 배포 아키텍처

### 10.1 로컬 개발 (Docker Compose)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
  redis:
    image: redis:7
  rabbitmq:
    image: rabbitmq:3-management
  coupon-service:
    build: ./coupon-service
  notification-service:
    build: ./notification-service
  nginx:
    image: nginx:latest
```

### 10.2 프로덕션 (AWS)

```
┌─────────────────────────────────────┐
│         Route 53 (DNS)              │
└────────────┬────────────────────────┘
             ↓
┌─────────────────────────────────────┐
│   Application Load Balancer         │
└────────┬────────────────────────────┘
         ↓
┌────────────────────┬────────────────┐
│  ECS Fargate       │  ECS Fargate   │
│  Coupon Service    │  Notification  │
│  (Auto Scaling)    │  Service       │
└────────┬───────────┴───────┬────────┘
         ↓                   ↓
┌────────────────┐    ┌──────────────┐
│ RDS PostgreSQL │    │ ElastiCache  │
│  (Multi-AZ)    │    │   Redis      │
└────────────────┘    └──────────────┘
```

---

## 11. 확장 계획

### Phase 3 이후 개선 사항:
- Kafka로 RabbitMQ 대체 (더 높은 처리량)
- Read Replica 추가 (읽기 성능 향상)
- CDN 추가 (정적 자원)
- Service Mesh (Istio) 도입
- Kubernetes 마이그레이션

---

**문서 버전:** 1.0  
**최종 수정일:** 2026-02-11  
**작성자:** gyrms
