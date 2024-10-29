package org.assignment.domainmodel;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
public class Item implements Serializable {
    private String name;
    private String quantity;
    private double rate;
    private double amount;

    @Override
    public String toString() {
        return String.format("Item[name=%s,quantity=%s,rate=%.2f,amount=%.2f]",
                name != null ? name : "",
                quantity != null ? quantity : "",
                rate,
                amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Double.compare(item.rate, rate) == 0 &&
                Double.compare(item.amount, amount) == 0 &&
                Objects.equals(name, item.name) &&
                Objects.equals(quantity, item.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, quantity, rate, amount);
    }
}