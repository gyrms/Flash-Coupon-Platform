# API 명세서 (API Specification)

## 1. 개요

### 1.1 Base URLs
- **Coupon Service (Java)**: `http://localhost:8080/api`
- **Notification Service (Node.js)**: `ws://localhost:3000`

### 1.2 인증
- **Type**: Bearer Token (JWT)
- **Header**: `Authorization: Bearer {token}`

### 1.3 공통 응답 포맷

#### 성공 응답
```json
{
  "success": true,
  "data": {
    // 응답 데이터
  },
  "message": "Success"
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description"
  }
}
```

---

## 2. 쿠폰 서비스 API (Java Spring Boot)

### 2.1 인증 API

#### POST /auth/register
회원가입

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

**Response: 201 Created**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "createdAt": "2026-02-11T10:00:00Z"
  }
}
```

**Error Codes:**
- `EMAIL_ALREADY_EXISTS`: 이미 등록된 이메일
- `INVALID_EMAIL_FORMAT`: 잘못된 이메일 형식
- `WEAK_PASSWORD`: 비밀번호 강도 부족

---

#### POST /auth/login
로그인

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "name": "홍길동"
    }
  }
}
```

**Error Codes:**
- `INVALID_CREDENTIALS`: 이메일 또는 비밀번호 오류
- `ACCOUNT_LOCKED`: 계정 잠김 (로그인 실패 5회 이상)

---

#### POST /auth/logout
로그아웃

**Headers:**
```
Authorization: Bearer {token}
```

**Response: 200 OK**
```json
{
  "success": true,
  "message": "Successfully logged out"
}
```

---

### 2.2 쿠폰 이벤트 API

#### GET /coupons/events
이벤트 목록 조회

**Query Parameters:**
- `status` (optional): `active`, `scheduled`, `ended`
- `page` (optional): 페이지 번호 (default: 1)
- `size` (optional): 페이지 크기 (default: 20)

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "events": [
      {
        "eventId": 1,
        "title": "신규 가입 환영 쿠폰",
        "description": "첫 구매 시 사용 가능한 10% 할인 쿠폰",
        "totalCount": 1000,
        "issuedCount": 850,
        "remainingCount": 150,
        "startTime": "2026-02-11T09:00:00Z",
        "endTime": "2026-02-11T23:59:59Z",
        "status": "active",
        "maxPerUser": 3,
        "discountType": "percentage",
        "discountValue": 10
      }
    ],
    "pagination": {
      "currentPage": 1,
      "totalPages": 5,
      "totalItems": 100,
      "pageSize": 20
    }
  }
}
```

---

#### GET /coupons/events/{eventId}
이벤트 상세 조회

**Path Parameters:**
- `eventId`: 이벤트 ID

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "eventId": 1,
    "title": "신규 가입 환영 쿠폰",
    "description": "첫 구매 시 사용 가능한 10% 할인 쿠폰",
    "totalCount": 1000,
    "issuedCount": 850,
    "remainingCount": 150,
    "startTime": "2026-02-11T09:00:00Z",
    "endTime": "2026-02-11T23:59:59Z",
    "status": "active",
    "maxPerUser": 3,
    "userIssuedCount": 1,
    "canIssue": true,
    "discountType": "percentage",
    "discountValue": 10,
    "terms": "본 쿠폰은 1만원 이상 구매 시 사용 가능합니다."
  }
}
```

**Error Codes:**
- `EVENT_NOT_FOUND`: 존재하지 않는 이벤트

---

### 2.3 쿠폰 발급 API

#### POST /coupons/issue
쿠폰 발급 (선착순)

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "eventId": 1
}
```

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "couponId": 12345,
    "couponCode": "WELCOME-A1B2C3D4",
    "eventId": 1,
    "eventTitle": "신규 가입 환영 쿠폰",
    "discountType": "percentage",
    "discountValue": 10,
    "issuedAt": "2026-02-11T10:30:00Z",
    "expiresAt": "2026-03-11T23:59:59Z",
    "status": "active"
  },
  "message": "쿠폰이 발급되었습니다!"
}
```

**Error Codes:**
- `EVENT_NOT_STARTED`: 이벤트가 아직 시작되지 않음
- `EVENT_ENDED`: 이벤트가 종료됨
- `COUPON_SOLD_OUT`: 쿠폰이 모두 소진됨
- `USER_LIMIT_EXCEEDED`: 사용자 발급 한도 초과 (3개)
- `ALREADY_ISSUED`: 이미 발급받은 쿠폰

**Rate Limiting:**
- 동일 사용자: 초당 5회 요청 제한
- 429 Too Many Requests 반환

