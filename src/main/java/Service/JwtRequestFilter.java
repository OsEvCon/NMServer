package Service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Master;
import model.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

//Проверка наличия и корректности токена
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private JwtUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Пропускаем OPTIONS-запросы (для CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Пропускаем публичные эндпоинты
        String requestURI = request.getRequestURI();
        if (requestURI.equals("/auth/login") || requestURI.equals("/auth/registerUser")
                || requestURI.equals("auth/checkUpdate") || requestURI.equals("auth/pingServer")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Для WebSocket-запросов (STOMP) проверяем заголовки вручную
        if (isWebSocketHandshake(request)) {
            String token = extractTokenFromStompHeader(request);
            if (token != null) {
                authenticateWebSocket(token, request);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT required");
                return;
            }
        }
        // Для обычных HTTP-запросов
        else {
            String jwt = extractTokenFromHeader(request);
            if (jwt != null) {
                authenticateHttp(jwt, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    // Проверяем, это WebSocket-рукопожатие?
    private boolean isWebSocketHandshake(HttpServletRequest request) {
        String upgradeHeader = request.getHeader("Upgrade");
        if (upgradeHeader != null && upgradeHeader.equalsIgnoreCase("websocket")){
            System.out.println("Есть WebSocket-рукопожатие");
        }
        return upgradeHeader != null && upgradeHeader.equalsIgnoreCase("websocket");
    }

    // Извлекаем JWT из заголовка STOMP (для WebSocket)
    private String extractTokenFromStompHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("Извлекаем JWT из заголовка STOMP (для WebSocket): " + authHeader.substring(7));
            return authHeader.substring(7);
        }
        return null;
    }

    // Аутентификация для WebSocket
    private void authenticateWebSocket(String token, HttpServletRequest request) {
        System.out.println("Аутентификация WebSocket запроса");
        String userEmail = jwtUtil.extractUserEmail(token);
        if (userEmail != null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                System.out.println("Токен WebSocket запроса принят");
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
    }

    // Извлекаем JWT из HTTP-заголовка
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // Аутентификация для HTTP
    private void authenticateHttp(String jwt, HttpServletRequest request) {
        String userEmail = jwtUtil.extractUserEmail(jwt);
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
    }
}
