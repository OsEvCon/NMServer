package DTO.request;

import lombok.AllArgsConstructor;
import lombok.Getter;


import javax.validation.constraints.NotBlank;

@AllArgsConstructor
@Getter
public class RefreshRequest {
    @NotBlank(message = "RefreshToken обязателен")
    private String refreshToken;

}
