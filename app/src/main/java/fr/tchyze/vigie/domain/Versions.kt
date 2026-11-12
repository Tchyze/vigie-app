package fr.tchyze.vigie.domain

/**
 * Comparaison tolérante des numéros de version des éditeurs : `7.4.13`, `12.1.10-h2`, `10.2p1`,
 * `17.18.1a`, `10.0.26100.7019`…
 *
 * La version est découpée en blocs de chiffres et de lettres, les séparateurs étant ignorés. Les
 * blocs numériques se comparent par valeur, les blocs alphabétiques par ordre alphabétique. Un
 * suffixe supplémentaire rend la version plus récente (`12.1.10-h2` > `12.1.10`).
 */
object Versions : Comparator<String> {
    private val token = Regex("""\d+|[a-z]+""")

    private fun tokens(version: String): List<String> =
        token.findAll(version.trim().lowercase()).map { it.value }.toList()

    override fun compare(a: String, b: String): Int {
        val ta = tokens(a)
        val tb = tokens(b)
        for (i in 0 until maxOf(ta.size, tb.size)) {
            val x = ta.getOrNull(i) ?: return -1
            val y = tb.getOrNull(i) ?: return 1
            val c = compareToken(x, y)
            if (c != 0) return c
        }
        return 0
    }

    private fun compareToken(x: String, y: String): Int {
        val xNum = x[0].isDigit()
        val yNum = y[0].isDigit()
        return when {
            xNum && yNum -> {
                // Comparaison sans conversion : les numéros de build peuvent être très longs.
                val nx = x.trimStart('0')
                val ny = y.trimStart('0')
                if (nx.length != ny.length) nx.length.compareTo(ny.length) else nx.compareTo(ny)
            }
            xNum -> 1 // 1.0.1 > 1.0rc1
            yNum -> -1
            else -> x.compareTo(y)
        }
    }
}
