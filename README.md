# MindMate 감정일기

2025.10 - 2025.11    
총 4명 팀 프로젝트

- URL: https://mindmate.co.kr/
- Backend: https://github.com/monancho/mindmate_backend
- Frontend: https://github.com/monancho/mindmate_frontend

## 1. 프로젝트 개요
---
MindMate는 사용자의 일기 내용을 Gemini API로 분석해 감정을 분류하고,  
감정 기록을 시각화하여 스스로의 상태를 돌아볼 수 있도록 돕는 감정 기반 기록 서비스입니다.

## 2. 팀원 구성
---
| 김윤섭 | 김국연 | 이상명 | 한수정 |
| --- | --- | --- | --- |
| ![github](https://github.com/monancho.png) | ![github](https://github.com/Kukyeon.png) | ![github](https://github.com/LSM-1020.png) | ![github](https://github.com/ddujeong.png) |
| [@monancho](https://github.com/monancho) | [@Kukyeon](https://github.com/Kukyeon) | [@LSM-1020](https://github.com/LSM-1020) | [@ddujeong](https://github.com/ddujeong) |

## 3. 기술 환경
---
- Back-end
  - Java 17
  - Spring Boot
  - JPA

- Front-end
  - React

- Database
  - MySQL
  - Redis

- Infra
  - Docker
  - AWS
  - Nginx

## 4. 기술 채택 이유
---
- Redis  
  인증과 토큰 관리 영역은 장애 발생 시 서비스 전반에 영향을 줄 수 있어,  
  자료와 레퍼런스가 풍부한 기술을 선택했습니다.  
  Refresh Token을 TTL 기반으로 관리하기에도 적합하다고 판단했습니다.

- Nginx  
  도메인 연결과 HTTPS 구성 과정을 직접 이해하고 경험하기 위해 선택했습니다.  
  실제 운영 환경과 유사한 구조를 구성하는 것을 목표로 했습니다.

- Gmail SMTP / Gemini  
  무료 사용 범위 내에서 프로젝트 요구사항을 충족할 수 있었고,  
  설정과 운영 측면에서 안정적이라고 판단해 선택했습니다.

## 5. 주요 기능
---
- 감정 일기
  - 텍스트 기반 감정 기록
  - 이미지 첨부 및 날짜별 관리

- AI 감정 분석
  - 일기 내용을 기반으로 감정 분류
  - 하루 상태 요약 코멘트 제공

- 시각화 리포트
  - 주간 및 월간 감정 변화 그래프
  - 감정 통계 데이터 제공

- 커뮤니티
  - 게시글 및 댓글 기능
  - 공감 기반 상호작용

- MBTI 기반 심리 테스트

- 생일 기반 별자리 오늘의 운세

## 6. 역할 분담
---
- 김윤섭 (팀장)
  인증 및 회원 관리 파트와 서비스 배포를 담당했다.  
  토큰 기반 인증과 소셜 로그인 기능을 통해 사용자 편의성과 안정성을 높였고, 서비스가 안정적으로 배포되도록 구성했다.

- 김국연  
  커뮤니티 서비스 파트를 담당했다.  
  게시판 기능을 중심으로 사용자 간 상호작용이 자연스럽게 이루어지도록 구성했다.

- 이상명  
  다이어리 및 데이터 시각화 파트를 담당했다.  
  감정 기록 달력과 통계 그래프를 구현하고, 데이터를 시각적으로 확인할 수 있도록 구성했다.

- 한수정  
  AI 지능형 서비스 파트를 담당했다.  
  감정 분석과 심리 테스트 기능을 통해 서비스의 힐링 요소를 강화했다.

---
본 프로젝트는 중앙정보처리학원 Java 풀스택 개발자 교육 과정의 팀 프로젝트로 진행되었습니다.  
본 프로젝트는 팀 협업과 실제 서비스 운영을 고려해 진행한 경험을 정리한 기록입니다.
