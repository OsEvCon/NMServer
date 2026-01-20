package DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClientRequest {
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    private String name;

    @Pattern(regexp = "^\\+?[78]\\d{10}$", message = "Неверный формат телефона")
    private String phoneNumber;

    @Email
    private String email;

}
