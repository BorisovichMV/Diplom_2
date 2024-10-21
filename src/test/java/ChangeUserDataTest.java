import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChangeUserDataTest {
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
        this.createdUsers.add(this.user);
    }

    @After
    public void tearDown() {
        this.createdUsers.forEach(this::deleteUser);
    }

    @Test
    @DisplayName("Тест изменения почты пользователя")
    public void testChangeUserEmail() {
        String newEmail = RandomStringGenerator.generateEmail();
        user.setEmail(newEmail);
        Response response = changeUserData(true, 200);
        JsonPath jsonPath = response.jsonPath();
        checkUserAttributes(jsonPath);
        checkLogin();
    }

    @Test
    @DisplayName("Тест изменения имени пользователя")
    public void testChangeUserName() {
        String newName = RandomStringGenerator.generateUsername();
        user.setName(newName);
        Response response = changeUserData(true, 200);
        JsonPath jsonPath = response.jsonPath();
        checkUserAttributes(jsonPath);
    }

    @Test
    @DisplayName("Тест изменения пароля пользователя")
    public void testChangeUserPassword() {
        String newPassword = RandomStringGenerator.generatePassword();
        user.setPassword(newPassword);
        Response response = changeUserData(true, 200);
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
        Response response = changeUserData(true, 403);
        JsonPath jsonPath = response.jsonPath();
        Assert.assertFalse(jsonPath.getBoolean("success"));
        Assert.assertEquals("User with such email already exists", jsonPath.getString("message"));
    }

    @Test
    @DisplayName("Тест запроса на изменение данных без авторизации")
    public void testChangeUserDataWithoutAuthorization() {
        Response response = changeUserData(false, 401);
        JsonPath jsonPath = response.jsonPath();
        Assert.assertFalse(jsonPath.getBoolean("success"));
        Assert.assertEquals("You should be authorised", jsonPath.getString("message"));
    }

    @Step("Проверяем возможность логина с новыми учетными данными")
    private void checkLogin() {
        Response response = loginUser();
        JsonPath jsonPath = response.jsonPath();
        Assert.assertTrue(jsonPath.getBoolean("success"));
        rememberTokens(jsonPath);
        checkUserAttributes(jsonPath);
    }

    @Step("Проверяем, что атрибуты пользователя совпадают с атрибутами созданного пользователя")
    private void checkUserAttributes(JsonPath jsonPath) {
        Map<String, String> returnedUser = jsonPath.getMap("user");
        Assert.assertEquals(user.getEmail().toLowerCase(), returnedUser.get("email"));
        Assert.assertEquals(user.getName(), returnedUser.get("name"));
    }

    @Step("Изменяем данные")
    private Response changeUserData(Boolean isAuthorizedRequest, Integer statusCode) {
        if (isAuthorizedRequest) {
            return sendAuthorizedRequest("PATCH", UserRegistrationModel.fromUser(user), "/auth/user", statusCode);
        }
        return sendRequest("PATCH", UserRegistrationModel.fromUser(user), "/auth/user", statusCode);
    }

    @Step("Отправляем запрос на вход пользователя")
    private Response loginUser() {
        return sendRequest("POST", UserLoginModel.fromUser(user), "/auth/login", 200);
    }

    @Step("Запоминаем токены")
    private void rememberTokens(JsonPath jsonPath) {
        rememberTokens(this.user, jsonPath);
    }

    @Step("Запоминаем токены дополнительного")
    private void rememberTokens(User user, JsonPath jsonPath) {
        boolean isSuccessful = jsonPath.getBoolean("success");
        Assert.assertTrue(isSuccessful);
        String accessToken = jsonPath.getString("accessToken");
        String refreshToken = jsonPath.getString("refreshToken");
        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
    }

    @Step("Отправляем запрос на создание пользователя")
    private Response createUser() {
        return createUser(this.user);
    }

    @Step("Отправляем запрос на создание дополнительного пользователя")
    private Response createUser(User user) {
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
