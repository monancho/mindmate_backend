package com.mind_mate.home.util.jwt;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

@Component
public class ExceptionUtil {
	public ResponseEntity<?> checkValid(BindingResult bindingResult) { // 유효성 체크
		Map<String, String> errors = new HashMap<>();
		bindingResult.getFieldErrors().forEach(
				err -> {
					errors.put(err.getField(), err.getDefaultMessage());
		});
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
}
}
