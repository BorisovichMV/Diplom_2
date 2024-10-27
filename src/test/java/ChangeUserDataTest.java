import entities.User;
import helpers.RandomStringGenerator;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static steps.UserSteps.*;

public class ChangeUserDataTest extends BaseTestWithUserCreating {

    @Test
    @DisplayName("Тест изменения почты пользователя")
    public void testChangeUserEmail() {
        String newEmail = RandomStringGenerator.generateEmail();
        user.setEmail(newEmail);
        Response response = changeUserData(this.user, true, 200);
        JsonPath jsonPath = response.jsonPath();

        checkUserAttributes(jsonPath);
        checkLogin();
    }

    @Test
    @DisplayName("Тест изменения имени пользователя")
    public void testChangeUserName() {
        String newName = RandomStringGenerator.generateUsername();
        user.setName(newName);
        Response response = changeUserData(this.user, true, 200);
        JsonPath jsonPath = response.jsonPath();

        checkUserAttributes(jsonPath);
    }

    @Test
    @DisplayName("Тест изменения пароля пользователя")
    public void testChangeUserPassword() {
        String newPassword = RandomStringGenerator.generatePassword();
        user.setPassword(newPassword);
        Response response = changeUserData(this.user, true, 200);
        JsonPath jsonPath = response.jsonPath();

        Assert.assertTrue(jsonPath.getBoolean("success"));
        checkUserAttributes(jsonPath);
        checkLogin();
    }

    @Test
    @DisplayName("Тест изменения почты на уже существующую у другого пользователя")
    public void testChangeUserEmailToExistingUser() {
        String email = RandomStringGenerator.generateEmail();
        String password = RandomStringGenerator.generatePassword();
        String username = RandomStringGenerator.generateUsername();
        User yetAnotherUser = new User(email, password, username);
        Response yetAnotherResponse = createUser(yetAnotherUser);
        rememberTokens(yetAnotherUser, yetAnotherResponse.jsonPath());
        this.createdUsers.add(yetAnotherUser);

        String newEmail = yetAnotherUser.getEmail();
        user.setEmail(newEmail);
        Response response = changeUserData(this.user, true, 403);
        JsonPath jsonPath = response.jsonPath();

        Assert.assertFalse(jsonPath.getBoolean("success"));
        Assert.assertEquals("User with such email already exists", jsonPath.getString("message"));
    }

    @Test
    @DisplayName("Тест запроса на изменение данных без авторизации")
    public void testChangeUserDataWithoutAuthorization() {
        Response response = changeUserData(this.user, false, 401);
        JsonPath jsonPath = response.jsonPath();

        Assert.assertFalse(jsonPath.getBoolean("success"));
        Assert.assertEquals("You should be authorised", jsonPath.getString("message"));
    }

    @Step("Проверяем возможность логина с новыми учетными данными")
    private void checkLogin() {
        Response response = loginUser(this.user);
        JsonPath jsonPath = response.jsonPath();
        rememberTokens(this.user, jsonPath);

        Assert.assertTrue(jsonPath.getBoolean("success"));
        checkUserAttributes(jsonPath);
    }

    @Step("Проверяем, что атрибуты пользователя совпадают с атрибутами созданного пользователя")
    private void checkUserAttributes(JsonPath jsonPath) {
        Map<String, String> returnedUser = jsonPath.getMap("user");
        Assert.assertEquals(user.getEmail().toLowerCase(), returnedUser.get("email"));
        Assert.assertEquals(user.getName(), returnedUser.get("name"));
    }
}
