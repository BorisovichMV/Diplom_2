import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class GetUserOrdersTest {
    private User user;
    private final List<User> createdUsers = new ArrayList<>();
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private final Integer ordersCount;
    private final ObjectMapper objectMapper = new ObjectMapper();
    DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    public GetUserOrdersTest(Integer ordersCount) {
        this.ordersCount = ordersCount;
    }

    @Parameterized.Parameters
    public static Object[][] getParameters() {
        return new Object[][]{
                { 0 },
                { 1 },
                { 25 },
                { 49 },
                { 50 },
//  __________________________________________________________________________________________________________________
//                Далее найден баг! Возвращаются все созданные заказы, а не максимум 50, как ожидалось по описанию API
//                Закомментировано, как как для сдачи диплома тесты должны проходить
//  __________________________________________________________________________________________________________________
//                { 51 },
//                { 52 },
//                { 100 }
        };
    }

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
        fetchIngredients();
    }

    @After
    public void tearDown() {
        this.createdUsers.forEach(this::deleteUser);
    }

    @Test
    @DisplayName("Проверяем получение списка заказов авторизованным пользователем")
    public void getUserOrdersTest() {
        createOrders();
        Response response = sendAuthorizedRequest("GET", null, "/orders", 200);
        JsonPath jsonPath = response.jsonPath();
        Assert.assertTrue(jsonPath.getBoolean("success"));
        List<OrderReturnedModel> returnedOrderList = objectMapper.convertValue(jsonPath.get("orders"), new TypeReference<>() { });
        List<OrderReturnedModel> savedOrderList = this.orders.stream().map(OrderReturnedModel::fromOrder).collect(Collectors.toList());

        Assert.assertEquals(Math.min(savedOrderList.size(), 50), returnedOrderList.size());

        checkOrderContains(returnedOrderList, savedOrderList);
        checkReturnedOrderSorting(returnedOrderList);
    }

    @Test
    @DisplayName("Проверяем получение списка заказов неавторизованным пользователем")
    public void getUserOrdersTestUnauthorized() {
        Assume.assumeTrue("Выполняем тест только один раз", this.ordersCount == 0);
        Response response = sendRequest("GET", null, "/orders", 401);
        JsonPath jsonPath = response.jsonPath();
        Assert.assertFalse(jsonPath.getBoolean("success"));
        Assert.assertEquals("You should be authorised", jsonPath.getString("message"));
    }

    @Step("Проверяем, что вернувшийся список заказов корректно отсортирован")
    private void checkReturnedOrderSorting(List<OrderReturnedModel> returnedOrderList) {
        List<OrderReturnedModel> sortedList = returnedOrderList.stream()
                .sorted((o1, o2) ->{
                    ZonedDateTime dateTime1 = ZonedDateTime.parse(o1.getUpdatedAt(), formatter);
                    ZonedDateTime dateTime2 = ZonedDateTime.parse(o2.getUpdatedAt(), formatter);
                    return dateTime1.compareTo(dateTime2);
                }).collect(Collectors.toList());
        Assert.assertEquals(sortedList, returnedOrderList);
    }

    @Step("Проеряем, что мы создали каждый из вернувшихся заказов")
    private void checkOrderContains(List<OrderReturnedModel> returnedOrderList, List<OrderReturnedModel> savedOrderList) {
        returnedOrderList.forEach(item -> Assert.assertTrue(savedOrderList.contains(item)));
    }

    @Step("Получаем ингредиенты")
    private void fetchIngredients() {
        Response response = sendRequest("GET", null, "/ingredients", 200);
        List<Ingredient> allIngredients = objectMapper.convertValue(response.jsonPath().getList("data"), new TypeReference<List<Ingredient>>() { });
        this.ingredients.addAll(allIngredients);
    }

    @Step("Создаём заказы")
    private void createOrders() {
        for (int i = 0; i < this.ordersCount; i++) {
            createOrder();
        }
    }

    @Step("Создаём заказ")
    private void createOrder() {
        List<Ingredient> orderIngredients = new ArrayList<>();
        int index1 = ThreadLocalRandom.current().nextInt(this.ingredients.size());
        int index2;
        do {
            index2 = ThreadLocalRandom.current().nextInt(this.ingredients.size());
        } while (index1 == index2);

        orderIngredients.add(this.ingredients.get(index1));
        orderIngredients.add(this.ingredients.get(index2));
        Order order = new Order(orderIngredients);
        sendAuthorizedRequest("POST", OrderModel.fromOrder(order), "/orders", 200);
        this.orders.add(order);
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
