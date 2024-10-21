import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateUserTest {

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
    }

    @After
    public void tearDown() {
        this.createdUsers.forEach(this::deleteUser);
    }

    @Test
    @DisplayName("Тест на создание уникального пользователя")
    public void testCreateUser() {
        Response response = createUser();
        JsonPath jsonPath = response.jsonPath();
        rememberTokens(jsonPath);
        checkUserAttributes(jsonPath);

        this.createdUsers.add(user);
    }

    @Test
    @DisplayName("Тест на создание уже зарегистрированного пользователя")
    public void testCreateUserAlreadyRegistered() {
        Response response = createUser();
        JsonPath jsonPath = response.jsonPath();
        rememberTokens(jsonPath);
        checkUserAttributes(jsonPath);
        this.createdUsers.add(user);

        Response yetAnotherResponse = createUser(true, false, false, false);
        JsonPath yetAnotherJsonPath = yetAnotherResponse.jsonPath();
        boolean isSuccessful = yetAnotherJsonPath.getBoolean("success");
        Assert.assertFalse(isSuccessful);
        String errorMessage = yetAnotherJsonPath.getString("message");
        Assert.assertEquals("User already exists", errorMessage);
    }

    @Test
    @DisplayName("Тест на создание пользователя с пустыми обязательными полями")
    public void testCreateUserEmptyFields() {
        checkEmptyName();
        checkEmptyEmail();
        checkEmptyPassword();
    }

    @Step("Проверка создания пользователя с пустым именем")
    private void checkEmptyName() {
        Response response = createUser(false, true, false, false);
        checkForbiddenResponseBody(response);
    }

    @Step("Проверка создания пользователя с пустым email")
    private void checkEmptyEmail() {
        Response response = createUser(false, false, true, false);
        checkForbiddenResponseBody(response);
    }

    @Step("Проверка создания пользователя с пустым паролем")
    private void checkEmptyPassword() {
        Response response = createUser(false, false, false, true);
        checkForbiddenResponseBody(response);
    }

    @Step("Проверяем содержание ответа на попытку созжания невалидного пользователя")
    private void checkForbiddenResponseBody(Response response) {
        JsonPath jsonPath = response.jsonPath();
        boolean isSuccessful = jsonPath.getBoolean("success");
        Assert.assertFalse(isSuccessful);
        String errorMessage = jsonPath.getString("message");
        Assert.assertEquals("Email, password and name are required fields", errorMessage);
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

    @Step("Отправляем запрос на создание пользователя")
    private Response createUser() {
        return sendRequest("POST", UserRegistrationModel.fromUser(user), "/auth/register", 200);
    }

    @Step("Отправляем невалидный запрос на создание пользователя")
    private Response createUser(Boolean userExisting, Boolean emptyName, Boolean emptyEmail, Boolean emptyPassword) {
        if (userExisting) {
            return sendRequest("POST", UserRegistrationModel.fromUser(user), "/auth/register", 403);
        }
        UserRegistrationModel model = UserRegistrationModel.fromUser(user);
        if (emptyName) {
            model.eraseName();
        }
        if (emptyEmail) {
            model.eraseEmail();
        }
        if (emptyPassword) {
            model.erasePassword();
        }
        return sendRequest("POST", model, "/auth/register", 403);
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
        sendAuthorizedRequest("DELETE", user, "/auth/user", 202);
    }

}
