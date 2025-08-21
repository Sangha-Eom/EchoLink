# EchoLink(Remote-Project)
Remote Desktop to Android for playing game

[Android 앱 클라이언트]
↕ Socket 통신
[Java 데스크톱 서버]

----
구현 할 기능
# Android

## 연결 중

- 조작
    - 3Finger Touch : 키보드 띄우기
    - 2Finger Touch : 우클릭
    - 1Finger Touch : 좌클릭
    - drag : 마우스 움직이기

- 메뉴
    - 키 배치
        - 버튼 생성
        - 버튼에 특정 키 지정
        - 버튼 크기 조절
        - 버튼 위치 조절
    - 프리셋
        - 기존에 저장한 ‘키 배치’ 불러오기
    - 기본 제공 UI
        - 조이스틱, 패드 등

---

## 연결 전

- 메뉴(조절)
    - 프레임 상한선
    - 해상도
    - 패킷 전송 속도(화질, 반응 속도)

- 연결
    - 로그인 인증
    - 기기 연결

---

---

# Desktop


서버(데스크탑) ↔ 클라이언트(모바일)

- 원격 연결
- 로그인 시스템
- 비트레이팅(화질, 이미지 전송, 프레임 제한 등)
- 소리 출력 넘기기


서버로 부터 받은 명령 시행

- 마우스 이동
- 키보드 입력
