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
            "milk", "melk", "cheese", "kaas", "cheddar", "gouda", "mozzarella", "feta",
            "maaskaas", "smeerkaas", "cottage cheese", "butter", "botter", "butro",
            "rama", "stork", "margarine", "yogurt", "yoghurt", "cream", "room",
            "suurroom", "egg", "eggs", "eiers", "custard", "vla", "ultramel",
            "maas", "amasi"
        ),
        "Fresh Produce" to listOf(
            "apple", "apples", "appels", "banana", "bananas", "piesang", "piesangs",
            "tomato", "tomatoes", "tamaties", "onion", "onions", "ui", "uie",
            "potato", "potatoes", "aartappel", "aartappels", "carrot", "carrots",
            "wortels", "lettuce", "slaai", "groenslaai", "blare", "spinach",
            "spinasie", "avocado", "avo", "lemon", "orange", "oranges", "naartjie",
            "naartjies", "butternut", "gem squash", "mielies", "sweet potato",
            "berry", "berries", "cucumber", "komkommer", "pepper", "peppers",
            "garlic", "knoffel", "ginger", "broccoli", "grape", "grapes", "mushroom",
            "mushrooms", "kool", "boontjies", "pynappel", "waartlemoen", "groente",
            "vrugte", "chillies", "parsley", "basil", "herbs"
        ),
        "Meat & Poultry" to listOf(
            "chicken", "hoender", "beef", "mince", "steak", "pork", "vark", "lamb",
            "boerewors", "wors", "biltong", "droewors", "droëwors", "polony",
            "viennas", "russians", "sosaties", "chops", "tjops", "eisbein", "sausage",
            "bacon", "rashers", "ham", "fish", "vis", "salmon", "hake", "tuna",
            "mossels", "prawns", "fillet", "drumstick", "thigh", "ribs", "vleis",
            "skilpaadjies", "patties", "hamburgers", "pies", "sushi"
        ),
        "Bakery" to listOf(
            "bread", "brood", "albany", "sasko", "loaf", "rolls", "bun", "buns",
            "vetkoek", "vetkoeke", "deeg", "roosterkoek", "croissant", "muffin",
            "muffins", "bagel", "wrap", "wraps", "pita", "cake", "koek", "rolkoek",
            "melktert", "koeksisters", "doughnuts", "panini", "pannekoek",
            "knoffelbrood", "piesangbrood", "potbrood", "rusk", "rusks", "beskuit",
            "ouma", "scones", "skons"
        ),
        "Frozen" to listOf(
            "frozen", "ice cream", "roomys", "ola", "mccain", "peas", "ertjies",
            "mixed veg", "oven chips", "chips", "fries", "nuggets", "schnitzel",
            "fish fingers", "visvingers", "pizza", "puff pastry", "quiche",
            "stir fry", "ice", "ys", "sorbet"
        ),
        "Drinks & Snacks" to listOf(
            "juice", "sap", "water", "coke", "cola", "sparletta", "creme soda",
            "stoney", "oros", "appletiser", "grapetiser", "soda", "sodastream",
            "koeldrank", "coffee", "koffie", "ricoffy", "jacobs", "tea", "tee",
            "rooibos", "five roses", "joko", "beer", "bier", "wine", "wyn",
            "brandewyn", "savanna", "simba", "niknaks", "flings", "crisps", "doritos",
            "chocolate", "sjokolade", "tennis biscuits", "marie biscuits", "biscuit",
            "biscuits", "koekies", "cookies", "provita", "salticrax", "kips",
            "rice cakes", "nuts", "peanuts", "popcorn", "sweets", "candy",
            "chappies", "marshmallow", "halls", "energade", "powerade", "monster",
            "prime", "tropika", "liquifruits", "hot chocolate", "dip", "nachos"
        ),
        "Household & Cleaning" to listOf(
            "braai wood", "hout", "charcoal", "briquettes", "firelighters", "blits",
            "blitz", "doom", "peaceful sleep", "muskiet", "soap", "seep",
            "detergent", "waspoeier", "omo", "skip", "sta-soft", "bleach", "chloor",
            "jik", "jeyes", "domestos", "pine gel", "toilet paper", "toilet papier",
            "toilet block", "toilet spray", "twinsaver", "baby soft", "tissues",
            "servette", "paper towel", "sponge", "sponsies", "bin bag", "bin bags",
            "swartsakke", "vuilgoedsakke", "broodsakkies", "trash bag", "dishwash",
            "dishwasher", "skottelgoed", "sunlight", "foil", "foelie", "cling wrap",
            "handy andy", "windowlene", "mr min", "shampoo", "conditioner",
            "body wash", "handwash", "toothpaste", "tandepaste", "tandeborsels",
            "vlos", "mouthwash", "deodorant", "underarm", "pads", "tampons",
            "vaseline", "zambuk", "nivea", "lyfroom", "gesig", "sonskerm", "skeer",
            "pleisters", "verband", "compral", "corenza", "rennies", "med lemon",
            "hoesstroop", "vitamiene", "watte", "oorstokkies", "batteries", "globes",
            "kerse", "lighter", "rattax", "gif", "goldilocks"
        ),
        "Pet Supplies" to listOf(
            "dog", "cat", "pet", "bobtail", "montego", "whiskas", "kibble",
            "cat food", "dog food", "hondekos", "haaskos", "haas", "hase",
            "teff", "lucern", "amos", "mienkie", "litter", "treats"
        ),
        "Pantry & Tins" to listOf(
            "pap", "mielie meal", "mielie meel", "maize meal", "iwisa", "chakalaka",
            "mrs balls", "chutney", "all gold", "tomato sauce", "tamatiesous",
            "tomato paste", "bovril", "marmite", "black cat", "peanut butter",
            "weetbix", "pronutro", "oats", "muesli", "milo", "condensed milk",
            "kondensmelk", "rice", "rys", "pasta", "spaghetti", "macaroni",
            "noodles", "flour", "meel", "koekmeel", "bakpoeier", "koeksoda",
            "sugar", "suiker", "stroop", "salt", "sout", "pepper", "peper",
            "swartpeper", "oil", "olie", "olive oil", "vinegar", "asyn", "sauce",
            "sous", "bisto", "gravy", "marinade", "mostert", "baked beans", "beans",
            "koo", "tin", "blikkie", "cereal", "honey", "jam", "konfyt",
            "appelkooskonfyt", "mayo", "mayonnaise", "spices", "paprika", "kerrie",
            "aromat", "braai salt", "stock", "sop", "jelly", "poeding", "klapper",
            "soya", "jalapeno", "gherkins"
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
