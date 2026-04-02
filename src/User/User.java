package User;

public abstract class User {
    protected String name;
    protected String password;

    public abstract String LogIn();
    public abstract String LogOut();
    public abstract String Register();
}
