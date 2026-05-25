package com.ecom.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.model.Product;
import com.ecom.repository.ProductRepository;

/**
 * AI-powered Shopping Assistant using Retrieval-Augmented Generation (RAG).
 *
 * Flow:
 *  1. Extract intent (category, max price, keywords) from user message
 *  2. Query the database for matching products  ← real, accurate data
 *  3. Inject those products as context into the AI prompt
 *  4. Call Hugging Face API for a natural-language response
 *  5. Return AI text + clickable product links
 *
 * Falls back to a helpful rule-based response if the API is unavailable.
 */
@Service
public class ChatService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private GeminiService geminiService;

    // ── Category keyword map ───────────────────────────────────────────────────
    private static final String[] CATEGORY_KEYWORDS = {
        "mobile", "mobiles", "phone", "phones", "smartphone",
        "laptop", "laptops", "notebook",
        "tv", "television", "televisions", "smart tv",
        "shoes", "shoe", "footwear", "sneakers", "sandals", "running shoes",
        "clothing", "clothes", "shirt", "shirts", "jeans", "dress", "kurta",
        "books", "book",
        "electronics", "electronic", "earbuds", "speaker", "headphone", "headphones",
        "furniture", "bed", "mattress", "sofa"
    };

    private static final java.util.Map<String, String> KEYWORD_TO_CATEGORY = new java.util.HashMap<>();
    static {
        for (String[] pair : new String[][] {
            {"mobile","Mobile"},{"mobiles","Mobile"},{"phone","Mobile"},
            {"phones","Mobile"},{"smartphone","Mobile"},
            {"laptop","Laptop"},{"laptops","Laptop"},{"notebook","Laptop"},
            {"tv","TV"},{"television","TV"},{"televisions","TV"},{"smart tv","TV"},
            {"shoes","Shoes"},{"shoe","Shoes"},{"footwear","Shoes"},
            {"sneakers","Shoes"},{"sandals","Shoes"},{"running shoes","Shoes"},
            {"clothing","Clothing"},{"clothes","Clothing"},{"shirt","Clothing"},
            {"shirts","Clothing"},{"jeans","Clothing"},{"dress","Clothing"},{"kurta","Clothing"},
            {"books","Books"},{"book","Books"},
            {"electronics","Electronics"},{"electronic","Electronics"},
            {"earbuds","Electronics"},{"speaker","Electronics"},
            {"headphone","Electronics"},{"headphones","Electronics"},
            {"furniture","Furniture"},{"bed","Furniture"},{"mattress","Furniture"},{"sofa","Furniture"}
        }) {
            KEYWORD_TO_CATEGORY.put(pair[0], pair[1]);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Main entry point
    // ─────────────────────────────────────────────────────────────────────────

    public ChatResponse processMessage(String userMessage) {
        String msg = userMessage.toLowerCase().trim();

        // 0. Greeting / help — no DB query needed
        if (msg.matches("(hi|hello|hey|hiya|namaste|good morning|good evening|good afternoon|howdy)(\\s.*)?")) {
            return new ChatResponse(
                "👋 Hi! I'm your **Ecom Store Shopping Assistant** powered by Gemini AI.\n\n" +
                "Ask me anything about our products:\n" +
                "• _\"Show me mobiles\"_\n" +
                "• _\"Laptops under ₹1,00,000\"_\n" +
                "• _\"Best earbuds in stock\"_\n" +
                "• _\"Cheapest shoes\"_",
                java.util.List.of()
            );
        }
        if (msg.contains("help") || msg.contains("what can you") || msg.contains("how do")) {
            return new ChatResponse(
                "🛍️ I can help you find products! Try:\n\n" +
                "• **By category** — _\"Show me TVs\"_, _\"Books\"_, _\"Furniture\"_\n" +
                "• **By price** — _\"Mobiles under ₹20,000\"_\n" +
                "• **Stock filter** — _\"Laptops in stock\"_\n" +
                "• **Best deals** — _\"Cheapest electronics\"_",
                java.util.List.of()
            );
        }

        // 1. Extract intent signals
        Double maxPrice   = extractPrice(msg);
        String  category  = detectCategory(msg);
        boolean wantStock = msg.contains("in stock") || msg.contains("available");
        boolean isCheap   = msg.contains("cheapest") || msg.contains("budget") || msg.contains("affordable");

        // 2. Query DB for matching products (up to 10 for context)
        List<Product> contextProducts = fetchContextProducts(category, maxPrice, wantStock, isCheap, msg);

        // 3. Build AI prompt with real product data injected
        String systemPrompt = buildSystemPrompt(contextProducts);

        // 4. Call Gemini API
        String aiReply = geminiService.generate(systemPrompt, userMessage);

        // 5. If AI fails → use a polished fallback
        if (aiReply == null || aiReply.isBlank()) {
            aiReply = buildFallbackReply(contextProducts, category, maxPrice);
        }

        // 6. Build product link cards (always from DB — never hallucinated)
        List<ChatResponse.ProductLink> links = buildProductLinks(contextProducts);

        return new ChatResponse(aiReply, links);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DB query
    // ─────────────────────────────────────────────────────────────────────────

    private List<Product> fetchContextProducts(String category, Double maxPrice,
                                               boolean wantStock, boolean isCheap, String msg) {
        List<Product> results;

        if (maxPrice != null && category != null && wantStock) {
            results = productRepository.findInStockByCategoryUnderPrice(category, maxPrice);
        } else if (maxPrice != null && category != null) {
            results = productRepository.findByIsActiveTrueAndCategoryIgnoreCaseAndDiscountPriceLessThanEqual(category, maxPrice);
        } else if (maxPrice != null) {
            results = productRepository.findByIsActiveTrueAndDiscountPriceLessThanEqual(maxPrice);
        } else if (category != null) {
            results = productRepository.findByIsActiveTrueAndCategoryIgnoreCase(category);
        } else {
            // keyword search
            String keyword = extractKeyword(msg);
            results = productRepository.findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(keyword, keyword);
            results = results.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList();
        }

        if (isCheap) {
            results = results.stream()
                    .sorted((a, b) -> Double.compare(
                            a.getDiscountPrice() == null ? 0 : a.getDiscountPrice(),
                            b.getDiscountPrice() == null ? 0 : b.getDiscountPrice()))
                    .toList();
        }

        // Cap at 10 products to keep the prompt concise
        return results.size() > 10 ? results.subList(0, 10) : results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Prompt builder — injects real product data
    // ─────────────────────────────────────────────────────────────────────────

    private String buildSystemPrompt(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a friendly and helpful shopping assistant for Ecom Store, ")
          .append("an online e-commerce platform in India. ")
          .append("Answer the customer's question in a natural, conversational way. ")
          .append("Keep your reply concise (under 120 words). ")
          .append("Do not make up products or prices — only refer to what is listed below.\n\n");

        if (products.isEmpty()) {
            sb.append("No matching products found in the store for this query.\n");
        } else {
            sb.append("Matching products from our store:\n");
            for (int i = 0; i < products.size(); i++) {
                Product p = products.get(i);
            String stock = (p.getStock() > 0) ? "In Stock" : "Out of Stock";
            String discount = (p.getDiscount() > 0)
                    ? ", " + p.getDiscount() + "% off" : "";
            sb.append(String.format("%d. %s — \u20b9%,.0f%s (%s) [Category: %s]\n",
                    i + 1, p.getTitle(), p.getDiscountPrice(), discount, stock, p.getCategory()));
            }
        }

        sb.append("\nInstructions: Be helpful, mention relevant product names and prices from the list above. ")
          .append("If nothing matches, suggest the customer browse the store or ask differently.");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Fallback when AI API is unavailable
    // ─────────────────────────────────────────────────────────────────────────

    private String buildFallbackReply(List<Product> products, String category, Double maxPrice) {
        if (products.isEmpty()) {
            String msg = "Sorry, I couldn't find matching products";
            if (category != null) msg += " in **" + category + "**";
            if (maxPrice != null) msg += " under ₹" + String.format("%,.0f", maxPrice);
            return msg + ". Try a different search or browse all products.";
        }

        StringBuilder sb = new StringBuilder();
        if (category != null && maxPrice != null)
            sb.append("Found **").append(products.size()).append("** ").append(category)
              .append(" products under ₹").append(String.format("%,.0f", maxPrice)).append(":\n\n");
        else if (category != null)
            sb.append("Here are our **").append(category).append("** products:\n\n");
        else if (maxPrice != null)
            sb.append("Found **").append(products.size()).append("** products under ₹")
              .append(String.format("%,.0f", maxPrice)).append(":\n\n");
        else
            sb.append("Here are some products that might interest you:\n\n");

        products.stream().limit(6).forEach(p -> {
            String stock = (p.getStock() > 0) ? "✅" : "❌";
            sb.append("• **").append(p.getTitle()).append("** — ₹")
              .append(String.format("%,.0f", p.getDiscountPrice()))
              .append(" ").append(stock).append("\n");
        });
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Build clickable product cards for the frontend
    // ─────────────────────────────────────────────────────────────────────────

    private List<ChatResponse.ProductLink> buildProductLinks(List<Product> products) {
        List<ChatResponse.ProductLink> links = new ArrayList<>();
        products.stream().limit(6).forEach(p ->
            links.add(new ChatResponse.ProductLink(
                    p.getId(), p.getTitle(), p.getDiscountPrice(),
                    p.getStock() > 0)));
        return links;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Double extractPrice(String msg) {
        Pattern p = Pattern.compile(
            "(?:under|below|less than|upto|up to|within|max|maximum)\\s*(?:rs\\.?|₹|inr)?\\s*([0-9][0-9,\\.]*)",
            Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(msg);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1).replaceAll(",", "")); }
            catch (NumberFormatException ignored) {}
        }
        Pattern p2 = Pattern.compile("(?:₹|rs\\.?)\\s*([0-9][0-9,\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(msg);
        if (m2.find()) {
            try { return Double.parseDouble(m2.group(1).replaceAll(",", "")); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String detectCategory(String msg) {
        for (String kw : CATEGORY_KEYWORDS) {
            if (msg.matches(".*\\b" + Pattern.quote(kw) + "\\b.*")) {
                return KEYWORD_TO_CATEGORY.get(kw);
            }
        }
        return null;
    }

    private String extractKeyword(String msg) {
        return msg.replaceAll("(?i)(do you have|show me|find|search for|looking for|i want|i need|any|some|tell me about)", "").trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Response DTO (unchanged — frontend stays the same)
    // ─────────────────────────────────────────────────────────────────────────

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

            public Integer getId()     { return id; }
            public String getTitle()   { return title; }
            public Double getPrice()   { return price; }
            public boolean isInStock() { return inStock; }
        }
    }
}
