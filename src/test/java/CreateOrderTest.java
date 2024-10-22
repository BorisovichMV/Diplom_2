import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class CreateOrderTest {
    private User user;
    private final List<User> createdUsers = new ArrayList<>();
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        chooseIngredients();
    }

    @After
    public void tearDown() {
        this.createdUsers.forEach(this::deleteUser);
    }

    @Test
    @DisplayName("Тест на создание заказа с авторизацией")
    public void testCreateOrder() {
        Order order = new Order(this.ingredients);
        Response response = sendAuthorizedRequest("POST", OrderModel.fromOrder(order), "/orders", 200);
        checkOrder(response, order);
    }

    @Test
    @DisplayName("Тест на создание заказа с авторизацией без ингредиентов")
    public void testCreateOrderWithoutIngredients() {
        Order order = new Order(new ArrayList<>());
        Response response = sendAuthorizedRequest("POST", OrderModel.fromOrder(order), "/orders", 400);
        JsonPath jsonPath = response.jsonPath();
        Assert.assertFalse(jsonPath.getBoolean("success"));
        Assert.assertEquals("Ingredient ids must be provided", jsonPath.getString("message"));
    }

    @Test
    @DisplayName("Тест на создание заказа без авторизации без ингредиентов")
    public void testCreateOrderWithoutIngredientsWithoutAuth() {
        Order order = new Order(new ArrayList<>());
        Response response = sendRequest("POST", OrderModel.fromOrder(order), "/orders", 400);
        JsonPath jsonPath = response.jsonPath();
        Assert.assertFalse(jsonPath.getBoolean("success"));
        Assert.assertEquals("Ingredient ids must be provided", jsonPath.getString("message"));
    }

    @Test
    @DisplayName("Тест на создание заказа без авторизации")
    public void testCreateOrderWithoutAuth() {
        Order order = new Order(this.ingredients);
        Response response = sendRequest("POST", OrderModel.fromOrder(order), "/orders", 200);
        checkUnauthorizedOrder(response);
    }

    @Test
    @DisplayName("Тест на создание заказа с автризацией с неверным хешем ингредиентов")
    public void testCreateOrderWithInvalidIngredients() {
        Response response = sendAuthorizedRequest("POST", OrderModel.badModel(this.ingredients, 2), "/orders", 500);
    }

    @Test
    @DisplayName("Тест на создание заказа без авторизации с неверным хешем ингредиентов")
    public void testCreateOrderWithInvalidIngredientsWithoutAuth() {
        Response response = sendRequest("POST", OrderModel.badModel(this.ingredients, 2), "/orders", 500);
    }

    @Step("Проверяем ответ на создание заказа без авторизации")
    private void checkUnauthorizedOrder(Response response) {
        JsonPath jsonPath = response.jsonPath();
        Assert.assertTrue(jsonPath.getBoolean("success"));
        Assert.assertTrue(jsonPath.get("name") instanceof String);
        Map<String, Object> order = jsonPath.getMap("order");
        Assert.assertTrue(order.containsKey("number"));
        Assert.assertTrue(order.get("number") instanceof Integer);
    }

    @Step("Проверяем ответ на создание заказа")
    private void checkOrder(Response response, Order orderObj) {
        JsonPath jsonPath = response.jsonPath();
        Assert.assertTrue(jsonPath.getBoolean("success"));
        Assert.assertTrue(jsonPath.get("name") instanceof String);
        Map<String, Object> order = jsonPath.getMap("order");
        checkOrderComposition(order, orderObj);
    }

    @Step("Проверяем состав созданного бургера")
    private void checkOrderComposition(Map<String, Object> order, Order orderObj) {
        Assert.assertTrue(order.containsKey("number"));
        Assert.assertTrue(order.get("number") instanceof Integer);
        Assert.assertTrue(order.get("_id") instanceof String);
        Assert.assertTrue(order.get("status") instanceof String);

        Assert.assertTrue(order.containsKey("ingredients"));
        List<Ingredient> orderIngredients = objectMapper.convertValue(order.get("ingredients"), new TypeReference<>() { });
        Assert.assertEquals(this.ingredients.size(), orderIngredients.size());
        Assert.assertTrue(orderIngredients.containsAll(this.ingredients));
        Assert.assertEquals(this.ingredients, orderIngredients);

        Assert.assertTrue(order.containsKey("owner"));
        checkOwner(objectMapper.convertValue(order.get("owner"), new TypeReference<>() { }));
        checkPrice(objectMapper.convertValue(order.get("price"), Integer.class), orderObj);
    }

    @Step("Проверяем цену заказа")
    private void checkPrice(Integer price, Order orderObj) {
        Assert.assertEquals(price, orderObj.getPrice());
    }

    @Step("Проверяем пользователя, сделавшего заказ")
    private void checkOwner(Map<String, String> owner) {
        Assert.assertEquals(owner.get("name"), this.user.getName());
        Assert.assertEquals(owner.get("email"), this.user.getEmail().toLowerCase());
    }

    @Step("Выбираем два разных ингредиента")
    private void chooseIngredients() {
        Response response = sendRequest("GET", null, "/ingredients", 200);
        List<Ingredient> allIngredients = objectMapper.convertValue(response.jsonPath().getList("data"), new TypeReference<List<Ingredient>>() { });

        int index1 = ThreadLocalRandom.current().nextInt(allIngredients.size());
        int index2;
        do {
            index2 = ThreadLocalRandom.current().nextInt(allIngredients.size());
        } while (index1 == index2);

        this.ingredients.add(allIngredients.get(index1));
        this.ingredients.add(allIngredients.get(index2));
    }

    @Step("Отправляем запрос на создание пользователя")
    private Response createUser() {
        return createUser(this.user);
    }

    @Step("Отправляем запрос на создание пользователя")
    private Response createUser(User user) {
        return sendRequest("POST", UserRegistrationModel.fromUser(user), "/auth/register", 200);
    }

    @Step("Запоминаем токены")
    private void rememberTokens(JsonPath jsonPath) {
        rememberTokens(this.user, jsonPath);
    }

    @Step("Запоминаем токены")
    private void rememberTokens(User user, JsonPath jsonPath) {
        boolean isSuccessful = jsonPath.getBoolean("success");
        Assert.assertTrue(isSuccessful);
        String accessToken = jsonPath.getString("accessToken");
        String refreshToken = jsonPath.getString("refreshToken");
        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
    }

    @Step("Отправляем запрос")
    private Response sendRequest(String method, Object obj, String uri, Integer statusCode) {
        if (method.equals("DELETE") || method.equals("GET")) {
            return RestAssured.given()
                    .request(method, uri)
                    .then()
                    .log().body()
                    .statusCode(statusCode)
                    .extract().response();
        }
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
        if (method.equals("DELETE") || method.equals("GET")) {
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
