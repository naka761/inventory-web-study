package filter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * POSTリクエストのCSRFトークンを確認するFilter。
 */
@WebFilter("/*")
public class CsrfFilter implements Filter {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;

        /*
         * 必ずgetParameter()より前に実行する。
         */
        httpRequest.setCharacterEncoding("UTF-8");

        HttpSession session =
                httpRequest.getSession(true);

        String sessionToken =
                (String) session.getAttribute("csrfToken");

        if (sessionToken == null) {
            sessionToken = createToken();

            session.setAttribute(
                    "csrfToken",
                    sessionToken);
        }

        if ("POST".equalsIgnoreCase(
                httpRequest.getMethod())) {

            String requestToken =
                    httpRequest.getParameter("csrfToken");

            if (requestToken == null
                    || !sessionToken.equals(requestToken)) {

                httpResponse.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "不正なリクエストです。");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String createToken() {

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                     .withoutPadding()
                     .encodeToString(bytes);
    }
}