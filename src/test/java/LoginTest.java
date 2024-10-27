import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static steps.UserSteps.loginUser;
import static steps.UserSteps.rememberTokens;

public class LoginTest extends BaseTestWithUserCreating {

    @Test
    @DisplayName("Тест логина под существующим пользователем")
    public void loginTest() {
        Response response = loginUser(this.user);
        rememberTokens(this.user, response.jsonPath());
        JsonPath jsonPath = response.jsonPath();
        boolean isSuccessful = jsonPath.getBoolean("success");
        Map<String, String> returnedUser = jsonPath.getMap("user");

        Assert.assertEquals(user.getEmail().toLowerCase(), returnedUser.get("email"));
        Assert.assertEquals(user.getName(), returnedUser.get("name"));
        Assert.assertTrue(isSuccessful);
    }
    
    @Test
    @DisplayName("Тест на невозможность входа с неправильным email")
    public void checkForgotPassword() {
        Response response = loginUser(this.user, true, false, 401);

        checkUnauthorizedResponseBody(response);
    }

    @Test
    @DisplayName("Тест на невозможность входа с неправильным паролем")
    public void checkForgotEmail() {
        Response response = loginUser(this.user, false, true, 401);

        checkUnauthorizedResponseBody(response);
    }

    @Step("Проверяем тело ответа на невозможность входа с неправильными учетными данными")
    private void checkUnauthorizedResponseBody(Response response) {
        JsonPath jsonPath = response.jsonPath();
        boolean isSuccessful = jsonPath.getBoolean("success");
        String error = jsonPath.getString("message");

        Assert.assertFalse(isSuccessful);
        Assert.assertEquals("email or password are incorrect", error);
    }
}
