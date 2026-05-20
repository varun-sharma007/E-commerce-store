package com.ecom.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.model.Category;
import com.ecom.model.Product;
import com.ecom.repository.CategoryRepository;
import com.ecom.repository.ProductRepository;

/**
 * Smart rule-based Shopping Assistant.
 * Parses natural-language queries and executes real DB searches.
 */
@Service
public class ChatService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // ── Known category names (lowercase for matching) ─────────────────────────
    private static final String[] CATEGORY_KEYWORDS = {
        "mobile", "mobiles", "phone", "phones", "smartphone",
        "laptop", "laptops", "notebook",
        "tv", "television", "televisions",
        "shoes", "shoe", "footwear", "sneakers", "sandals",
        "clothing", "clothes", "shirt", "shirts", "jeans", "dress",
        "books", "book",
        "electronics", "electronic", "earbuds", "speaker", "headphone",
        "furniture", "bed", "mattress", "sofa"
    };

    private static final java.util.Map<String, String> KEYWORD_TO_CATEGORY = new java.util.HashMap<>();
    static {
        for (String[] pair : new String[][] {
            {"mobile","Mobile"},{"mobiles","Mobile"},{"phone","Mobile"},
            {"phones","Mobile"},{"smartphone","Mobile"},
            {"laptop","Laptop"},{"laptops","Laptop"},{"notebook","Laptop"},
            {"tv","TV"},{"television","TV"},{"televisions","TV"},
            {"shoes","Shoes"},{"shoe","Shoes"},{"footwear","Shoes"},
            {"sneakers","Shoes"},{"sandals","Shoes"},
            {"clothing","Clothing"},{"clothes","Clothing"},{"shirt","Clothing"},
            {"shirts","Clothing"},{"jeans","Clothing"},{"dress","Clothing"},
            {"books","Books"},{"book","Books"},
            {"electronics","Electronics"},{"electronic","Electronics"},
            {"earbuds","Electronics"},{"speaker","Electronics"},{"headphone","Electronics"},
            {"furniture","Furniture"},{"bed","Furniture"},{"mattress","Furniture"},{"sofa","Furniture"}
        }) {
            KEYWORD_TO_CATEGORY.put(pair[0], pair[1]);
        }
    }

    /**
     * Main entry point: parse user message and return a ChatResponse
     */
    public ChatResponse processMessage(String message) {
        String msg = message.toLowerCase().trim();

        // ── Greeting ──────────────────────────────────────────────────────────
        if (isGreeting(msg)) {
            return greeting();
        }

        // ── Help / capability question ─────────────────────────────────────────
        if (msg.contains("help") || msg.contains("what can you") || msg.contains("how do")) {
            return help();
        }

        // ── In-stock query ─────────────────────────────────────────────────────
        boolean wantsInStock = msg.contains("in stock") || msg.contains("available")
                || msg.contains("stock") || msg.contains("buy");

        // ── Extract price ceiling ──────────────────────────────────────────────
        Double maxPrice = extractPrice(msg);

        // ── Detect category ────────────────────────────────────────────────────
        String category = detectCategory(msg);

        // ── Search / cheapest / best ───────────────────────────────────────────
        boolean isCheapest = msg.contains("cheapest") || msg.contains("lowest price")
                || msg.contains("budget") || msg.contains("affordable") || msg.contains("cheap");
        boolean isBest = msg.contains("best") || msg.contains("top") || msg.contains("recommend");

        // ── Keyword title search ───────────────────────────────────────────────
        String titleSearch = extractTitleSearch(msg);

        // ── Build response ────────────────────────────────────────────────────
        List<Product> results = new ArrayList<>();

        if (maxPrice != null && category != null && wantsInStock) {
            results = productRepository.findInStockByCategoryUnderPrice(category, maxPrice);
        } else if (maxPrice != null && category != null) {
            results = productRepository.findByIsActiveTrueAndCategoryIgnoreCaseAndDiscountPriceLessThanEqual(category, maxPrice);
        } else if (maxPrice != null && wantsInStock) {
            results = productRepository.findInStockUnderPrice(maxPrice);
        } else if (maxPrice != null) {
            results = productRepository.findByIsActiveTrueAndDiscountPriceLessThanEqual(maxPrice);
        } else if (category != null) {
            results = productRepository.findByIsActiveTrueAndCategoryIgnoreCase(category);
        } else if (titleSearch != null && !titleSearch.isBlank()) {
            results = productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(titleSearch, titleSearch);
            results = results.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList();
        } else if (isCheapest) {
            results = productRepository.findByIsActiveTrueAndStockGreaterThan();
            if (results.size() > 6) results = results.subList(0, 6);
        } else if (isBest) {
            results = productRepository.findByIsActiveTrue();
            if (results.size() > 6) results = results.subList(0, 6);
        } else {
            // Generic — show all active
            results = productRepository.findByIsActiveTrue();
            if (results.size() > 8) results = results.subList(0, 8);
        }

        return buildProductResponse(results, msg, category, maxPrice);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isGreeting(String msg) {
        return msg.matches("(hi|hello|hey|hiya|howdy|namaste|good morning|good evening|good afternoon)(\\s.*)?");
    }

    private ChatResponse greeting() {
        return new ChatResponse(
            "👋 Hi! I'm your **Ecom Store Shopping Assistant**.\n\n" +
            "I can help you find products. Try asking:\n" +
            "• _\"Show me mobiles under ₹10000\"_\n" +
            "• _\"Best laptops in stock\"_\n" +
            "• _\"Cheapest shoes available\"_\n" +
            "• _\"Do you have running shoes?\"_",
            List.of()
        );
    }

    private ChatResponse help() {
        List<Category> cats = categoryRepository.findByIsActiveTrue();
        StringBuilder sb = new StringBuilder("Here's what I can do:\n\n");
        sb.append("🔍 **Search by category** — Mobile, Laptop, TV, Shoes, Clothing, Books, Electronics, Furniture\n");
        sb.append("💰 **Filter by price** — \"under ₹5000\", \"below 2000\"\n");
        sb.append("📦 **Check stock** — \"in stock\", \"available\"\n");
        sb.append("🏷️ **Find deals** — \"cheapest\", \"budget\", \"best\"\n\n");
        sb.append("**Active categories:** ");
        cats.forEach(c -> sb.append(c.getName()).append(", "));
        return new ChatResponse(sb.toString().replaceAll(", $", ""), List.of());
    }

    private Double extractPrice(String msg) {
        // Matches: "under 5000", "below ₹2000", "less than 10,000", "under rs 500"
        Pattern pattern = Pattern.compile(
            "(?:under|below|less than|upto|up to|within|max|maximum)\\s*(?:rs\\.?|₹|inr)?\\s*([0-9][0-9,\\.]*)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(msg);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replaceAll("[,]", ""));
            } catch (NumberFormatException ignored) {}
        }
        // Also match "₹2000" or "rs 2000" standalone
        Pattern p2 = Pattern.compile("(?:₹|rs\\.?)\\s*([0-9][0-9,\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(msg);
        if (m2.find()) {
            try {
                return Double.parseDouble(m2.group(1).replaceAll("[,]", ""));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String detectCategory(String msg) {
        for (String kw : CATEGORY_KEYWORDS) {
            // Use word boundary to avoid partial matches
            if (msg.matches(".*\\b" + kw + "\\b.*")) {
                return KEYWORD_TO_CATEGORY.get(kw);
            }
        }
        return null;
    }

    private String extractTitleSearch(String msg) {
        // Strip filler words and use remainder as search term
        return msg.replaceAll("(?i)(do you have|show me|find|search for|looking for|i want|i need|any|some)", "").trim();
    }

    private ChatResponse buildProductResponse(List<Product> products, String msg, String category, Double maxPrice) {
        if (products.isEmpty()) {
            String suggestion = (category != null)
                ? "No " + category + " products found" + (maxPrice != null ? " under ₹" + maxPrice.intValue() : "") + ". Try a different category or price range."
                : "No products found matching your query. Try: _\"Show me mobiles\"_, _\"Laptops under ₹50000\"_";
            return new ChatResponse(suggestion, List.of());
        }

        StringBuilder text = new StringBuilder();
        if (category != null && maxPrice != null) {
            text.append("Found **").append(products.size()).append("** ").append(category)
                .append(" products under ₹").append(String.format("%,.0f", maxPrice)).append(":\n\n");
        } else if (category != null) {
            text.append("Here are **").append(products.size()).append("** ").append(category).append(" products:\n\n");
        } else if (maxPrice != null) {
            text.append("Found **").append(products.size()).append("** products under ₹")
                .append(String.format("%,.0f", maxPrice)).append(":\n\n");
        } else {
            text.append("Here are some products for you:\n\n");
        }

        // Limit to 6 results in chat
        List<Product> display = products.size() > 6 ? products.subList(0, 6) : products;
        List<ChatResponse.ProductLink> links = new ArrayList<>();
        for (Product p : display) {
            String badge = p.getStock() > 0 ? "✅ In Stock" : "❌ Out of Stock";
            String discount = p.getDiscount() > 0 ? " (" + p.getDiscount() + "% off)" : "";
            text.append("• **").append(p.getTitle()).append("** — ₹")
                .append(String.format("%,.0f", p.getDiscountPrice()))
                .append(discount).append(" ").append(badge).append("\n");
            links.add(new ChatResponse.ProductLink(p.getId(), p.getTitle(), p.getDiscountPrice(), p.getStock() > 0));
        }

        if (products.size() > 6) {
            text.append("\n_...and ").append(products.size() - 6).append(" more. Click a product below to view._");
        }

        return new ChatResponse(text.toString(), links);
    }

    // ── Inner response DTO ────────────────────────────────────────────────────

    public static class ChatResponse {
        private final String message;
        private final List<ProductLink> products;

        public ChatResponse(String message, List<ProductLink> products) {
            this.message = message;
            this.products = products;
        }

        public String getMessage() { return message; }
        public List<ProductLink> getProducts() { return products; }

        public static class ProductLink {
            private final Integer id;
            private final String title;
            private final Double price;
            private final boolean inStock;

            public ProductLink(Integer id, String title, Double price, boolean inStock) {
                this.id = id; this.title = title; this.price = price; this.inStock = inStock;
            }

            public Integer getId() { return id; }
            public String getTitle() { return title; }
            public Double getPrice() { return price; }
            public boolean isInStock() { return inStock; }
        }
    }
}
