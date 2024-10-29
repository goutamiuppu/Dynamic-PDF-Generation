package org.assignment.mockdata;

import org.assignment.domainmodel.Document;
import org.assignment.domainmodel.Item;

import java.util.ArrayList;
import java.util.List;

public class MockDocumentData {
    public Document createSampleDocument() {
        Document document = new Document();
        document.setSeller("Test Seller");
        document.setSellerGstin("27AAAAA0000A1Z5");
        document.setSellerAddress("123 Seller Street");
        document.setBuyer("Test Buyer");
        document.setBuyerGstin("27BBBBB0000B1Z5");
        document.setBuyerAddress("456 Buyer Street");

        List<Item> items = new ArrayList<>();
        Item item1 = new Item();
        item1.setName("Test Item 1");
        item1.setQuantity("10");
        item1.setRate(100.00);
        item1.setAmount(1000.00);

        Item item2 = new Item();
        item2.setName("Test Item 2");
        item2.setQuantity("5");
        item2.setRate(200.00);
        item2.setAmount(1000.00);

        items.add(item1);
        items.add(item2);
        document.setItems(items);

        return document;
    }

    public Document createLargeDocument() {
        Document document = new Document();
        document.setSeller("Large Document Seller");
        document.setSellerGstin("27AAAAA0000A1Z5");
        document.setSellerAddress("123 Seller Street");
        document.setBuyer("Large Document Buyer");
        document.setBuyerGstin("27BBBBB0000B1Z5");
        document.setBuyerAddress("456 Buyer Street");

        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Item item = new Item();
            item.setName("Item " + i);
            item.setQuantity(String.valueOf(i + 1));
            item.setRate(100.00 * (i + 1));
            item.setAmount(100.00 * (i + 1) * (i + 1));
            items.add(item);
        }
        document.setItems(items);

        return document;
    }
}