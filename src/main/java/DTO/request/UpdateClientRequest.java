package DTO.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UpdateClientRequest {
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    private String name;

    @Pattern(regexp = "^\\+?[78]\\d{10}$", message = "Неверный формат телефона")
    private String phoneNumber;

    @Email
    private String email;

}
