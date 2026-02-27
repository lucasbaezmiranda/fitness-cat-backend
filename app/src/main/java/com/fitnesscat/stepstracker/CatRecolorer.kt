package com.fitnesscat.stepstracker

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Recolorea la imagen base del gato en runtime según un mapa de partes -> color RGB.
 * Usa la misma lógica que recolor_cat.py: paleta Top-20, cuantización al más cercano, reemplazo por grupo.
 * Bordes siempre negro.
 */
object CatRecolorer {

    private const val TOP_N = 20

    // Grupo 0=Cuerpo, 1=Bordes, 2=Manchas, 3=Ojos, 4=Orejas. IDs son índices en la paleta (0..19).
    private val SIMILAR_COLORS: List<IntArray> = listOf(
        intArrayOf(1),                                    // 0 -> Cuerpo
        intArrayOf(2, 6, 9, 10, 11, 12, 15, 18),         // 1 -> Bordes (siempre negro)
        intArrayOf(3, 7, 13, 14, 16, 17, 19),            // 2 -> Manchas
        intArrayOf(4),                                    // 3 -> Ojos
        intArrayOf(5)                                     // 4 -> Orejas
    )

    // 5 opciones de color por parte (RGB como 0x00RRGGBB). Bordes no se usa (siempre negro).
    val CUERPO_OPTIONS = intArrayOf(
        rgb(210, 140, 70),   // marrón claro
        rgb(25, 25, 25),     // negro
        rgb(60, 40, 35),     // marrón oscuro
        rgb(225, 210, 185),  // crema
        rgb(120, 75, 60)     // marrón
    )
    val OJOS_OPTIONS = intArrayOf(
        rgb(210, 170, 40),   // amarillo
        rgb(240, 200, 60),   // amarillo claro
        rgb(110, 180, 90),   // verde
        rgb(120, 170, 220),  // azul
        rgb(190, 160, 60)    // ámbar
    )
    val OREJAS_OPTIONS = intArrayOf(
        rgb(240, 185, 170),  // rosa claro
        rgb(200, 150, 160),  // rosa oscuro
        rgb(220, 170, 150),  // beige
        rgb(245, 190, 195),  // rosa
        rgb(215, 170, 150)   // melocotón
    )
    val MANCHAS_OPTIONS = intArrayOf(
        rgb(130, 75, 40),    // marrón
        rgb(75, 45, 35),     // marrón muy oscuro
        rgb(90, 100, 110),   // gris
        rgb(220, 195, 160),  // beige claro
        rgb(95, 75, 55)      // marrón
    )

    private const val BORDES_RGB = 0x00191919  // negro

    private fun rgb(r: Int, g: Int, b: Int): Int {
        return (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    private fun r(c: Int): Int = Color.red(c)
    private fun g(c: Int): Int = Color.green(c)
    private fun b(c: Int): Int = Color.blue(c)

    /** Paleta: 20 colores como 0x00RRGGBB, extraídos de la imagen de referencia. */
    private var palette: IntArray? = null

    /**
     * Inicializa la paleta desde una imagen de referencia (solo RGB, se ignoran píxeles muy transparentes).
     * Debe llamarse al menos una vez antes de recolor (p. ej. desde Application o primer uso).
     */
    fun initPaletteFromBitmap(bitmap: Bitmap) {
        palette = extractTopColors(bitmap, TOP_N)
    }

    /**
     * Extrae los top_n colores más frecuentes (por conteo de píxeles). RGB sin alpha.
     */
    private fun extractTopColors(bitmap: Bitmap, topN: Int): IntArray {
        val count = mutableMapOf<Int, Int>()
        val w = bitmap.width
        val h = bitmap.height
        for (x in 0 until w) {
            for (y in 0 until h) {
                val px = bitmap.getPixel(x, y)
                if (Color.alpha(px) < 128) continue  // ignorar muy transparentes
                val rgb = (Color.red(px) shl 16) or (Color.green(px) shl 8) or Color.blue(px)
                count[rgb] = (count[rgb] ?: 0) + 1
            }
        }
        val sorted = count.entries.sortedByDescending { it.value }.take(topN)
        return sorted.map { it.key }.toIntArray()
    }

    /**
     * Índice del color de la paleta más cercano a (r,g,b) por distancia euclidiana al cuadrado.
     */
    private fun nearestPaletteIndex(r: Int, g: Int, b: Int, palette: IntArray): Int {
        var best = 0
        var bestDist = Long.MAX_VALUE
        for (i in palette.indices) {
            val pr = r(palette[i])
            val pg = g(palette[i])
            val pb = b(palette[i])
            val dr = (r - pr).toLong(); val dg = (g - pg).toLong(); val db = (b - pb).toLong()
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                best = i
            }
        }
        return best
    }

    /**
     * Recolorea el bitmap según el mapa de partes -> color (0x00RRGGBB).
     * Claves: "cuerpo", "ojos", "orejas", "manchas". "bordes" se fuerza a negro si no se pasa.
     */
    fun recolor(source: Bitmap, colors: Map<String, Int>): Bitmap {
        val pal = palette
            ?: throw IllegalStateException("Call initPaletteFromBitmap() first with reference cat image")
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source

        val mapping = IntArray(TOP_N)
        for (i in 0 until TOP_N) mapping[i] = -1
        val cuerpo = colors["cuerpo"] ?: CUERPO_OPTIONS[0]
        val ojos = colors["ojos"] ?: OJOS_OPTIONS[0]
        val orejas = colors["orejas"] ?: OREJAS_OPTIONS[0]
        val manchas = colors["manchas"] ?: MANCHAS_OPTIONS[0]
        for (group in 0 until 5) {
            val newRgb = when (group) {
                0 -> cuerpo
                1 -> BORDES_RGB
                2 -> manchas
                3 -> ojos
                4 -> orejas
                else -> cuerpo
            }
            for (id in SIMILAR_COLORS[group]) {
                if (id in 0 until TOP_N) mapping[id] = newRgb
            }
        }

        val w = mutable.width
        val h = mutable.height
        for (x in 0 until w) {
            for (y in 0 until h) {
                val px = mutable.getPixel(x, y)
                val alpha = Color.alpha(px)
                val pr = Color.red(px)
                val pg = Color.green(px)
                val pb = Color.blue(px)
                val idx = nearestPaletteIndex(pr, pg, pb, pal)
                val newRgb = mapping[idx]
                if (newRgb >= 0) {
                    mutable.setPixel(x, y, (alpha shl 24) or (newRgb and 0x00FFFFFF))
                }
            }
        }
        return mutable
    }

    /**
     * Construye el mapa de colores a partir de los índices (0..4) por parte.
     */
    fun colorsFromIndices(cuerpoIdx: Int, ojosIdx: Int, orejasIdx: Int, manchasIdx: Int): Map<String, Int> {
        return mapOf(
            "cuerpo" to CUERPO_OPTIONS[cuerpoIdx.coerceIn(0, CUERPO_OPTIONS.size - 1)],
            "ojos" to OJOS_OPTIONS[ojosIdx.coerceIn(0, OJOS_OPTIONS.size - 1)],
            "orejas" to OREJAS_OPTIONS[orejasIdx.coerceIn(0, OREJAS_OPTIONS.size - 1)],
            "manchas" to MANCHAS_OPTIONS[manchasIdx.coerceIn(0, MANCHAS_OPTIONS.size - 1)]
        )
    }
}
