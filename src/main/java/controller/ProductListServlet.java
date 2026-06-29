package controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.ProductDAO;
import model.Product;

/**
 * 商品一覧画面を表示するServlet。
 */
@WebServlet("/products")
public class ProductListServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        ProductDAO productDAO = new ProductDAO();

        try {
            List<Product> products = productDAO.findAll();

            request.setAttribute("products", products);

            request.getRequestDispatcher(
                    "/WEB-INF/jsp/product-list.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "商品一覧の取得に失敗しました", e);
        }
    }
}