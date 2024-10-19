public class UserRegistrationModel {
    private final String name;
    private final String password;
    private final String email;

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
}
