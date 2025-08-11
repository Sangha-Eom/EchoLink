package Server;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 인증
 * @author ESH
 */
public class AuthManager {
    private static final Map<String, String> userDatabase = new HashMap<>();
    
    static {
        // 예시 사용자 추가
        userDatabase.put("user1", "pass123");
        userDatabase.put("test", "1234");
    }
    
    /**
     * 사용자 id, password를 통한 인증 절차
     * @param id 사용자 id
     * @param password 사용자 비밀번호
     * @return true 사용자 인증
     * 			false 사용자 인증 실패
     */
    public static boolean authenticate(String id, String password) {
        return userDatabase.containsKey(id) && userDatabase.get(id).equals(password);
    }
}
