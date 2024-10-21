public class UserLoginModel {
    private String password;
    private String email;

    public UserLoginModel(String username, String password, String email) {
        this.password = password;
        this.email = email;
    }

    public static UserLoginModel fromUser(User user) {
        return new UserLoginModel(user.getName(), user.getPassword(), user.getEmail());
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public void forgotPassword() {
        String newPassword = RandomStringGenerator.generatePassword();
        while (newPassword.equals(password)) {
            newPassword = RandomStringGenerator.generatePassword();
        }
        this.password = newPassword;
    }

    public void forgotEmail() {
        String newEmail = RandomStringGenerator.generateEmail();
        while (newEmail.equals(email)) {
            newEmail = RandomStringGenerator.generateEmail();
        }
        this.email = newEmail;
    }
}