# Analytics 측정 계획 (v0.7)

**작성일**: 2025-10-29  
**버전**: v0.7 (2.5차 고도화)  
**기준 문서**: [2차 고도화 Analytics 스키마](./02_advanced_analytics_schema.md), [2.5차 고도화 작업 계획](./02.5_autosetting_todolist.md)

---

## 1. 개요

### 1.1 목적
- 자동 실행 기능(시간/위치 기반)의 사용률 및 효과성을 정량적으로 측정
- Firebase Analytics 대시보드를 통한 실시간 지표 추적
- 사용자 행동 패턴 분석 및 개선 방향 도출

### 1.2 측정 도구
- **Firebase Analytics Console**: 실시간 이벤트 모니터링
- **BigQuery Export**: 원시 데이터 분석 (선택 사항)
- **Custom SQL Queries**: 복잡한 지표 계산

---

## 2. 수집 완료된 이벤트 (v0.7 기준)

### 2.1 위치 기반 자동 실행 이벤트
#### 2.1.1 설정 이벤트
- ✅ `auto_run_location_created` - 위치 기반 자동 실행 생성
  - **로깅 위치**: `LocationBasedAutoRunViewModel.addLocation()`
  - **파라미터**: location_label_hash, radius_meters, duration_minutes, preset_type, trigger_type, dwell_time_minutes, requires_confirmation

- ✅ `auto_run_location_toggled` - 위치 기반 자동 실행 활성화/비활성화
  - **로깅 위치**: `LocationBasedAutoRunViewModel.toggleLocation()`
  - **파라미터**: is_enabled, total_enabled_count

- ✅ `auto_run_location_deleted` - 위치 기반 자동 실행 삭제
  - **로깅 위치**: `LocationBasedAutoRunViewModel.deleteLocation()`
  - **파라미터**: usage_count, success_rate, days_active
  - **참고**: 현재 usage_count, success_rate는 0 (3차 고도화에서 구현 예정)

#### 2.1.2 트리거 이벤트
- ✅ `auto_run_triggered` - 자동 실행 트리거 발생
  - **로깅 위치**: `GeofenceTransitionsReceiver.handleGeofenceTrigger()`
  - **파라미터**: trigger_type (LOCATION), source_id_hash, duration_minutes, preset_type

- ✅ `auto_run_notification_shown` - 자동 실행 알림 표시
  - **로깅 위치**: `GeofenceTransitionsReceiver.handleGeofenceTrigger()`
  - **파라미터**: trigger_type (LOCATION), is_pre_notification (false), minutes_before (null)
  - **참고**: 현재는 트리거 시점에 로깅 (Week 3 알림 구현 후 실제 알림 표시 시점으로 이동 예정)

- ⏳ `auto_run_started` - 자동 실행으로 타이머 시작
  - **로깅 예정 위치**: Week 3 작업 - 타이머 시작 로직 연동 후
  - **파라미터**: trigger_type, duration_minutes, is_auto_start, delay_seconds, gps_accuracy_meters, dwell_seconds

- ⏳ `auto_run_skipped` - 자동 실행 건너뜀
  - **로깅 예정 위치**: Week 3 작업 - 타이머 실행 중 체크 로직 추가 후
  - **파라미터**: trigger_type, reason

- ⏳ `auto_run_failed` - 자동 실행 실패
  - **로깅 예정 위치**: Week 3 작업 - GPS 정확도 낮음 처리 로직 추가 후
  - **파라미터**: trigger_type, failure_reason, gps_accuracy_meters

### 2.2 시간 기반 자동 실행 이벤트
- ✅ `auto_run_time_created` - 시간 기반 자동 실행 생성 (2차 고도화 완료)
- ✅ `auto_run_time_toggled` - 시간 기반 자동 실행 활성화/비활성화 (2차 고도화 완료)
- ✅ `auto_run_time_deleted` - 시간 기반 자동 실행 삭제 (2차 고도화 완료)
- ✅ `auto_run_triggered` - 시간 기반 트리거 (2차 고도화 완료)
- ✅ `auto_run_started` - 시간 기반 타이머 시작 (2차 고도화 완료)

---

## 3. Firebase Analytics 이벤트 확인 방법

### 3.1 실시간 이벤트 모니터링
**경로**: Firebase Console → Analytics → 이벤트

1. **최근 이벤트 확인**:
   - 지난 30분간 발생한 이벤트 실시간 확인
   - 이벤트명, 사용자 수, 이벤트 수 표시

2. **이벤트 상세 확인**:
   - 특정 이벤트 클릭 → 파라미터 분포 확인
   - 예: `auto_run_location_created` → `radius_meters` 분포

3. **Conversion 이벤트 설정**:
   - `auto_run_started` 를 Conversion 이벤트로 설정
   - 전환율 추적 가능

### 3.2 대시보드 생성
**경로**: Firebase Console → Analytics → 대시보드

**추천 대시보드 구성**:
1. **자동 실행 사용률 대시보드**:
   - 시간 기반 활성화 사용자 수
   - 위치 기반 활성화 사용자 수
   - 일별 자동 실행 트리거 수

