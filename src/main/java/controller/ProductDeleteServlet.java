package controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.ProductDAO;

/**
 * 商品削除処理を担当するServlet。
 */
@WebServlet("/products/delete")
public class ProductDeleteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String idText = request.getParameter("id");

        try {
            int id = Integer.parseInt(idText);

            ProductDAO productDAO = new ProductDAO();
            int deletedCount = productDAO.deleteById(id);

            if (deletedCount == 0) {
                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "削除対象の商品は存在しません。");
                return;
            }

            response.sendRedirect(
                    request.getContextPath() + "/products");

        } catch (NumberFormatException e) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "商品IDが不正です。");

        } catch (SQLException e) {

            throw new ServletException(
                    "商品の削除に失敗しました。", e);
        }
    }
}