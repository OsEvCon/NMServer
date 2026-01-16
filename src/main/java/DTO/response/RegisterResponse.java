package DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor // или ручной конструктор
public class RegisterResponse {
    private final String secretKey;
    private final String message;
}