2. **자동 실행 효과성 대시보드**:
   - 자동 실행으로 시작된 세션 수
   - 자동 실행 성공률
   - GPS 정확도 분포

3. **위치 기반 상세 대시보드**:
   - 반경별 사용 분포 (50m, 100m, 200m, 500m)
   - 체류 시간별 사용 분포 (0/1/3/5분)
   - 트리거 타입별 사용 분포 (ENTER, PERIODIC)

---

## 4. 성공률 계산 로직

### 4.1 자동 실행 성공률
**정의**: 자동 실행 트리거 후 실제로 타이머가 시작된 비율

**계산식**:
```
성공률 = (auto_run_started 이벤트 수) / (auto_run_triggered 이벤트 수) × 100
```

**Firebase Analytics SQL (BigQuery Export 활용 시)**:
```sql
WITH triggered AS (
  SELECT
    COUNT(*) AS trigger_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_triggered'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
),
started AS (
  SELECT
    COUNT(*) AS started_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_started'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
)
SELECT
  ROUND((started.started_count / triggered.trigger_count) * 100, 2) AS success_rate_percentage
FROM
  triggered, started;
```

### 4.2 위치 기반 자동 실행 성공률
**추가 필터**: `trigger_type = "LOCATION"`

**계산식**:
```
위치 기반 성공률 = (auto_run_started (LOCATION)) / (auto_run_triggered (LOCATION)) × 100
```

**Firebase Analytics SQL**:
```sql
WITH triggered AS (
  SELECT
    COUNT(*) AS trigger_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_triggered'
    AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'trigger_type') = 'LOCATION'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
),
started AS (
  SELECT
    COUNT(*) AS started_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_started'
    AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'trigger_type') = 'LOCATION'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
)
SELECT
  ROUND((started.started_count / triggered.trigger_count) * 100, 2) AS location_success_rate_percentage
FROM
  triggered, started;
```

### 4.3 GPS 정확도별 성공률
**목적**: GPS 정확도가 성공률에 미치는 영향 분석

**Firebase Analytics SQL**:
```sql
WITH gps_accuracy_bins AS (
  SELECT
    CASE
      WHEN CAST((SELECT value.double_value FROM UNNEST(event_params) WHERE key = 'gps_accuracy_meters') AS FLOAT64) < 20 THEN 'HIGH'
      WHEN CAST((SELECT value.double_value FROM UNNEST(event_params) WHERE key = 'gps_accuracy_meters') AS FLOAT64) < 50 THEN 'MEDIUM'
      ELSE 'LOW'
    END AS accuracy_level,
    COUNT(*) AS started_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_started'
    AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'trigger_type') = 'LOCATION'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
  GROUP BY
    accuracy_level
)
SELECT
  accuracy_level,
  started_count,
  ROUND((started_count / SUM(started_count) OVER ()) * 100, 2) AS percentage
FROM
  gps_accuracy_bins
ORDER BY
  started_count DESC;
```

---

## 5. 자동 실행 사용률 측정 쿼리

### 5.1 자동 실행 설정 사용자 비율 (≥ 40% 목표)
**정의**: DAU 대비 자동 실행 설정을 생성한 사용자 비율

**Firebase Analytics SQL**:
```sql
WITH daily_active_users AS (
  SELECT
    COUNT(DISTINCT user_pseudo_id) AS dau
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 1 DAY)
),
auto_run_users AS (
  SELECT
    COUNT(DISTINCT user_pseudo_id) AS auto_run_user_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name IN ('auto_run_time_created', 'auto_run_location_created')
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 1 DAY)
)
SELECT
  ROUND((auto_run_users.auto_run_user_count / daily_active_users.dau) * 100, 2) AS usage_rate_percentage
FROM
  daily_active_users, auto_run_users;
```

### 5.2 시간 기반 자동 실행 활성화 (≥ 30% 목표)
**정의**: DAU 대비 시간 기반 자동 실행을 활성화한 사용자 비율

**Firebase Analytics SQL**:
```sql
WITH daily_active_users AS (
  SELECT
    COUNT(DISTINCT user_pseudo_id) AS dau
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 1 DAY)
),
time_based_users AS (
  SELECT
    COUNT(DISTINCT user_pseudo_id) AS time_user_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_time_toggled'
    AND (SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'is_enabled') = 1
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
)
SELECT
  ROUND((time_based_users.time_user_count / daily_active_users.dau) * 100, 2) AS time_based_usage_percentage
FROM
  daily_active_users, time_based_users;
```

### 5.3 위치 기반 자동 실행 활성화 (≥ 20% 목표)
**정의**: 위치 권한 승인 사용자 대비 위치 기반 자동 실행을 활성화한 사용자 비율 (50% 목표)

