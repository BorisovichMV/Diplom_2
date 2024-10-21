import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LoginTest {
    private User user;
    private final List<User> createdUsers = new ArrayList<>();

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site/api";
    }

    @Before
    public void setup() {
        String email = RandomStringGenerator.generateEmail();
        String password = RandomStringGenerator.generatePassword();
        String username = RandomStringGenerator.generateUsername();
        this.user = new User(email, password, username);
        Response response = createUser();
        rememberTokens(response.jsonPath());
        this.createdUsers.add(user);
    }

    @After
    public void tearDown() {
        this.createdUsers.forEach(this::deleteUser);
    }

    @Test
    @DisplayName("Тест логина под существующим пользователем")
    public void loginTest() {
        Response response = loginUser();
        rememberTokens(response.jsonPath());
        checkSuccessfulLoginBody(response);
    }

    @Test
    @DisplayName("Тест логина под неверными учетными данными")
    public void loginTestWithWrongCredentials() {
        checkForgotPassword();
        checkForgotEmail();
    }

    @Step("Проверяем невозможность входа с неправильным email")
    private void checkForgotPassword() {
        Response response = loginUser(true, false, 401);
        checkUnauthorizedResponseBody(response);
    }

    @Step("Проверяем невозможность входа с неправильным паролем")
    private void checkForgotEmail() {
        Response response = loginUser(false, true, 401);
        checkUnauthorizedResponseBody(response);
    }

    @Step("Проверяем тело ответа на невозможность входа с неправильными учетными данными")
    private void checkUnauthorizedResponseBody(Response response) {
        JsonPath jsonPath = response.jsonPath();
        boolean isSuccessful = jsonPath.getBoolean("success");
        Assert.assertFalse(isSuccessful);
        String error = jsonPath.getString("message");
        Assert.assertEquals("email or password are incorrect", error);
    }

    @Step("Проверяем тело ответа на успешный вход")
    private void checkSuccessfulLoginBody(Response response) {
        JsonPath jsonPath = response.jsonPath();
        checkUserAttributes(jsonPath);
        boolean isSuccessful = jsonPath.getBoolean("success");
        Assert.assertTrue(isSuccessful);
    }

    @Step("Проверяем, что атрибуты пользователя совпадают с атрибутами созданного пользователя")
    private void checkUserAttributes(JsonPath jsonPath) {
        Map<String, String> returnedUser = jsonPath.getMap("user");
        Assert.assertEquals(user.getEmail().toLowerCase(), returnedUser.get("email"));
        Assert.assertEquals(user.getName(), returnedUser.get("name"));
    }

    @Step("Запоминаем токены")
    private void rememberTokens(JsonPath jsonPath) {
        boolean isSuccessful = jsonPath.getBoolean("success");
        Assert.assertTrue(isSuccessful);
        String accessToken = jsonPath.getString("accessToken");
        String refreshToken = jsonPath.getString("refreshToken");
        this.user.setAccessToken(accessToken);
        this.user.setRefreshToken(refreshToken);
    }


    @Step("Отправляем запрос на вход пользователя")
    private Response loginUser() {
        return loginUser(false, false, 200);
    }

    @Step("Отправляем запрос на вход пользователя")
    private Response loginUser(Boolean isForgotPassword, Boolean isForgotEmail, Integer statusCode) {
        UserLoginModel model = UserLoginModel.fromUser(user);
        if (isForgotPassword) {
            model.forgotPassword();
        }
        if (isForgotEmail) {
            model.forgotEmail();
        }
        return sendRequest("POST", model, "/auth/login", statusCode);
    }

    @Step("Отправляем запрос на создание пользователя")
    private Response createUser() {
        return sendRequest("POST", UserRegistrationModel.fromUser(user), "/auth/register", 200);
    }

    @Step("Отправляем запрос")
    private Response sendRequest(String method, Object obj, String uri, Integer statusCode) {
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .body(obj)
                .log().body()
                .when()
                .request(method, uri)
                .then()
                .log().body()
                .statusCode(statusCode)
                .extract().response();
    }

    @Step("Отправляем запрос с авторизацией")
    private Response sendAuthorizedRequest(String method, Object obj, String uri, Integer statusCode) {
        return sendAuthorizedRequest(this.user, method, obj, uri, statusCode);
    }

    @Step("Отправляем запрос с авторизацией")
    private Response sendAuthorizedRequest(User user, String method, Object obj, String uri, Integer statusCode) {
        if (method.equals("DELETE")) {
            return RestAssured.given()
                    .header("Authorization", user.getAccessToken())
                    .when()
                    .request(method, uri)
                    .then()
                    .log().body()
                    .statusCode(statusCode)
                    .extract().response();
        }
        return RestAssured.given()
                .header("Authorization", user.getAccessToken())
                .header("Content-Type", "application/json")
                .body(obj)
                .log().body()
                .when()
                .request(method, uri)
                .then()
                .log().body()
                .statusCode(statusCode)
                .extract().response();
    }

    @Step("Удаляем пользователя")
    private void deleteUser(User user) {
        sendAuthorizedRequest(user, "DELETE", user, "/auth/user", 202);
    }

}
