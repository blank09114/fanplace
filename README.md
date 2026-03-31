# Fan Place

**Fan Place**는 **스포츠 팬덤**을 위한 **커뮤니티 플랫폼**입니다.
🔗 [배포 주소](https://fanplace.co.kr/)

---

## 📌 프로젝트 개요
- **개발 형태**: Spring Boot 기반 웹 애플리케이션(MVC 구조)
- **개발 인원**: 약 2주
- **핵심 목표**: 실제 운영 가능한 커뮤니티 플랫폼 만들기

---

## 🛠️ 기술 스택
| 분야 | 사용 기술 |
|------|-----------|
| Front-end | HTML, CSS, JavaScript, Thymeleaf |
| Back-end | Java, Spring Boot, Spring Data JPA, Spring Security, Lombok |
| Database | MySQL |
| Infra | AWS EC2, AWS RDS, AWS S3 |
| DevOps/CI·CD | Docker, GitHub Actions |
| Dev Tools | IntelliJ IDEA, VS Code, Git/GitHub |

---

## 🚀 주요 기능
### 게시판
- 게시글 작성/수정/삭제
- 카테고리/검색/인기글
- 조회수/좋아요/댓글 수
### 댓글
- 댓글/대댓글 구조
- 삭제 상태 관리(Soft Delete)
- 권한 기반 UI 처리
### 회원
- 회원가입(이메일 인증)
- 로그인/로그아웃
- 비밀번호 변경/계정 복구
- 회원 탈퇴
### 알림
- 댓글/대댓글 알림
- 읽음 처리/미읽음 개수
- WebSocket 기반 실시간 알림
### 관리자
- 사용자 제재(차단)
- 삭제된 게시글/댓글 관리
- 활동 통계 대시보드
- 이상 행동 탐지
### 기타
- 로그인 기록
- 사용자 활동 리포트
- 이미지 업로드 (S3)

---

## ⚙️ 기술적 구현
### SSR+Rest API 하이브리드 구조
- Thymeleaf 기반 SSR로 초기 페이지 렌더링
- 댓글, 좋아요, 알림 등은 API로 분리
- 페이지 단위 JS 모듈 초기화 구조 적용
### 계정 인증/토큰 시스템
- 이메일 인증/비밀번호 재설정/탈퇴를 토큰 기반으로 처리
- 토큰은 해시(SHA-256)로 저장
- 만료/사용 여부 관리
### 커뮤니티 데이터 구조 설계
- Post/Comment/Recomment 계층 분리
- Soft Delete+주기적 purge 구조
- 댓글/대댓글 권한 및 상태 관리
### 운영 기능 구현
- 관리자 대시보드(활동 추이/게시판 상태/유저 이상 행동)
- 사용자 제재 시스템(차단 로그)
- 로그인 기록 추적
### 사용자 경험(UX) 개선
- 페이지 이동 없는 댓글/좋아요 처리
- 토스트/모달/알림 시스템 구현
- 다크모드 지원
### 인프라 및 배포
- S3 기반 이미지 업로드
- Docker 이미지 빌드 및 GHCR 배포
- GitHub Actions+AWS SSM을 통한 자동 배포

---

## 📁 디렉터리 구조
```
fanplace/
├── .github/workflows # CI/CD 파이프라인
├── src/main/
│   ├── kr/co/fanplace
│   │   ├── api/ # API Controller
│   │   ├── controller/ # Thymeleaf View 렌더링용 MVC Controller
│   │   ├── service/ # 비즈니스 로직
│   │   ├── repository/ # JPA Interface
│   │   ├── entity/ # JPA Entity
│   │   ├── dto/ # 계층 간 데이터 전달용 DTO
│   │   ├── setting/ # JPA, Security, S3, Web, GeoIP, 로그인 기록 관련 설정
│   │   └── FanplaceApplication.java # 실행 파일 
│   └── resources/
│       ├── templates/ # Thymeleaf 템플릿
│       └── static/ # 정적 리소스
└── build.gradle
