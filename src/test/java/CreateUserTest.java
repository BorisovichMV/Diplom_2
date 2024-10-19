import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

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
        if (response.statusCode() == 200) {
            JsonPath jsonPath = response.jsonPath();
            String accessToken = jsonPath.getString("accessToken");
            String refreshToken = jsonPath.getString("refreshToken");
            this.user.setAccessToken(accessToken);
            this.user.setRefreshToken(refreshToken);
            this.createdUsers.add(user);
        }
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
