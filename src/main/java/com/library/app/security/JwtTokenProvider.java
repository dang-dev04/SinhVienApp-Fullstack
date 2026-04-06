package com.library.app.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Khoá bí mật để ký JWT (Thay đổi an toàn tự động tuỳ môi trường thực tế)
    private static final String JWT_SECRET = "KhoaBaoMatCuaHeThongThuVienDACNTT_DaiHocThoSuy_HoacCongTy";
    
    // Thời gian sống của JWT (Ví dụ 24h = 86400000ms)
    private static final long JWT_EXPIRATION = 86400000L; 

    // Mã hoá khoá tự động qua chuẩn HS256
    private final Key key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    // 1. Phương thức khởi tạo Token (Cấp cho Frontend khi Login API thành công)
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .setSubject(username)       // Chứa tên đăng nhập (chủ thể Token)
                .setIssuedAt(now)           // Ngày khởi tạo
                .setExpiration(expiryDate)  // Hạn Token
                .signWith(key, SignatureAlgorithm.HS256) // Đóng dấu bảo mật
                .compact();
    }

    // 2. Phương thức lấy Username thông qua mã Token (Dùng lúc Client gọi request)
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    // 3. Phương thức kiểm tra Token xem có phải đồ thật & còn hạn không
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            System.err.println("Lỗi: Mã JWT không hợp lệ (Bị sửa đổi trái phép).");
        } catch (ExpiredJwtException ex) {
            System.err.println("Lỗi: Mã JWT đã hết hạn đăng nhập.");
        } catch (UnsupportedJwtException ex) {
            System.err.println("Lỗi: Mã JWT format không được hỗ trợ.");
        } catch (IllegalArgumentException ex) {
            System.err.println("Lỗi: Mã JWT bị trống.");
        }
        return false;
    }
}
