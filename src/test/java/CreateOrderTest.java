import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import entities.Ingredient;
import entities.Order;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import models.OrderModel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static steps.OrderSteps.chooseIngredients;
import static steps.RequestSteps.sendAuthorizedRequest;
import static steps.RequestSteps.sendRequest;

public class CreateOrderTest extends BaseTestWithUserCreating {
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        super.setup();
        chooseIngredients(this.ingredients);
    }

    @Test
    @DisplayName("Тест на создание заказа с авторизацией")
    public void testCreateOrder() {
        Order newOrder = new Order(this.ingredients);
        Response response = sendAuthorizedRequest(this.user, "POST", OrderModel.fromOrder(newOrder), "/orders", 200);

        JsonPath jsonPath = response.jsonPath();
        Assert.assertTrue(jsonPath.getBoolean("success"));
        Assert.assertTrue(jsonPath.get("name") instanceof String);
        Map<String, Object> order = jsonPath.getMap("order");
        Map<String, String> owner = objectMapper.convertValue(order.get("owner"), new TypeReference<>() { });
        List<Ingredient> orderIngredients = objectMapper.convertValue(order.get("ingredients"), new TypeReference<>() { });

        Assert.assertTrue(order.containsKey("number"));
        Assert.assertTrue(order.get("number") instanceof Integer);
        Assert.assertTrue(order.get("_id") instanceof String);
        Assert.assertTrue(order.get("status") instanceof String);
        Assert.assertTrue(order.containsKey("ingredients"));
        Assert.assertEquals(this.ingredients.size(), orderIngredients.size());
        Assert.assertTrue(orderIngredients.containsAll(this.ingredients));
        Assert.assertEquals(this.ingredients, orderIngredients);
        Assert.assertTrue(order.containsKey("owner"));
        Assert.assertEquals(owner.get("name"), this.user.getName());
        Assert.assertEquals(owner.get("email"), this.user.getEmail().toLowerCase());
        Assert.assertEquals(order.get("price"), newOrder.getPrice());
    }

    @Test
    @DisplayName("Тест на создание заказа с авторизацией без ингредиентов")
    public void testCreateOrderWithoutIngredients() {
        Order order = new Order(new ArrayList<>());
        Response response = sendAuthorizedRequest(this.user, "POST", OrderModel.fromOrder(order), "/orders", 400);
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
        Order newOrder = new Order(this.ingredients);
        Response response = sendRequest("POST", OrderModel.fromOrder(newOrder), "/orders", 200);
        JsonPath jsonPath = response.jsonPath();
        Map<String, Object> order = jsonPath.getMap("order");

        Assert.assertTrue(jsonPath.getBoolean("success"));
        Assert.assertTrue(jsonPath.get("name") instanceof String);
        Assert.assertTrue(order.containsKey("number"));
        Assert.assertTrue(order.get("number") instanceof Integer);
    }

    @Test
    @DisplayName("Тест на создание заказа с автризацией с неверным хешем ингредиентов")
    public void testCreateOrderWithInvalidIngredients() {
        sendAuthorizedRequest(this.user, "POST", OrderModel.badModel(this.ingredients, 2), "/orders", 500);
    }

    @Test
    @DisplayName("Тест на создание заказа без авторизации с неверным хешем ингредиентов")
    public void testCreateOrderWithInvalidIngredientsWithoutAuth() {
        sendRequest("POST", OrderModel.badModel(this.ingredients, 2), "/orders", 500);
    }
}
