package com.mind_mate.home.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequestDto {

//@NotBlank(message = "아이디를 입력해 주세여")
//@Size(min = 6, max = 20, message = "아이디는 6~ 20글자로 입력할 수 있습니다.")
//@Pattern(regexp = "^[a-zA-Z0-9]*$", message = "영어 알파벳과 숫자만 입력할 수 있습니다.")
//private String username;


@NotBlank(message = "이메일을 입력해 주세요.")
@Email(message = "이메일 형식이 올바르지 않습니다.")
@Size(max = 50, message = "이메일은 50자 이내로 입력해 주세요.")
private String email;

@NotBlank(message = "코드를 입력해 주세요")
private String code;

@NotBlank(message = "비밀번호를 입력해 주세여")
@Size(min = 8, max = 16, message = "비밀번호는 8 ~ 16글자로 입력할 수 있습니다.")
@Pattern(regexp = "^[a-zA-Z0-9]*$", message = "영어 알파벳과 숫자만 입력할 수 있습니다.")
private String password;

}
