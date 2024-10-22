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
        String newPassword;
        do {
            newPassword = RandomStringGenerator.generatePassword();
        } while (newPassword.equals(password));
        this.password = newPassword;
    }

    public void forgotEmail() {
        String newEmail;
        do {
            newEmail = RandomStringGenerator.generateEmail();
        } while (newEmail.equals(email));
        this.email = newEmail;
    }
}
