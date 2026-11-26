package com.cartvr.app.domain

import com.cartvr.app.data.local.entity.CategoryEntity
import com.cartvr.app.data.local.entity.ItemHistoryEntity

data class ParsedItemInput(
    val name: String,
    val quantity: String?,
    val note: String?,
    val suggestedCategoryId: String?
)

object SmartItemParser {

    // Matches leading quantity tokens such as:
    // "2L", "500ml", "1.5kg", "250g", "6x", "12pk", "2 dozen", "3"
    private val leadingQtyRegex = Regex(
        pattern = """^(\d+(?:[.,]\d+)?\s*(?:l|ml|kg|g|mg|pk|pack|packs|x|dozen|doz|cans|tins|bottles|btl|rolls|loaf|loaves)?)\s+(.+)$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    // Matches inline note: "Item name - the low-fat one"
    private val dashNoteRegex = Regex("""^(.+?)\s+-\s+(.+)$""")

    // Matches inline note in parentheses: "Item name (Woolies)"
    private val parenNoteRegex = Regex("""^(.+?)\s*\(([^)]+)\)\s*$""")

    private val categoryKeywords: Map<String, List<String>> = mapOf(
        "Dairy & Fridge" to listOf(
            "milk", "cheese", "cheddar", "gouda", "mozzarella", "butter", "rama",
            "stork", "margarine", "yogurt", "yoghurt", "cream", "egg", "eggs",
            "custard", "ultramel", "feta", "cottage cheese", "maas", "amasi"
        ),
        "Fresh Produce" to listOf(
            "apple", "apples", "banana", "bananas", "tomato", "tomatoes", "onion",
            "onions", "potato", "potatoes", "carrot", "carrots", "lettuce", "spinach",
            "avocado", "avo", "lemon", "orange", "oranges", "naartjie", "naartjies",
            "butternut", "gem squash", "mielies", "sweet potato", "berry", "berries",
            "cucumber", "pepper", "peppers", "garlic", "ginger", "broccoli", "grape",
            "grapes", "mushroom", "mushrooms"
        ),
        "Meat & Poultry" to listOf(
            "chicken", "beef", "mince", "steak", "pork", "lamb", "boerewors",
            "wors", "biltong", "droewors", "droëwors", "polony", "viennas",
            "russians", "sosaties", "chops", "tjops", "eisbein", "sausage",
            "bacon", "ham", "fish", "salmon", "hake", "tuna", "fillet",
            "drumstick", "thigh", "ribs"
        ),
        "Bakery" to listOf(
            "bread", "albany", "sasko", "loaf", "rolls", "bun", "buns", "vetkoek",
            "roosterkoek", "croissant", "muffin", "bagel", "wrap", "wraps", "pita",
            "cake", "rusk", "rusks", "ouma", "scones"
        ),
        "Frozen" to listOf(
            "frozen", "ice cream", "ola", "mccain", "peas", "mixed veg",
            "oven chips", "chips", "fries", "nuggets", "fish fingers", "pizza",
            "ice", "sorbet"
        ),
        "Drinks & Snacks" to listOf(
            "juice", "water", "coke", "cola", "sparletta", "creme soda", "stoney",
            "oros", "appletiser", "grapetiser", "soda", "coffee", "ricoffy", "jacobs",
            "tea", "rooibos", "five roses", "joko", "beer", "wine", "simba",
            "niknaks", "flings", "crisps", "doritos", "chocolate", "tennis biscuits",
            "marie biscuits", "biscuit", "biscuits", "cookies", "provita", "nuts",
            "popcorn", "sweets", "candy", "energade", "powerade"
        ),
        "Household & Cleaning" to listOf(
            "braai wood", "charcoal", "briquettes", "firelighters", "blitz",
            "doom", "peaceful sleep", "soap", "detergent", "omo", "skip", "sta-soft",
            "bleach", "jeyes", "domestos", "toilet paper", "twinsaver", "baby soft",
            "tissues", "paper towel", "sponge", "bin bag", "bin bags", "trash bag",
            "dishwash", "sunlight", "foil", "cling wrap", "handy andy", "shampoo",
            "toothpaste", "deodorant"
        ),
        "Pet Supplies" to listOf(
            "dog", "cat", "pet", "bobtail", "montego", "whiskas", "kibble",
            "cat food", "dog food", "litter", "treats"
        ),
        "Pantry & Tins" to listOf(
            "pap", "mielie meal", "maize meal", "iwisa", "chakalaka", "mrs balls",
            "chutney", "all gold", "tomato sauce", "bovril", "marmite", "black cat",
            "peanut butter", "weetbix", "pronutro", "oats", "milo", "condensed milk",
            "rice", "pasta", "spaghetti", "macaroni", "flour", "sugar", "salt",
            "pepper", "oil", "olive oil", "vinegar", "sauce", "baked beans",
            "beans", "koo", "tin", "cereal", "honey", "jam", "mayo", "mayonnaise",
            "spices", "aromat", "braai salt", "stock"
        )
    )

    fun parse(
        rawInput: String,
        categories: List<CategoryEntity>,
        history: List<ItemHistoryEntity> = emptyList()
    ): ParsedItemInput {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParsedItemInput(name = "", quantity = null, note = null, suggestedCategoryId = null)
        }

        // 1. Extract inline note (" - note" or "(note)")
        var withoutNote = trimmed
        var extractedNote: String? = null

        val dashMatch = dashNoteRegex.matchEntire(withoutNote)
        if (dashMatch != null) {
            withoutNote = dashMatch.groupValues[1].trim()
            extractedNote = dashMatch.groupValues[2].trim().ifBlank { null }
        } else {
            val parenMatch = parenNoteRegex.matchEntire(withoutNote)
            if (parenMatch != null) {
                withoutNote = parenMatch.groupValues[1].trim()
                extractedNote = parenMatch.groupValues[2].trim().ifBlank { null }
            }
        }

        // 2. Extract leading quantity ("2x", "2L", "500g", etc.)
        val qtyMatch = leadingQtyRegex.matchEntire(withoutNote)
        val quantity: String?
        val itemName: String

        if (qtyMatch != null) {
            quantity = qtyMatch.groupValues[1].trim()
            itemName = qtyMatch.groupValues[2].trim()
        } else {
            quantity = null
            itemName = withoutNote
        }

        val normalizedName = itemName.lowercase()

        // 3. Learned aisle from item_history takes priority over keyword defaults
        val learnedCategoryId = history.firstOrNull {
            it.normalizedName == normalizedName && it.defaultCategoryId != null
        }?.defaultCategoryId?.takeIf { id -> categories.any { it.id == id } }

        val suggestedCategoryId = learnedCategoryId ?: run {
            val matchedCategoryName = categoryKeywords.entries.firstOrNull { (_, keywords) ->
                keywords.any { kw -> normalizedName.contains(kw) }
            }?.key

            matchedCategoryName?.let { targetName ->
                categories.firstOrNull { it.name.equals(targetName, ignoreCase = true) }?.id
            }
        }

        return ParsedItemInput(
            name = itemName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            quantity = quantity,
            note = extractedNote,
            suggestedCategoryId = suggestedCategoryId
        )
    }
}
