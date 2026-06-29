package controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.ProductDAO;
import model.Product;

/**
 * 商品編集画面の表示と更新処理を担当するServlet。
 */
@WebServlet("/products/edit")
public class ProductEditServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * 編集対象の商品を取得し、編集画面を表示する。
     */
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String idText = request.getParameter("id");

        try {
            int id = Integer.parseInt(idText);

            ProductDAO productDAO = new ProductDAO();
            Product product = productDAO.findById(id);

            if (product == null) {
                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "指定された商品は存在しません。");
                return;
            }

            setFormValues(
                    request,
                    String.valueOf(product.getId()),
                    product.getName(),
                    String.valueOf(product.getPrice()),
                    String.valueOf(product.getStock()));

            request.getRequestDispatcher(
                    "/WEB-INF/jsp/product-edit.jsp")
                   .forward(request, response);

        } catch (NumberFormatException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "商品IDが不正です。");

        } catch (SQLException e) {
            throw new ServletException(
                    "編集対象商品の取得に失敗しました。", e);
        }
    }

    /**
     * 編集画面から送られた内容で商品を更新する。
     */
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String idText = request.getParameter("id");
        String name = request.getParameter("name");
        String priceText = request.getParameter("price");
        String stockText = request.getParameter("stock");

        int id;

        try {
            id = Integer.parseInt(idText);

        } catch (NumberFormatException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "商品IDが不正です。");
            return;
        }

        setFormValues(
                request,
                idText,
                name,
                priceText,
                stockText);

        try {
            int price = Integer.parseInt(priceText);
            int stock = Integer.parseInt(stockText);

            String trimmedName =
                    name == null ? "" : name.trim();

            if (trimmedName.isEmpty()) {
                throw new IllegalArgumentException(
                        "商品名を入力してください。");
            }

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
                    id,
                    trimmedName,
                    price,
                    stock
            );

            ProductDAO productDAO = new ProductDAO();
            int updatedCount = productDAO.update(product);

            if (updatedCount == 0) {
                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "更新対象の商品は存在しません。");
                return;
            }

            response.sendRedirect(
                    request.getContextPath() + "/products");

        } catch (NumberFormatException e) {

            request.setAttribute(
                    "errorMessage",
                    "価格と在庫数は整数で入力してください。");

            request.getRequestDispatcher(
                    "/WEB-INF/jsp/product-edit.jsp")
                   .forward(request, response);

        } catch (IllegalArgumentException e) {

            request.setAttribute(
                    "errorMessage",
                    e.getMessage());

            request.getRequestDispatcher(
                    "/WEB-INF/jsp/product-edit.jsp")
                   .forward(request, response);

        } catch (SQLException e) {
            throw new ServletException(
                    "商品の更新に失敗しました。", e);
        }
    }

    /**
     * 編集画面へ再表示する値をrequest属性へ保存する。
     */
    private void setFormValues(
            HttpServletRequest request,
            String id,
            String name,
            String price,
            String stock) {

        request.setAttribute("formId", id);
        request.setAttribute("formName", name);
        request.setAttribute("formPrice", price);
        request.setAttribute("formStock", stock);
    }
}
