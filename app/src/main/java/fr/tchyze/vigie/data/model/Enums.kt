package fr.tchyze.vigie.data.model

enum class Category(val key: String, val label: String) {
    FIREWALL("firewall", "Firewalls"),
    SWITCH("switch", "Switchs"),
    OS("os", "Systèmes"),
    SERVICE("service", "Services"),
    ;

    companion object {
        fun of(key: String): Category? = entries.firstOrNull { it.key == key }
    }
}

/** Statut d'une branche, du moins grave au plus grave. */
enum class BranchStatus(val key: String) {
    ACTIVE("active"),
    EOL_SOON("eol_soon"),
    EOL("eol"),
    ;

    companion object {
        fun of(key: String): BranchStatus = entries.firstOrNull { it.key == key } ?: ACTIVE
    }
}

enum class Severity(val key: String, val label: String) {
    CRITICAL("critical", "Critique"),
    HIGH("high", "Élevée"),
    MEDIUM("medium", "Moyenne"),
    LOW("low", "Faible"),
    INFO("info", "Information"),
    ;

    companion object {
        fun of(key: String?): Severity? = entries.firstOrNull { it.key == key }
    }
}

/** Libellés français des quatre dates de fin de support du schéma. */
object EolKeys {
    const val ACTIVE_SUPPORT = "end_of_active_support"
    const val SECURITY_SUPPORT = "end_of_security_support"
    const val END_OF_LIFE = "end_of_life"
    const val EXTENDED_SUPPORT = "end_of_extended_support"

    fun label(key: String): String = when (key) {
        ACTIVE_SUPPORT -> "Fin du support actif"
        SECURITY_SUPPORT -> "Fin des correctifs de sécurité"
        END_OF_LIFE -> "Fin de vie"
        EXTENDED_SUPPORT -> "Fin du support étendu"
        else -> key
    }
}
