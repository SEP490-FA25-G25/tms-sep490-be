package org.fyp.tmssep490be.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.security.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT token from Authorization header
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                try {
                    if (jwtTokenProvider.validateAccessToken(token)) {
                        Long userId = jwtTokenProvider.getUserIdFromJwt(token);
                        String email = jwtTokenProvider.getEmailFromJwt(token);
                        
                        // Extract roles from token
                        String rolesStr = jwtTokenProvider.getTokenType(token); // This is a simplified version
                        List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesStr.split(","))
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .collect(Collectors.toList());
                        
                        // Create authentication object
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(userId.toString(), null, authorities);
                        
                        // Set authentication in accessor for user-specific messaging
                        accessor.setUser(authentication);
                        
                        // Set in security context
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        log.info("WebSocket authenticated for user ID: {}, email: {}", userId, email);
                    } else {
                        // Token invalid/expired - allow anonymous connection
                        log.warn("Invalid or expired JWT token in WebSocket connection - allowing anonymous access");
                    }
                } catch (Exception e) {
                    // Authentication failed - allow anonymous connection but log error
                    log.warn("Error authenticating WebSocket connection: {} - allowing anonymous access", e.getMessage());
                }
            } else {
                // No token provided - allow anonymous connection
                log.debug("No JWT token found in WebSocket connection - anonymous access");
            }
        }
        
        return message;
    }
}
