public class UserRegistrationModel {
    private String name;
    private String password;
    private String email;

    public UserRegistrationModel(String username, String password, String email) {
        this.name = username;
        this.password = password;
        this.email = email;
    }

    public static UserRegistrationModel fromUser(User user) {
        return new UserRegistrationModel(user.getName(), user.getPassword(), user.getEmail());
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void eraseName() {
        this.name = null;
    }

    public void erasePassword() {
        this.password = null;
    }

    public void eraseEmail() {
        this.email = null;
    }
}
