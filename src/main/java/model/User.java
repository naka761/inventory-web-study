package model;

/**
 * ログイン済みユーザーの情報を保持するクラス。
 */
public class User {

    private int id;
    private String loginId;
    private String displayName;

    public User() {
    }

    public User(int id, String loginId, String displayName) {
        this.id = id;
        this.loginId = loginId;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}