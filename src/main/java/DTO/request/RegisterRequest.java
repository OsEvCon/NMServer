package DTO.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Имя обязательно")
    @Size(min = 2, max = 50, message = "Имя должно содержать от 2 до 50 символов")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 10, max = 50, message = "Пароль должен содержать от 10 до 50 символов")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$",
            message = "Пароль должен содержать минимум одну строчную букву, одну заглавную букву и одну цифру." +
                    " Разрешены только буквы латинского алфавита, цифры и специальные символы"
    )
    private String password;

    @NotBlank(message = "Email обязателен")
    @Email
    private String email;
}
