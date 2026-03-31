package com.irerin.travelan.auth.dto;

import java.util.List;

import com.irerin.travelan.auth.validation.PasswordMatch;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@PasswordMatch
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    @Pattern(regexp = "^\\S+$", message = "비밀번호에 공백을 사용할 수 없습니다")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String passwordConfirm;

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다 (010으로 시작하는 11자리)")
    private String phone;

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다")
    @Pattern(regexp = "^\\S+$", message = "닉네임에 공백을 사용할 수 없습니다")
    private String nickname;

    @Size(max = 5, message = "관심 지역은 최대 5개까지 선택할 수 있습니다")
    private List<String> interestRegions;

}
