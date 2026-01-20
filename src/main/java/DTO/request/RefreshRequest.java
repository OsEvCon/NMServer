package DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RefreshRequest {
    @NotBlank(message = "RefreshToken обязателен")
    private String refreshToken;

}
