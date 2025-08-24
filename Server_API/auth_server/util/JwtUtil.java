-적용 후 기대 동작

브라우저(또는 WebView)로 http://localhost:8080/login → 구글 로그인

성공 시 응답 바디로
{ "token": "eyJhbGciOi...", "email": "you@example.com" } 를 받음

클라이언트(모바일/데스크탑)는 이 token을 저장

이후 보호 API 호출 시 Authorization: Bearer <token>


헤더로 전달 → JwtAuthenticationFilter가 검증/인증 컨텍스트 설정 → 컨트롤러 접근 성공