---

### 2.4 쿠폰 조회 API

#### GET /coupons/my
내 쿠폰함 조회

**Headers:**
```
Authorization: Bearer {token}
```

**Query Parameters:**
- `status` (optional): `active`, `used`, `expired`
- `page` (optional): 페이지 번호 (default: 1)
- `size` (optional): 페이지 크기 (default: 20)

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "coupons": [
      {
        "couponId": 12345,
        "couponCode": "WELCOME-A1B2C3D4",
        "eventTitle": "신규 가입 환영 쿠폰",
        "discountType": "percentage",
        "discountValue": 10,
        "issuedAt": "2026-02-11T10:30:00Z",
        "expiresAt": "2026-03-11T23:59:59Z",
        "usedAt": null,
        "status": "active"
      }
    ],
    "summary": {
      "totalCount": 5,
      "activeCount": 3,
      "usedCount": 1,
      "expiredCount": 1
    },
    "pagination": {
      "currentPage": 1,
      "totalPages": 1,
      "totalItems": 5,
      "pageSize": 20
    }
  }
}
```

---

#### GET /coupons/{couponId}
쿠폰 상세 조회

**Headers:**
```
Authorization: Bearer {token}
```

**Path Parameters:**
- `couponId`: 쿠폰 ID

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "couponId": 12345,
    "couponCode": "WELCOME-A1B2C3D4",
    "eventId": 1,
    "eventTitle": "신규 가입 환영 쿠폰",
    "description": "첫 구매 시 사용 가능한 10% 할인 쿠폰",
    "discountType": "percentage",
    "discountValue": 10,
    "minPurchaseAmount": 10000,
    "issuedAt": "2026-02-11T10:30:00Z",
    "expiresAt": "2026-03-11T23:59:59Z",
    "usedAt": null,
    "status": "active",
    "terms": "본 쿠폰은 1만원 이상 구매 시 사용 가능합니다."
  }
}
```

**Error Codes:**
- `COUPON_NOT_FOUND`: 존재하지 않는 쿠폰
- `UNAUTHORIZED_ACCESS`: 다른 사용자의 쿠폰

---

### 2.5 관리자 API (Phase 2)

#### POST /admin/events
이벤트 생성

**Headers:**
```
Authorization: Bearer {admin_token}
X-Admin-Role: ADMIN
```

**Request Body:**
```json
{
  "title": "봄맞이 특가 쿠폰",
  "description": "모든 상품 20% 할인",
  "totalCount": 500,
  "startTime": "2026-03-01T00:00:00Z",
  "endTime": "2026-03-31T23:59:59Z",
  "maxPerUser": 2,
  "discountType": "percentage",
  "discountValue": 20,
  "minPurchaseAmount": 20000,
  "validDays": 30
}
```

**Response: 201 Created**
```json
{
  "success": true,
  "data": {
    "eventId": 10,
    "title": "봄맞이 특가 쿠폰",
    "status": "scheduled",
    "createdAt": "2026-02-11T11:00:00Z"
  }
}
```

---

#### GET /admin/events/{eventId}/stats
이벤트 통계 조회

**Response: 200 OK**
```json
{
  "success": true,
  "data": {
    "eventId": 1,
    "title": "신규 가입 환영 쿠폰",
    "totalCount": 1000,
    "issuedCount": 850,
    "remainingCount": 150,
    "usedCount": 200,
    "expiredCount": 50,
    "issueRate": 85.0,
    "useRate": 23.5,
    "peakTPS": 1250,
    "avgResponseTime": 320,
    "errorCount": 15,
    "topUsers": [
      {
        "userId": 123,
        "email": "user@example.com",
        "issuedCount": 3
      }
    ],
    "hourlyIssues": [
      { "hour": "09:00", "count": 150 },
      { "hour": "10:00", "count": 300 }
    ]
  }
}
```

---

## 3. 알림 서비스 API (Node.js WebSocket)

### 3.1 WebSocket 연결

#### Connection
```javascript
const socket = io('ws://localhost:3000', {
  auth: {
    token: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
  }
});
```

---

### 3.2 이벤트 (Client → Server)

#### subscribe
특정 이벤트 구독

**Emit:**
```javascript
socket.emit('subscribe', {
  eventId: 1
});
```

**Response:**
```javascript
socket.on('subscribed', (data) => {
  // data: { eventId: 1, currentCount: 850 }
});
```

---

#### unsubscribe
이벤트 구독 취소

**Emit:**
```javascript
socket.emit('unsubscribe', {
  eventId: 1
});
```

