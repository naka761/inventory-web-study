package controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import model.User;
import dao.ProductDAO;
import model.Product;

/**
 * 商品登録画面の表示と登録処理を担当するServlet。
 */
@WebServlet("/products/create")
public class ProductCreateServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * 商品登録画面を表示する。
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher(
                "/WEB-INF/jsp/product-form.jsp")
               .forward(request, response);
    }

    /**
     * 入力された商品をDBへ登録する。
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String priceText = request.getParameter("price");
        String stockText = request.getParameter("stock");

        try {
            int price = Integer.parseInt(priceText);
            int stock = Integer.parseInt(stockText);

            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "商品名を入力してください。");
            }

            String trimmedName = name.trim();

            if (trimmedName.length() > 100) {
                throw new IllegalArgumentException(
                        "商品名は100文字以内で入力してください。");
            }

            if (price < 0) {
                throw new IllegalArgumentException(
                        "価格は0以上で入力してください。");
            }

            if (stock < 0) {
                throw new IllegalArgumentException(
                        "在庫数は0以上で入力してください。");
            }

            Product product = new Product(
                    0,
                    trimmedName,
                    price,
                    stock
            );

            HttpSession session =
                    request.getSession(false);

            User loginUser =
                    (User) session.getAttribute("loginUser");

            ProductDAO productDAO = new ProductDAO();

            productDAO.insertWithLog(
                    product,
                    loginUser.getId());

            response.sendRedirect(
                    request.getContextPath() + "/products");

        } catch (NumberFormatException e) {

            request.setAttribute(
                    "errorMessage",
                    "価格と在庫数は整数で入力してください。");

            request.getRequestDispatcher(
                    "/WEB-INF/jsp/product-form.jsp")
                   .forward(request, response);

        } catch (IllegalArgumentException e) {

            request.setAttribute(
                    "errorMessage",
                    e.getMessage());

            request.getRequestDispatcher(
                    "/WEB-INF/jsp/product-form.jsp")
                   .forward(request, response);

        } catch (SQLException e) {

            throw new ServletException(
                    "商品の登録に失敗しました。", e);
        }
    }
}
