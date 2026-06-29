package model;

/**
 * 商品1件分の情報を保持するクラス。
 */
public class Product {

    private int id;
    private String name;
    private int price;
    private int stock;

    /**
     * JSPや各種フレームワークから利用するための
     * 引数なしコンストラクタ。
     */
    public Product() {
    }

    /**
     * 全項目を受け取るコンストラクタ。
     */
    public Product(int id, String name, int price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}