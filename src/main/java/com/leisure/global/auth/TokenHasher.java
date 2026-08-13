package com.leisure.global.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 문제정의
 * access 토큰을 블랙리스트 Redis 키로 써야 하는데, 원본 토큰을 그대로 키로 두면
 * 너무 길고, 유출 시 그대로 유효한 인증수단이라 위험
 *
 * 요구사항
 * 원본을 복원할 수 없는 단방향 변환으로, 짧고 고정 길이(64자 hex)인 안전한 키를 생서
 *
 * 왜 final, static?
 * static: 상태 없는 순수 함수라 인스턴스가 필요 없음, new 없이 바로 호출
 * final + private 생성자: 상속, 인스턴스화를 막음
 */
public final class TokenHasher {

    private TokenHasher() {}

    public static String hash(String token) {

        try {
            // SHA-256 다이제스트 확보
            MessageDigest digester = MessageDigest.getInstance("SHA-256");

            // 토큰을 UTF-8 바이트로 변환 후 해싱 → 32byte 결과
            byte[] encodedHash = digester.digest(token.getBytes(StandardCharsets.UTF_8));

//            StringBuilder stringBuilder = new StringBuilder(encodedHash.length * 2);
//
//            for (byte b : encodedHash) {
//                String hexString = Integer.toHexString(0xff & b);
//
//                if (hexString.length() == 1) {
//                    stringBuilder.append('0');
//                }
//
//                stringBuilder.append(hexString);
//            }
//
//            return stringBuilder.toString();


            // 32byte를 사람이 읽는 hex 문자열(64자)로 인코딩
            // byte 1개 = hex 2자
            // HexFormat이 부호(0xff), 앞자리 0 패딩까지 대신 처리
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 JVM 표준이라 실제론 절대 안 남
            // 만약 난다면 환경 자체가 깨진 것 → 복구 불가 → unchecked 로 즉시 실패(fail-fast)
            throw new IllegalStateException(e);
        }
    }
}
