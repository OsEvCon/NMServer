package DTO.request;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class UpdateClientRequest {
    @Size(min = 2, max = 100, message = "Имя должно быть от 2 до 100 символов")
    private String name;

    @Pattern(regexp = "^\\+?[78]\\d{10}$", message = "Неверный формат телефона")
    private String phoneNumber;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
