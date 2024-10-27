import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static steps.UserSteps.createUser;
import static steps.UserSteps.rememberTokens;

public class CreateUserTest extends BaseTest {

    @Test
    @DisplayName("Тест на создание уникального пользователя")
    public void testCreateUser() {
        Response response = createUser(this.user);
        JsonPath jsonPath = response.jsonPath();
        rememberTokens(this.user, jsonPath);
        this.createdUsers.add(user);
        Map<String, String> returnedUser = jsonPath.getMap("user");

        Assert.assertEquals(user.getEmail().toLowerCase(), returnedUser.get("email"));
        Assert.assertEquals(user.getName(), returnedUser.get("name"));
    }

    @Test
    @DisplayName("Тест на создание уже зарегистрированного пользователя")
    public void testCreateUserAlreadyRegistered() {
        Response response = createUser(this.user);
        JsonPath jsonPath = response.jsonPath();
        rememberTokens(this.user, jsonPath);
        this.createdUsers.add(user);

        Response yetAnotherResponse = createUser(this.user,true, false, false, false, 403);
        JsonPath yetAnotherJsonPath = yetAnotherResponse.jsonPath();
        boolean isSuccessful = yetAnotherJsonPath.getBoolean("success");
        Assert.assertFalse(isSuccessful);
        String errorMessage = yetAnotherJsonPath.getString("message");
        Assert.assertEquals("User already exists", errorMessage);
    }

    @Test
    @DisplayName("Тест невозможности создания пользователя с пустым именем")
    public void checkEmptyName() {
        Response response = createUser(this.user, false, true, false, false, 403);
        checkForbiddenResponseBody(response);
    }

    @Test
    @DisplayName("Тест невозможности создания пользователя с пустым email")
    public void checkEmptyEmail() {
        Response response = createUser(this.user, false, false, true, false, 403);
        checkForbiddenResponseBody(response);
    }

    @Test
    @DisplayName("Тест невозможности создания пользователя с пустым паролем")
    public void checkEmptyPassword() {
        Response response = createUser(this.user, false, false, false, true, 403);
        checkForbiddenResponseBody(response);
    }

    @Step("Проверяем содержание ответа на попытку созжания невалидного пользователя")
    private void checkForbiddenResponseBody(Response response) {
        JsonPath jsonPath = response.jsonPath();
        boolean isSuccessful = jsonPath.getBoolean("success");
        String errorMessage = jsonPath.getString("message");

        Assert.assertFalse(isSuccessful);
        Assert.assertEquals("Email, password and name are required fields", errorMessage);
    }
}
