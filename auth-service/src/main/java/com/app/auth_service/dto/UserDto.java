package com.app.auth_service.dto;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class UserDto {

    @Column(unique = true, nullable = false)
    private String mobile;

    @Column(unique = true, nullable = false)
    private String email;

}
