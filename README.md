# Koreano - B2C Coffee Platform

커피 소비자와 비즈니스를 연결하는 B2C 플랫폼 프로젝트입니다. 주문과 재고 관리를 중심으로 마이크로서비스 아키텍처를 구현하며, 다양한 기술을 활용해 성능과 안정성을 높였습니다.

### 프로젝트 개요

- **기간**: 2024.04.06 ~ 2024.06.01
- **인원**: 백엔드 4명
- **역할**: 주문/재고 MicroService 담당

### 주요 기여

- **SAGA 패턴 도입**: 분산 트랜잭션 관리 최적화
    - [Orchestration SAGA로 주문 트랜잭션 최적화 회고 [링크]](https://blog.naver.com/smileman___/223417654224)
- **Redis 분산 Lock 적용**: 트랜잭션 분리 및 동시성 제어
    - [Redis 분산 락으로 재고 동시성 해결 회고 [링크]](https://blog.naver.com/smileman___/223416744799)
- **Redis 캐싱 적용**: 실시간 재고 조회 성능 향상
    - [Redis로 실시간 재고 관리 구현 회고 [링크]](https://blog.naver.com/smileman___/223424047791)
- **Qodana, GitHub Actions CI/CD 적용**: CI/CD 및 코드 커버리지(80%) 검증 자동화
    - [Qodana 기반 CI/CD 워크플로우 [링크]](https://github.com/Team-Koreano/Koreano/actions/workflows/qodana.yml)
- **MySQL Index 사용**: 주문 목록 조회 성능 향상 (900ms → 120ms)

### 기술 환경

- **백엔드**: Spring Boot 3.2.4, Java 17, Multi-Module, Gradle
- **인프라**: AWS RDS (MySQL), EC2, S3, Redis, Kafka, OpenFeign

### 아키텍처
![koreano_Architecture](https://github.com/user-attachments/assets/3c055f46-a165-494c-8803-5df2c7e9f832)

### ERD
![order-erd](https://github.com/user-attachments/assets/adfc9a57-cc41-4902-99c1-96834745a305)

### 주문 시퀀스 다이어그램
![koreano_order_sequence_diagram](https://github.com/user-attachments/assets/b080e36d-b014-4c7e-8d09-edb3a4d5f175)
<br>


## 트러블 슈팅 1

**[Orchestration SAGA로 주문 트랜잭션 최적화](https://blog.naver.com/smileman___/223417654224) (결과 : 응답 시간 : 250ms → 100ms)**
### 주문 생명주기

- **생성**: 회원, 장바구니, 재고 확인 후 주문 생성 (OPEN)
- **승인**: 결제 성공 시 주문 승인 (ACCEPTED)
- **완료**: 재고 차감 성공 시 주문 완료 (CLOSED)
- **취소**: 재고 부족, 결제 실패, 사용자/판매자 요청 시

![주문도메인생명주기](https://github.com/user-attachments/assets/fb6e7db5-b5fb-43cc-8990-6f66c3e1a324)

### 시나리오

1. 클라이언트가 Order-Service에 주문 요청
2. User, Bucket, Product Service로 유효성 및 재고 확인
3. 주문 생성 → 결제 진행 → 재고 차감 → 완료 응답

### 문제 상황

- **트랜잭션 관리**: 독립 MS 간 @Transactional 사용 어려움
- **장애 전파**: 동기 통신으로 응답 지연 및 서비스 장애 확산
- **사용자 경험**: 재고 차감 실패 시 대기 시간 증가

![장애전파문제](https://github.com/user-attachments/assets/9fe03fe4-6136-47eb-a8fc-56ddd192bc38)

### 해결 방법

- **Orchestration SAGA 패턴**:
    - Order-Orchestrator가 이벤트를 관리하며 비동기 처리
    - 재고 차감 실패 시 주문 취소 및 결제 롤백 자동화
    - 유지보수성과 응답 속도 개선

**주문 완료 시나리오 SAGA Pattern**

![SAGA-Pattern](https://github.com/user-attachments/assets/f9de8b04-195a-43c7-8bbf-1c13c9c76b0e)

**주문 취소 시나리오 SAGA Pattern**

![SAGA-pattern-주문취소](https://github.com/user-attachments/assets/5de213be-c5b4-403a-b3ea-e4fa0765df41)


## 트러블 슈팅 2

**[Redis 분산 락으로 재고 동시성 및 데드락 해결](https://blog.naver.com/smileman___/223416744799)**
### 요구사항

- 주문 시 재고 차감 (Order → Product 연동)
- 재고 부족 시 판매 불가
- 문제 발생 시 재고 복구 (재고 부족, 결제 실패, 주문 취소)

### 인프라

- 각 마이크로서비스는 독립 DB 사용
- 전체 재고는 Product-Service RDB에서 관리
- 다중 인스턴스 환경 지원

### 문제 상황

- **동시성**: 여러 사용자가 동시에 주문 시 재고 데이터 충돌 발생
    
    ![동시성문제](https://github.com/user-attachments/assets/cce7a270-40db-45ca-9317-1e9109a4c774)

    
- **데드락**: 다중 상품 주문 시 Lock 충돌로 교착 상태 발생
    
    ![DeadLock문제](https://github.com/user-attachments/assets/82d288fe-fdd0-436b-bda8-36da1e0d90ca)


### 해결 방법

- **Redis(Redisson) 분산 락**: 동시성 제어 및 트랜잭션 직렬화로 재고 충돌 해결
- MySQL (비/낙관적 락 검토 후 제외)
    
    ![동시성문제해결](https://github.com/user-attachments/assets/ce507c06-4ae8-4fc3-9838-7e38b430dad6)

    
- **트랜잭션 분리**: 주문 단위를 세분화해 데드락 방지 및 프로모션 이벤트 지원
    
    ![DeadLock문제해결](https://github.com/user-attachments/assets/0b6958db-7c0b-4be6-bc53-0bf94ead7611)


## 트러블 슈팅 3
**[Redis로 실시간 재고 관리 구현](https://blog.naver.com/smileman___/223424047791) (결과 : 250ms → 50ms)**
### 기존 방식
- **재고 확인**: FeignClient로 MS 간 Internal API 호출
  
### 문제점
- **실시간성 부족**: 호출 시점 재고만 확인, 진행 중 주문 반영 안 됨
- **비효율성**: 변동 없는 상품도 API 호출로 네트워크 지연 발생

### 해결 방법
- **Redis Set 활용**:
    - In-memory DB로 실시간 재고 관리 (가용 재고 = 전체 재고 - 처리 중 재고)
    - (stock:productId { "total:productId": X, "inProcessing:productId": Y })
    - 주문 생성 시 처리 중 재고 증가, 결제 성공 시 전체 재고 감소
    - Stock-History RDS로 데이터 유실 방지 및 로그 기록
    
    ![realtime-stock-management-architechture](https://github.com/user-attachments/assets/8853b7a4-5e39-4663-b4ac-69e848f83313)

### 시나리오 과정
 - **재고 확인**:
    - 현재 재고 4개, 처리 중 3개 → 가용 재고 1개 확인
    - 주문 수량 2개 시 재고 부족 응답
    
       ![재고확인](https://github.com/user-attachments/assets/9880b8db-a229-453b-8cbb-aeeb55914559)

- **주문 생성**:
    - 유효성 검사(회원, 장바구니, 재고) 후 처리 중 재고 1 증가
    - Stock-History RDS에 로그 기록, Order-Orchestrator로 결제 이벤트 전송

      ![주문생성완료](https://github.com/user-attachments/assets/b1b6a733-92e0-492c-abd4-e3f239e60c51)

- **결제 처리**:
    - 성공: 처리 중 재고 1 감소, 전체 재고 1 감소, Product RDS 반영
    - 실패: 처리 중 재고 1 감소, 전체 재고 유지, Stock-History RDS에 로그 기록
  
    ![결제완료후](https://github.com/user-attachments/assets/972320b0-3156-4c33-a06a-20df3693f3ce)

### 결과
- 재고 확인 네트워크 지연 시간을 **평균 250ms에서 50ms로 약 80% 대폭 단축**
- 정확한 실시간 재고 반영

## 회고

### 데이터베이스 설계의 제약

이상적으로 각 마이크로서비스가 독립적인 DB를 가져야 했으나, 현실적으로 비용 문제로 인해 하나의 MySQL DB를 여러 MS가 공유하게 되었습니다. 이로 인해 데이터 분리와 확장성에서 한계가 있었고, 트래픽 증가 시 성능 병목 가능성을 간과했습니다. 초기 설계 시 인프라 비용과 아키텍처 간 균형을 더 고민했어야 했던 점이 아쉬웠습니다. 이후 이를 극복하기 위해 **스키마 분리와 권한 설정**을 도입했습니다. 예를 들어, OrderServiceDB, ProductServiceDB 같은 별도 스키마를 생성하고, 각 MS에 전용 DB 사용자 권한을 부여해 논리적 분리를 구현했습니다. 이를 통해 물리적 분리 없이도 데이터 격리성을 확보하고, 단일 RDS의 자원을 효율적으로 활용할 수 있었습니다.

### 오버 엔지니어링의 교훈

프로젝트 초기에 11개의 마이크로서비스로 과도하게 분리하여 설계하려는 시도를 했습니다. 그러나 백엔드 4명이라는 팀 규모와 2개월이라는 프로젝트 기간을 고려했을 때, 이는 오히려 불필요한 복잡성을 야기하고 초기 개발 속도를 늦추는 원인이 되었습니다. 이 경험을 통해 **요구사항과 가용 리소스에 맞는 '적정 수준의 아키텍처 설계'가 무엇보다 중요하다는 점을 절실히 깨달았습니다.**

이후, 학습한 내용을 바탕으로 과감하게 설계를 재고하여 **장바구니, 주문, 재고 도메인을 하나의 마이크로서비스로 통합**하는 결정을 내렸습니다. 핵심 도메인을 단일 서비스로 묶자 코드 관리 및 배포 프로세스가 간편해졌고, 팀 전체의 생산성이 눈에 띄게 향상되었습니다. 이 과정에서 **과도한 분리 대신 실용성을 우선시하는 접근 방식의 중요성**을 체득했습니다.

이번 프로젝트를 통해 무조건적인 MSA 분리가 아닌, **비즈니스 도메인의 응집도와 팀의 역량, 그리고 프로젝트의 규모를 종합적으로 고려한 설계 역량**을 기를 수 있었습니다.
