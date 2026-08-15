package com.leisure.global.auth;

import com.leisure.global.exception.ErrorCode;
import com.leisure.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Writer;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.fail(String.valueOf(code), errorCode.getMessage());

        String json = objectMapper.writeValueAsString(body);

        Writer writer = response.getWriter();
        writer.write(json);
    }
}
