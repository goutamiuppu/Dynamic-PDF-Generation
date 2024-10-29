package org.assignment.domainmodel;

import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
public class Document implements Serializable {
    private String seller;
    private String sellerGstin;
    private String sellerAddress;
    private String buyer;
    private String buyerGstin;
    private String buyerAddress;
    private List<Item> items;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Document[")
                .append("seller=").append(seller != null ? seller : "")
                .append(",sellerGstin=").append(sellerGstin != null ? sellerGstin : "")
                .append(",sellerAddress=").append(sellerAddress != null ? sellerAddress : "")
                .append(",buyer=").append(buyer != null ? buyer : "")
                .append(",buyerGstin=").append(buyerGstin != null ? buyerGstin : "")
                .append(",buyerAddress=").append(buyerAddress != null ? buyerAddress : "")
                .append(",items=").append(formatItems())
                .append("]");
        return sb.toString();
    }

    private String formatItems() {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        return items.stream()
                .map(Item::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(seller, document.seller) &&
                Objects.equals(sellerGstin, document.sellerGstin) &&
                Objects.equals(sellerAddress, document.sellerAddress) &&
                Objects.equals(buyer, document.buyer) &&
                Objects.equals(buyerGstin, document.buyerGstin) &&
                Objects.equals(buyerAddress, document.buyerAddress) &&
                Objects.equals(items, document.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seller, sellerGstin, sellerAddress,
                buyer, buyerGstin, buyerAddress, items);
    }
}