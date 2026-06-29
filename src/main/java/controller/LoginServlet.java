package controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.UserDAO;
import model.User;

/**
 * ログイン画面の表示とログイン処理を担当するServlet。
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * ログイン画面を表示する。
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        /*
         * すでにログイン済みなら、
         * ログイン画面を出さず商品一覧へ移動する。
         */
        HttpSession session = request.getSession(false);

        if (session != null
                && session.getAttribute("loginUser") != null) {

            response.sendRedirect(
                    request.getContextPath() + "/products");
            return;
        }

        request.getRequestDispatcher(
                "/WEB-INF/jsp/login.jsp")
               .forward(request, response);
    }

    /**
     * ログインIDとパスワードを受け取って認証する。
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String loginId = request.getParameter("loginId");
        String password = request.getParameter("password");

        if (loginId == null
                || loginId.trim().isEmpty()
                || password == null
                || password.isEmpty()) {

            request.setAttribute(
                    "errorMessage",
                    "ログインIDとパスワードを入力してください。");

            request.getRequestDispatcher(
                    "/WEB-INF/jsp/login.jsp")
                   .forward(request, response);
            return;
        }

        UserDAO userDAO = new UserDAO();

        try {
            User user =
                    userDAO.findByLoginIdAndPassword(
                            loginId.trim(),
                            password);

            if (user == null) {

                request.setAttribute(
                        "errorMessage",
                        "ログインIDまたはパスワードが違います。");

                request.getRequestDispatcher(
                        "/WEB-INF/jsp/login.jsp")
                       .forward(request, response);
                return;
            }

            /*
             * 古いセッションがあれば破棄する。
             * ログイン時に新しいセッションを作る。
             */
            HttpSession oldSession =
                    request.getSession(false);

            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession newSession =
                    request.getSession(true);

            newSession.setAttribute(
                    "loginUser",
                    user);

            /*
             * 30分間操作がなければセッションを終了する。
             */
            newSession.setMaxInactiveInterval(30 * 60);

            response.sendRedirect(
                    request.getContextPath() + "/products");

        } catch (SQLException e) {

            throw new ServletException(
                    "ログイン処理に失敗しました。",
                    e);
        }
    }
}