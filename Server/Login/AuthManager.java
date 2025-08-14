package Server;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 인증
 * @author ESH
 */
public class AuthManager {
	
    private final Map<String, String> userDatabase;
    
    /**
     * 생성자
     * 사용자 데이터 초기화
     * 향후 파일, DB에서 사용자 정보 읽어오도록 확장
     */
    public AuthManager() {
    	this.userDatabase = new HashMap<>();
    	
    	// 사용자 추가
    	// userDatabase.put("user1", "1234")
	}
    
    /**
     * 사용자 id, password를 통한 인증 절차
     * @param id 사용자 id
     * @param password 사용자 비밀번호
     * @return true 사용자 인증
     * 			false 사용자 인증 실패
     */
    public boolean authenticate(String id, String password) {
        return userDatabase.containsKey(id) && userDatabase.get(id).equals(password);
    }
}
