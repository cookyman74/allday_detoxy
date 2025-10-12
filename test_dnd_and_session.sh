#!/bin/bash
# DND 및 세션 데이터 저장 테스트 스크립트
# 사용법: ./test_dnd_and_session.sh

set -e

echo "=========================================="
echo "DND 및 세션 데이터 저장 테스트"
echo "=========================================="

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo "${YELLOW}[1/5] 앱 재시작 및 로그 초기화${NC}"
adb shell am force-stop com.allday.detoxy
adb logcat -c
adb shell am start -n com.allday.detoxy/.MainActivity
sleep 2

echo ""
echo "${YELLOW}[2/5] DND 권한 확인${NC}"
echo "DND 권한 상태:"
adb shell dumpsys notification | grep -A 5 "policy access" || echo "DND 정보를 가져올 수 없습니다"

echo ""
echo "${GREEN}✓ 이제 앱에서 타이머를 시작해주세요 (25분 권장)${NC}"
echo "  타이머 시작 후 Enter를 눌러주세요..."
read

echo ""
echo "${YELLOW}[3/5] DND 활성화 로그 확인${NC}"
adb logcat -d | grep -E "DndManager|enableDnd|disableDnd" | tail -20

echo ""
echo "${YELLOW}[4/5] 세션 데이터 저장 확인${NC}"
echo "데이터베이스 경로:"
adb shell "run-as com.allday.detoxy ls -la databases/"

echo ""
echo "세션 데이터 조회:"
adb shell "run-as com.allday.detoxy sqlite3 databases/detoxy_database 'SELECT * FROM focus_sessions ORDER BY startTime DESC LIMIT 5;'" || echo "세션 데이터가 없거나 조회할 수 없습니다"

echo ""
echo "${GREEN}✓ 타이머를 포기하거나 완료될 때까지 기다려주세요${NC}"
echo "  타이머 종료 후 Enter를 눌러주세요..."
read

echo ""
echo "${YELLOW}[5/5] DND 비활성화 및 세션 업데이트 확인${NC}"
adb logcat -d | grep -E "DndManager|disableDnd|endSession" | tail -20

echo ""
echo "업데이트된 세션 데이터:"
adb shell "run-as com.allday.detoxy sqlite3 databases/detoxy_database 'SELECT id, datetime(startTime/1000, \"unixepoch\", \"localtime\") as start, datetime(endTime/1000, \"unixepoch\", \"localtime\") as end, durationMinutes, success FROM focus_sessions ORDER BY startTime DESC LIMIT 5;'"

echo ""
echo "${GREEN}=========================================="
echo "테스트 완료!"
echo "==========================================${NC}"

