import java.util.ArrayList;
import java.util.List;

public class Order {
    private final List<Ingredient> ingredients = new ArrayList<>();

    public Order(List<Ingredient> ingredients) {
        this.ingredients.addAll(ingredients);
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public Integer getPrice() {
        return ingredients.stream().mapToInt(Ingredient::getPrice).sum();
    }
}
