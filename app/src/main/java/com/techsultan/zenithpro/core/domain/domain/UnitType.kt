package com.techsultan.zenithpro.core.domain.domain

enum class UnitType(
    val label: String,
    val abbreviation: String,
    val isDecimal: Boolean,
) {
    // Add to the Discrete group — but with isDecimal = true
    BAG("Per Bag", "bag", true),
    SACHET("Per Sachet", "sachet", false),
    BOTTLE("Per Bottle", "bottle", false),
    TIN("Per Tin", "tin", false),
    ROLL("Per Roll", "roll", true),
    BUNDLE("Per Bundle", "bundle", false),
    PALLET("Per Pallet", "pallet", false),
    UNIT("Per Unit", "unit", false),
    PIECE("Per Piece", "pcs", false),
    PACK("Per Pack", "pack", false),
    DOZEN("Per Dozen", "doz", false),
    CARTON("Per Carton", "ctn", false),

    // Weight — decimals
    KILOGRAM("Per Kilogram", "kg", true),
    GRAM("Per Gram", "g", true),
    POUND("Per Pound", "lb", true),

    // Volume — decimals (paint shop)
    LITRE("Per Litre", "L", true),
    MILLILITRE("Per Millilitre", "mL", true),
    GALLON("Per Gallon", "gal", true),

    // Length — decimals (fabric)
    METRE("Per Metre", "m", true),
    YARD("Per Yard", "yd", true),
    FOOT("Per Foot", "ft", true);

    companion object {
        fun fromString(value: String?): UnitType {
            if (value == null) return UNIT
            val cleanValue = value.uppercase().trim()
            return entries.firstOrNull { 
                it.name == cleanValue || 
                it.label.uppercase() == cleanValue || 
                it.abbreviation.uppercase() == cleanValue ||
                (it.name + "S") == cleanValue || 
                it.name == cleanValue.trimEnd('S') 
            } ?: when(cleanValue) {
                "ML" -> MILLILITRE
                "L" -> LITRE
                "PCS" -> PIECE
                "CTN" -> CARTON
                "DOZ" -> DOZEN
                "LB" -> POUND
                "G" -> GRAM
                "KG" -> KILOGRAM
                "M" -> METRE
                "YD" -> YARD
                "FT" -> FOOT
                else -> UNIT
            }
        }
    }
}