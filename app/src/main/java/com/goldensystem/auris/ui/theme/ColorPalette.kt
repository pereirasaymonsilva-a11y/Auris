// ui/theme/ColorPalette.kt
package com.goldensystem.auris.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de cores completa para o seletor
val COLOR_PALETTE = listOf(
    // Tons de Roxo
    ColorPaletteItem("#7B2CBF", "Roxo Escuro"),
    ColorPaletteItem("#9D4EDD", "Roxo Médio"),
    ColorPaletteItem("#C77DFF", "Roxo Claro"),
    ColorPaletteItem("#E0AAFF", "Roxo Pastel"),
    
    // Tons de Azul
    ColorPaletteItem("#023E8A", "Azul Escuro"),
    ColorPaletteItem("#0077B6", "Azul Médio"),
    ColorPaletteItem("#0096C7", "Azul Claro"),
    ColorPaletteItem("#00B4D8", "Azul Ciano"),
    ColorPaletteItem("#48CAE4", "Ciano Claro"),
    ColorPaletteItem("#90E0EF", "Ciano Pastel"),
    
    // Tons de Verde
    ColorPaletteItem("#004B23", "Verde Escuro"),
    ColorPaletteItem("#006400", "Verde Médio"),
    ColorPaletteItem("#008000", "Verde"),
    ColorPaletteItem("#38B000", "Verde Limão"),
    ColorPaletteItem("#70E000", "Verde Claro"),
    ColorPaletteItem("#9EF01A", "Verde Neon"),
    
    // Tons de Amarelo/Laranja
    ColorPaletteItem("#FF6D00", "Laranja Escuro"),
    ColorPaletteItem("#FF9100", "Laranja"),
    ColorPaletteItem("#FFAB00", "Laranja Claro"),
    ColorPaletteItem("#FFD600", "Amarelo"),
    ColorPaletteItem("#FFEA00", "Amarelo Claro"),
    
    // Tons de Vermelho/Rosa
    ColorPaletteItem("#B71C1C", "Vermelho Escuro"),
    ColorPaletteItem("#D32F2F", "Vermelho"),
    ColorPaletteItem("#F44336", "Vermelho Claro"),
    ColorPaletteItem("#E91E63", "Rosa"),
    ColorPaletteItem("#F06292", "Rosa Claro"),
    ColorPaletteItem("#F8BBD0", "Rosa Pastel"),
    
    // Tons de Cinza
    ColorPaletteItem("#212121", "Cinza Escuro"),
    ColorPaletteItem("#424242", "Cinza Médio"),
    ColorPaletteItem("#757575", "Cinza"),
    ColorPaletteItem("#BDBDBD", "Cinza Claro"),
    ColorPaletteItem("#E0E0E0", "Cinza Pastel"),
    
    // Branco e Preto
    ColorPaletteItem("#FFFFFF", "Branco"),
    ColorPaletteItem("#000000", "Preto"),
    
    // Cores Especiais
    ColorPaletteItem("#1A1A2E", "Azul Noturno"),
    ColorPaletteItem("#16213E", "Azul Profundo"),
    ColorPaletteItem("#0F3460", "Azul Marinho"),
    ColorPaletteItem("#533483", "Roxo Profundo"),
    ColorPaletteItem("#E94560", "Vermelho Coral"),
    ColorPaletteItem("#F5A623", "Dourado"),
    ColorPaletteItem("#4A90D9", "Azul Royal"),
    ColorPaletteItem("#50E3C2", "Turquesa"),
    ColorPaletteItem("#B8E986", "Verde Maçã"),
    ColorPaletteItem("#F8E71C", "Amarelo Sol")
)

data class ColorPaletteItem(
    val hex: String,
    val name: String
) {
    val color: Color
        get() = Color(hex.removePrefix("#").toLong(16) or 0xFF00000000)
}