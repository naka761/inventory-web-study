package filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 商品関連画面へのアクセス時に、
 * ログイン済みかどうかを確認するFilter。
 */
@WebFilter(urlPatterns = {
        "/products",
        "/products/*"
})
public class LoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig)
            throws ServletException {
        // Filter作成時の初期化処理。
        // 今回は特に何もしない。
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        /*
         * ServletRequestにはHTTP専用メソッドがないため、
         * HttpServletRequestへキャストする。
         */
        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;

        /*
         * falseを指定することで、
         * セッションがない場合に新規作成しない。
         */
        HttpSession session =
                httpRequest.getSession(false);

        boolean loggedIn =
                session != null
                && session.getAttribute("loginUser") != null;

        if (loggedIn) {

            /*
             * ログイン済みなら、
             * 次のFilterまたはServletへ処理を渡す。
             */
            chain.doFilter(request, response);

        } else {

            /*
             * 未ログインならServletへ通さず、
             * ログイン画面へリダイレクトする。
             */
            httpResponse.sendRedirect(
                    httpRequest.getContextPath() + "/login");
        }
    }

    @Override
    public void destroy() {
        // Filter破棄時の処理。
        // 今回は特に何もしない。
    }
}