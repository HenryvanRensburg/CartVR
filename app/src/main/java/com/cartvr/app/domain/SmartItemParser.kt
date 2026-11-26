package com.cartvr.app.domain

import com.cartvr.app.data.local.entity.CategoryEntity

data class ParsedItemInput(
    val name: String,
    val quantity: String?,
    val suggestedCategoryId: String?
)

object SmartItemParser {

    // Matches leading quantity tokens such as:
    // "2L", "500ml", "1.5kg", "250g", "6x", "12pk", "2 dozen", "3"
    private val leadingQtyRegex = Regex(
        pattern = """^(\d+(?:[.,]\d+)?\s*(?:l|ml|kg|g|mg|pk|pack|packs|x|dozen|doz|cans|tins|bottles|btl|rolls|loaf|loaves)?)\s+(.+)$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val categoryKeywords: Map<String, List<String>> = mapOf(
        "Dairy & Fridge" to listOf(
            "milk", "cheese", "cheddar", "mozzarella", "butter", "margarine",
            "yogurt", "yoghurt", "cream", "egg", "eggs", "custard", "feta", "maas"
        ),
        "Fresh Produce" to listOf(
            "apple", "apples", "banana", "bananas", "tomato", "tomatoes", "onion",
            "onions", "potato", "potatoes", "carrot", "carrots", "lettuce", "spinach",
            "avocado", "avo", "lemon", "orange", "oranges", "berry", "berries",
            "cucumber", "pepper", "peppers", "garlic", "ginger", "broccoli", "grape"
        ),
        "Meat & Poultry" to listOf(
            "chicken", "beef", "mince", "steak", "pork", "lamb", "boerewors",
            "wors", "sausage", "bacon", "ham", "biltong", "fish", "salmon",
            "hake", "tuna", "fillet", "drumstick", "thigh", "ribs"
        ),
        "Bakery" to listOf(
            "bread", "loaf", "rolls", "bun", "buns", "croissant", "muffin",
            "bagel", "wrap", "wraps", "pita", "cake", "rusk", "rusks"
        ),
        "Frozen" to listOf(
            "frozen", "ice cream", "peas", "chips", "fries", "nuggets",
            "pizza", "ice", "sorbet"
        ),
        "Drinks & Snacks" to listOf(
            "juice", "water", "coke", "cola", "soda", "coffee", "tea", "rooibos",
            "beer", "wine", "crisps", "doritos", "chocolate", "biscuit", "biscuits",
            "cookies", "nuts", "popcorn", "sweets", "candy", "energade"
        ),
        "Household & Cleaning" to listOf(
            "soap", "detergent", "omo", "skip", "bleach", "jeyes", "domestos",
            "toilet paper", "tissues", "paper towel", "sponge", "bin bag",
            "bin bags", "trash bag", "dishwash", "sunlight", "foil", "cling wrap",
            "handy andy", "shampoo", "toothpaste", "deodorant"
        ),
        "Pet Supplies" to listOf(
            "dog", "cat", "pet", "kibble", "cat food", "dog food", "litter", "treats"
        ),
        "Pantry & Tins" to listOf(
            "rice", "pasta", "spaghetti", "macaroni", "flour", "sugar", "salt",
            "pepper", "oil", "olive oil", "vinegar", "sauce", "tomatosauce",
            "baked beans", "beans", "tin", "cereal", "oats", "weetbix", "honey",
            "jam", "peanut butter", "mayo", "mayonnaise", "spices", "stock"
        )
    )

    fun parse(rawInput: String, categories: List<CategoryEntity>): ParsedItemInput {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParsedItemInput(name = "", quantity = null, suggestedCategoryId = null)
        }

        val match = leadingQtyRegex.matchEntire(trimmed)
        val quantity: String?
        val itemName: String

        if (match != null) {
            quantity = match.groupValues[1].trim()
            itemName = match.groupValues[2].trim()
        } else {
            quantity = null
            itemName = trimmed
        }

        val lowerName = itemName.lowercase()
        val matchedCategoryName = categoryKeywords.entries.firstOrNull { (_, keywords) ->
            keywords.any { kw -> lowerName.contains(kw) }
        }?.key

        val categoryId = matchedCategoryName?.let { targetName ->
            categories.firstOrNull { it.name.equals(targetName, ignoreCase = true) }?.id
        }

        return ParsedItemInput(
            name = itemName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            quantity = quantity,
            suggestedCategoryId = categoryId
        )
    }
}