---

### 3.3 이벤트 (Server → Client)

#### coupon:issued
쿠폰 발급 성공 알림

**Payload:**
```javascript
{
  type: 'coupon:issued',
  data: {
    couponId: 12345,
    couponCode: 'WELCOME-A1B2C3D4',
    eventId: 1,
    eventTitle: '신규 가입 환영 쿠폰',
    discountValue: 10,
    issuedAt: '2026-02-11T10:30:00Z',
    expiresAt: '2026-03-11T23:59:59Z'
  },
  message: '쿠폰이 발급되었습니다!'
}
```

---

#### coupon:failed
쿠폰 발급 실패 알림

**Payload:**
```javascript
{
  type: 'coupon:failed',
  data: {
    eventId: 1,
    reason: 'COUPON_SOLD_OUT'
  },
  message: '쿠폰이 모두 소진되었습니다.'
}
```

**Reason Codes:**
- `COUPON_SOLD_OUT`: 품절
- `USER_LIMIT_EXCEEDED`: 개인 한도 초과
- `EVENT_ENDED`: 이벤트 종료
- `SERVER_ERROR`: 서버 오류

---

#### coupon:soldout
쿠폰 품절 알림 (구독자 전체)

**Payload:**
```javascript
{
  type: 'coupon:soldout',
  data: {
    eventId: 1,
    eventTitle: '신규 가입 환영 쿠폰',
    soldoutAt: '2026-02-11T10:35:00Z'
  },
  message: '쿠폰이 모두 소진되었습니다.'
}
```

---

#### event:count
실시간 잔여 수량 업데이트

**Payload:**
```javascript
{
  type: 'event:count',
  data: {
    eventId: 1,
    remainingCount: 145,
    issuedCount: 855
  }
}
```

---

#### connection:heartbeat
연결 유지 (Ping)

**Emit (Client):**
```javascript
socket.emit('heartbeat');
```

**Response (Server):**
```javascript
socket.on('heartbeat:ack', (data) => {
  // data: { timestamp: 1707649800000 }
});
```

---

## 4. 에러 코드 정리

### 4.1 인증 에러 (AUTH_xxx)
| Code | HTTP Status | Description |
|------|-------------|-------------|
| AUTH_INVALID_TOKEN | 401 | 유효하지 않은 토큰 |
| AUTH_EXPIRED_TOKEN | 401 | 만료된 토큰 |
| AUTH_MISSING_TOKEN | 401 | 토큰 누락 |
| AUTH_INVALID_CREDENTIALS | 401 | 잘못된 인증 정보 |

### 4.2 쿠폰 에러 (COUPON_xxx)
| Code | HTTP Status | Description |
|------|-------------|-------------|
| COUPON_NOT_FOUND | 404 | 쿠폰을 찾을 수 없음 |
| COUPON_SOLD_OUT | 409 | 쿠폰 소진 |
| COUPON_ALREADY_ISSUED | 409 | 이미 발급받음 |

### 4.3 이벤트 에러 (EVENT_xxx)
| Code | HTTP Status | Description |
|------|-------------|-------------|
| EVENT_NOT_FOUND | 404 | 이벤트를 찾을 수 없음 |
| EVENT_NOT_STARTED | 400 | 이벤트 시작 전 |
| EVENT_ENDED | 400 | 이벤트 종료 |

### 4.4 사용자 에러 (USER_xxx)
| Code | HTTP Status | Description |
|------|-------------|-------------|
| USER_LIMIT_EXCEEDED | 429 | 발급 한도 초과 |
| USER_RATE_LIMIT | 429 | 요청 횟수 초과 |

---

## 5. Rate Limiting

### 5.1 일반 API
- **Limit**: 100 requests / minute / user
- **Header**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`

### 5.2 쿠폰 발급 API
- **Limit**: 5 requests / second / user
- **Response**: 429 Too Many Requests

---

## 6. 테스트 시나리오

### 6.1 정상 시나리오
```
1. 회원가입 → 로그인
2. 이벤트 목록 조회
3. 이벤트 상세 조회
4. WebSocket 연결 + 이벤트 구독
5. 쿠폰 발급 요청
6. 발급 성공 알림 수신
7. 내 쿠폰함 조회
```

### 6.2 예외 시나리오
```
1. 품절 상황에서 발급 시도
2. 한도 초과 후 발급 시도
3. 만료된 토큰으로 요청
4. WebSocket 연결 끊김 후 재연결
```

---

**문서 버전:** 1.0  
**최종 수정일:** 2026-02-11  
**작성자:** gyrms
