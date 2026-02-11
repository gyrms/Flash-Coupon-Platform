# Flash Coupon Platform API 명세서

## Base URL
```
http://localhost:8080/api/v1
```

---

## 1. 쿠폰 생성

### Request
```http
POST /api/v1/coupons
Content-Type: application/json
```

```json
{
  "name": "선착순 1000원 할인 쿠폰",
  "totalQuantity": 100,
  "startAt": "2025-01-01T00:00:00",
  "endAt": "2025-12-31T23:59:59"
}
```

### Response
```json
{
  "id": 1,
  "name": "선착순 1000원 할인 쿠폰",
  "totalQuantity": 100,
  "remainingQuantity": 100,
  "startAt": "2025-01-01T00:00:00",
  "endAt": "2025-12-31T23:59:59",
  "createdAt": "2025-01-15T10:30:00"
}
```

---

## 2. 쿠폰 조회

### Request
```http
GET /api/v1/coupons/{id}
```

### Response
```json
{
  "id": 1,
  "name": "선착순 1000원 할인 쿠폰",
  "totalQuantity": 100,
  "remainingQuantity": 50,
  "startAt": "2025-01-01T00:00:00",
  "endAt": "2025-12-31T23:59:59",
  "createdAt": "2025-01-15T10:30:00"
}
```

---

## 3. 쿠폰 발급 (핵심 API)

### Request
```http
POST /api/v1/coupons/{id}/issue
Content-Type: application/json
```

```json
{
  "userId": 12345
}
```

### Response - 성공 (200 OK)
```json
{
  "success": true,
  "message": "쿠폰 발급 성공",
  "issueId": 1,
  "couponId": 1,
  "userId": 12345
}
```

### Response - 실패 (409 Conflict)
```json
{
  "success": false,
  "message": "쿠폰이 모두 소진되었습니다.",
  "issueId": null,
  "couponId": null,
  "userId": null
}
```

### 실패 사유
| 메시지 | 설명 |
|--------|------|
| "쿠폰이 모두 소진되었습니다." | 재고 없음 |
| "이미 발급받은 쿠폰입니다." | 중복 발급 시도 |
| "쿠폰 발급 기간이 아니거나 품절되었습니다." | 기간 외 발급 시도 |

---

## 4. 잔여 수량 조회

### Request
```http
GET /api/v1/coupons/{id}/stock
```

### Response
```json
{
  "couponId": 1,
  "remainingStock": 50,
  "issuedCount": 50
}
```

---

## 에러 응답

### 400 Bad Request
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "쿠폰명은 필수입니다"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "존재하지 않는 쿠폰입니다: 999"
}
```

---

## 실행 방법

```bash
# 전체 실행
docker-compose up -d

# 빌드 후 실행
docker-compose up --build

# 로그 확인
docker-compose logs -f coupon-service
```

---

## 테스트 시나리오

### 동시성 테스트 (curl 예시)
```bash
# 1. 쿠폰 생성
curl -X POST http://localhost:8080/api/v1/coupons \
  -H "Content-Type: application/json" \
  -d '{
    "name": "선착순 테스트 쿠폰",
    "totalQuantity": 100,
    "startAt": "2025-01-01T00:00:00",
    "endAt": "2025-12-31T23:59:59"
  }'

# 2. 쿠폰 발급
curl -X POST http://localhost:8080/api/v1/coupons/1/issue \
  -H "Content-Type: application/json" \
  -d '{"userId": 1}'

# 3. 재고 확인
curl http://localhost:8080/api/v1/coupons/1/stock
```
