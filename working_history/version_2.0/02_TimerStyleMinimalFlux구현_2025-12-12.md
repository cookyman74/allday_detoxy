# 작업 결과: Type B (Minimal Flux) 구현

## 1. 개요
뽀모도로 타이머의 두 번째 스타일인 **Type B: Minimal Flux (Zen Mode)**를 구현하였습니다.
이 스타일은 숫자와 그래프에 얽매이지 않고, 색(Color) 변화와 호흡(Breathing) 애니메이션을 통해 부드럽게 집중을 유도하는 '엠비언트(Ambient)' 모드입니다.

## 2. 구현 내용

### 2.1 `TimerStyleMinimalFlux.kt` 생성
*   **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/timer/components/TimerStyleMinimalFlux.kt`
*   **주요 기능**:
    *   **Breathing Animation**: `rememberInfiniteTransition`을 사용하여 중앙의 빛나는 구(Orb)가 마치 호흡하듯 커졌다 작아지는 스케일 애니메이션(`0.85f` ~ `1.05f`)을 적용했습니다.
    *   **Color Transition**: 타이머 진행률(`progress`)에 따라 색상이 동적으로 변화합니다.
        *   초기 (0~50%): **Blue (Calm)** → **Purple (Deep)**
        *   후반 (50~100%): **Purple (Deep)** → **Red/Magenta (Focus/Urgent)**
    *   **Toggleable Time Text**: 기본적으로 숫자를 숨기고, 탭(Click) 했을 때만 남은 시간이 크게 표시되도록 하여 시각적 방해를 최소화했습니다.

### 2.2 Pager 연동
*   **파일**: `app/src/main/java/com/allday/detoxy/presentation/ui/timer/TimerScreen.kt`
*   **변경**: `HorizontalPager`의 두 번째 페이지(`page == 1`)에 `TimerStyleMinimalFlux`를 배치했습니다.
    *   Page 0: Type A (Liquid Ring)
    *   Page 1: **Type B (Minimal Flux)** ✅
    *   Page 2: Type A (Placeholder for Type C)

## 3. 테스트 결과
*   **컴파일 테스트**: `./gradlew compileDebugKotlin` → **SUCCESS**
*   **기능 확인**:
    *   타이머 시작 후 좌측으로 스와이프하면 Zen Mode로 전환.
    *   구체가 호흡하듯 움직이며 색상이 변하는지 확인.
    *   화면 탭 시 남은 시간이 토글 되는지 확인.

## 4. 향후 계획
*   Step 3에서 세 번째 페이지에 `TimerStyleGlassSector` (Type C) 구현 예정.
