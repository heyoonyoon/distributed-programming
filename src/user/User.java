package user;

public abstract class User {
    protected String userId;
    protected String name;
    protected String email;
    protected String phone;
    protected String password;

    public boolean login(String email, String password) {
        return false;
    }

    public void logout() {
    }

    public void updateContact(String email, String phone) {
    }
}
