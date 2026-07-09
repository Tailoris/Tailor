package com.tailoris.common.filter;

import com.tailoris.common.metrics.EncodingMetrics;
import com.tailoris.common.util.EncodingUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@WebFilter(urlPatterns = "/*")
@Order(1)
@RequiredArgsConstructor
public class CharsetValidationFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_SIZE = 1024 * 1024;

    private final EncodingMetrics encodingMetrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestCharset = request.getCharacterEncoding();
        if (requestCharset == null || !requestCharset.equalsIgnoreCase("UTF-8")) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            log.debug("Request charset was null or not UTF-8, forced to UTF-8. Original: {}", requestCharset);
            encodingMetrics.incrementInvalidUtf8Requests();
        }

        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            CharsetValidationRequestWrapper wrapper = new CharsetValidationRequestWrapper(request);
            if (wrapper.hasInvalidCharacters()) {
                log.warn("Request body contains invalid UTF-8 characters for URI: {}", request.getRequestURI());
                encodingMetrics.incrementMojibakeDetected();
            } else {
                encodingMetrics.incrementEncodingValidationPassed();
            }
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            filterChain.doFilter(wrapper, response);
        } else {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            encodingMetrics.incrementEncodingValidationPassed();
            filterChain.doFilter(request, response);
        }
    }

    private static class CharsetValidationRequestWrapper extends HttpServletRequestWrapper {

        private byte[] body;
        private boolean invalidCharactersDetected = false;

        public CharsetValidationRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = readLimitedBody(request.getInputStream());
            validateBody();
        }

        private byte[] readLimitedBody(InputStream inputStream) throws IOException {
            byte[] buffer = new byte[MAX_BODY_SIZE + 1];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead <= 0) {
                return new byte[0];
            }
            if (bytesRead > MAX_BODY_SIZE) {
                return new byte[0];
            }
            byte[] result = new byte[bytesRead];
            System.arraycopy(buffer, 0, result, 0, bytesRead);
            return result;
        }

        private void validateBody() {
            if (body == null || body.length == 0) {
                return;
            }
            if (!EncodingUtils.isValidUtf8(body)) {
                this.invalidCharactersDetected = true;
                return;
            }
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            if (EncodingUtils.containsMojibake(bodyStr)) {
                this.invalidCharactersDetected = true;
            }
        }

        public boolean hasInvalidCharacters() {
            return invalidCharactersDetected;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new ServletInputStream() {
                private final ByteArrayInputStream bis = new ByteArrayInputStream(body);

                @Override
                public int read() throws IOException {
                    return bis.read();
                }

                @Override
                public boolean isFinished() {
                    return bis.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                }
            };
        }
    }
}