**Firebase Analytics SQL**:
```sql
WITH location_permission_users AS (
  SELECT
    COUNT(DISTINCT user_pseudo_id) AS permission_user_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'permission_location_granted'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
),
location_based_users AS (
  SELECT
    COUNT(DISTINCT user_pseudo_id) AS location_user_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_location_toggled'
    AND (SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'is_enabled') = 1
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
)
SELECT
  ROUND((location_based_users.location_user_count / location_permission_users.permission_user_count) * 100, 2) AS location_based_usage_percentage
FROM
  location_permission_users, location_based_users;
```

---

## 6. 자동 실행 효과성 측정 쿼리

### 6.1 자동 실행 세션 비율 (≥ 35% 목표)
**정의**: 전체 세션 대비 자동 실행으로 시작된 세션 비율

**Firebase Analytics SQL**:
```sql
WITH total_sessions AS (
  SELECT
    COUNT(*) AS total_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'focus_session_start'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
),
auto_run_sessions AS (
  SELECT
    COUNT(*) AS auto_run_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_started'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
)
SELECT
  ROUND((auto_run_sessions.auto_run_count / total_sessions.total_count) * 100, 2) AS auto_run_session_percentage
FROM
  total_sessions, auto_run_sessions;
```

### 6.2 자동 실행 후 세션 완주율 (≥ 65% 목표)
**정의**: 자동 실행으로 시작된 세션 중 완료된 세션 비율

**Firebase Analytics SQL**:
```sql
WITH auto_run_started AS (
  SELECT
    COUNT(*) AS started_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_started'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
),
auto_run_completed AS (
  SELECT
    COUNT(*) AS completed_count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'focus_session_completed'
    AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'trigger_source') = 'auto_run'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
)
SELECT
  ROUND((auto_run_completed.completed_count / auto_run_started.started_count) * 100, 2) AS completion_rate_percentage
FROM
  auto_run_started, auto_run_completed;
```

---

## 7. 기술적 지표 측정

### 7.1 AlarmManager 정확도 (≥ 98% 목표)
**정의**: 예정 시각 ±2분 내 트리거된 비율

**측정 방법**:
- `auto_run_triggered` 이벤트의 `delay_seconds` 파라미터 분석
- Week 3 작업: `delay_seconds` 계산 로직 추가 필요

**Firebase Analytics SQL**:
```sql
WITH alarm_accuracy AS (
  SELECT
    CASE
      WHEN ABS(CAST((SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'delay_seconds') AS INT64)) <= 120 THEN 'ON_TIME'
      ELSE 'LATE'
    END AS accuracy,
    COUNT(*) AS count
  FROM
    `project.analytics_*.events_*`
  WHERE
    event_name = 'auto_run_triggered'
    AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'trigger_type') = 'TIME'
    AND event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
  GROUP BY
    accuracy
)
SELECT
  accuracy,
  count,
  ROUND((count / SUM(count) OVER ()) * 100, 2) AS percentage
FROM
  alarm_accuracy;
```

### 7.2 Geofence 트리거 정확도 (≥ 95% 목표)
**정의**: 위치 진입 5분 내 트리거된 비율

**측정 방법**:
- GPS 로그와 트리거 시간 비교 (서버 사이드 분석 필요)
- 현재는 사용자 피드백 기반 측정

---

## 8. 측정 스케줄

### 8.1 Week 2 말 (Day 14) - 초기 검증
- [ ] Firebase Analytics 이벤트 정상 수집 확인
- [ ] 실시간 이벤트 모니터링 대시보드 설정
- [ ] 위치 기반 이벤트 로깅 확인 (최소 10회 이상)

### 8.2 Week 4 초 (Day 22-24) - 중간 측정
- [ ] 자동 실행 사용률 측정 (40% 목표 대비)
- [ ] 자동 실행 성공률 측정 (80% 목표 대비)
- [ ] GPS 정확도 분포 분석

### 8.3 Week 4 말 (Day 28) - 최종 측정
- [ ] 모든 KPI 지표 최종 측정
- [ ] 성과 보고서 작성
- [ ] 개선 방향 도출

---

## 9. 참고 사항

### 9.1 BigQuery Export 활성화 (선택 사항)
**경로**: Firebase Console → 프로젝트 설정 → 통합 → BigQuery

**장점**:
- 원시 데이터 접근 가능
- 복잡한 SQL 쿼리 실행 가능
- 데이터 보존 기간 무제한

**단점**:
- BigQuery 사용료 발생 (소규모는 무료)
- 설정 후 24시간 후부터 데이터 수집

### 9.2 개인정보 보호
- ✅ 위치 좌표 수집 금지 (location_label_hash만 수집)
- ✅ 소스 ID 해시 처리 (source_id_hash)
- ✅ 사용자 식별 정보 제외

### 9.3 Firebase Analytics 제한 사항
- 이벤트당 최대 25개 파라미터
- 파라미터명 최대 40자
- 파라미터 값 최대 100자
- 일일 500개 unique 이벤트 (무료 플랜)

---

**문서 최종 업데이트**: 2025-10-29  
**작성자**: AI Assistant  
**버전**: 1.0 (Week 2, Day 14 완료)

