# Flash Coupon Platform

범용 선착순 쿠폰 발급 + 실시간 알림 시스템

## 📋 프로젝트 개요

대용량 트래픽 처리가 가능한 MSA(Micro Service Architecture) 기반의 쿠폰 발급 플랫폼입니다.
실시간 알림 시스템과 Redis 기반 동시성 제어를 통해 안정적인 선착순 이벤트를 지원합니다.

### 🎯 목표
- **동시 사용자**: 10,000명
- **쿠폰 수량**: 1,000개/이벤트
- **응답 시간**: 500ms 이내
- **발급 성공률**: 99% 이상
- **알림 지연**: 1초 이내

## 🏗️ 시스템 아키텍처

```
[사용자] 
   ↓
[Nginx (Load Balancer)]
   ↓
   ├─→ [Java Spring Boot] - 쿠폰 발급 API
   │      ↓
   │   [Redis] - 선착순 제어, 캐싱
   │      ↓
   │   [PostgreSQL] - 영구 저장
   │
   └─→ [Node.js] - 실시간 알림 서버
          ↓
       [WebSocket]
          ↓
       [RabbitMQ] - 메시지 큐
```

## 🛠️ 기술 스택

### 백엔드 서비스
- **쿠폰 서비스 (Java)**: Spring Boot 3.x, Spring Data JPA
- **알림 서비스 (Node.js)**: Express, Socket.io
- **API Gateway**: Nginx

### 데이터 저장
- **PostgreSQL**: 사용자, 쿠폰, 발급 이력
- **Redis**: 선착순 제어, 발급 카운터, 세션 관리, 캐싱

### 메시징
- **RabbitMQ**: 서비스 간 비동기 통신

### 프론트엔드
- **React**: 사용자/관리자 페이지
- **Socket.io-client**: 실시간 알림 수신

### 인프라
- **로컬 개발**: Docker Compose (라즈베리파이)
- **프로덕션**: AWS (EC2, RDS, ElastiCache)
- **모니터링**: Prometheus + Grafana
- **부하테스트**: JMeter

## 📂 프로젝트 구조

```
Flash-Coupon-Platform/
├── docs/                       # 문서
│   ├── requirements.md         # 요구사항 명세서
│   ├── architecture.md         # 아키텍처 설계
│   └── api-spec.md            # API 명세서
├── coupon-service/            # 쿠폰 발급 서비스 (Java)
├── notification-service/      # 알림 서비스 (Node.js)
├── frontend/                  # React 프론트엔드
├── docker-compose.yml         # 로컬 개발 환경
└── README.md
```

## 🚀 핵심 기능

### Phase 1 (필수 - 4주)
- ✅ 선착순 쿠폰 발급 (Redis 분산 락)
- ✅ 1인당 발급 제한 (3개)
- ✅ 실시간 발급 성공/실패 알림
- ✅ 회원가입/로그인 (JWT)
- ✅ 내 쿠폰함 조회

### Phase 2 (확장 - 여유있으면)
- 쿠폰 오픈 사전 알림
- 쿠폰 소진 알림
- 쿠폰 사용/취소
- 관리자 대시보드
- 이벤트 등록 관리

## 📖 문서

- [요구사항 명세서](docs/requirements.md)
- [시스템 아키텍처](docs/architecture.md)
- [API 명세서](docs/api-spec.md)

## 🔧 로컬 개발 환경 설정

### 사전 요구사항
- Docker & Docker Compose
- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Redis 7+

### 실행 방법

```bash
# 1. 레포지토리 클론
git clone https://github.com/gyrms/Flash-Coupon-Platform.git
cd Flash-Coupon-Platform

# 2. Docker Compose로 인프라 실행
docker-compose up -d

# 3. 쿠폰 서비스 실행 (Java)
cd coupon-service
./gradlew bootRun

# 4. 알림 서비스 실행 (Node.js)
cd notification-service
npm install
npm start

# 5. 프론트엔드 실행 (React)
cd frontend
npm install
npm start
```

## 📊 부하 테스트

```bash
# JMeter 부하 테스트 실행
cd load-test
./run-test.sh
```

## 🎯 학습 목표

이 프로젝트를 통해 다음을 경험할 수 있습니다:
- MSA 아키텍처 설계 및 구현
- Redis를 활용한 동시성 제어
- 메시지 큐를 통한 서비스 간 통신
- WebSocket 기반 실시간 알림
- 대용량 트래픽 처리 기법
- Docker를 활용한 컨테이너화
- 모니터링 및 로깅 시스템

## 📝 라이센스

MIT License

## 👤 Author

gyrms
