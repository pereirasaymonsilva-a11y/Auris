package com.goldensystem.auris.ui.theme

import androidx.compose.ui.graphics.Color

data class ColorPaletteItem(
    val hex: String,
    val name: String
) {
    val color: Color
        get() = Color(hex.removePrefix("#").toLong(16) or 0xFF00000000)
}

// Paleta completa com 140+ cores organizadas por família
val COLOR_PALETTE = listOf(
    // ========== ROSA / VERMELHO ==========
    ColorPaletteItem("#FFEBEE", "Rosa Claro"),
    ColorPaletteItem("#FFCDD2", "Rosa"),
    ColorPaletteItem("#EF9A9A", "Rosa Médio"),
    ColorPaletteItem("#E57373", "Rosa Intenso"),
    ColorPaletteItem("#EF5350", "Vermelho"),
    ColorPaletteItem("#F44336", "Vermelho Vivo"),
    ColorPaletteItem("#E53935", "Vermelho Escuro"),
    ColorPaletteItem("#C62828", "Vermelho Profundo"),
    ColorPaletteItem("#B71C1C", "Vermelho Sangue"),
    
    // ========== ROSA / MAGENTA ==========
    ColorPaletteItem("#FCE4EC", "Rosa Pastel"),
    ColorPaletteItem("#F8BBD0", "Rosa Bebê"),
    ColorPaletteItem("#F48FB1", "Rosa Médio"),
    ColorPaletteItem("#F06292", "Rosa Vibrante"),
    ColorPaletteItem("#EC407A", "Rosa Forte"),
    ColorPaletteItem("#E91E63", "Rosa Choque"),
    ColorPaletteItem("#D81B60", "Rosa Escuro"),
    ColorPaletteItem("#C2185B", "Rosa Profundo"),
    ColorPaletteItem("#880E4F", "Rosa Vinho"),
    
    // ========== ROXO / VIOLETA ==========
    ColorPaletteItem("#F3E5F5", "Lilás Claro"),
    ColorPaletteItem("#E1BEE7", "Lilás"),
    ColorPaletteItem("#CE93D8", "Roxo Claro"),
    ColorPaletteItem("#BA68C8", "Roxo Médio"),
    ColorPaletteItem("#AB47BC", "Roxo"),
    ColorPaletteItem("#9C27B0", "Roxo Vibrante"),
    ColorPaletteItem("#8E24AA", "Roxo Escuro"),
    ColorPaletteItem("#6A1B9A", "Roxo Profundo"),
    ColorPaletteItem("#4A148C", "Roxo Intenso"),
    
    // ========== ÍNDIGO / VIOLETA ESCURO ==========
    ColorPaletteItem("#E8EAF6", "Índigo Claro"),
    ColorPaletteItem("#C5CAE9", "Índigo"),
    ColorPaletteItem("#9FA8DA", "Índigo Médio"),
    ColorPaletteItem("#7986CB", "Índigo Vibrante"),
    ColorPaletteItem("#5C6BC0", "Índigo"),
    ColorPaletteItem("#3F51B5", "Índigo Escuro"),
    ColorPaletteItem("#3949AB", "Índigo Profundo"),
    ColorPaletteItem("#303F9F", "Índigo Intenso"),
    ColorPaletteItem("#1A237E", "Azul Noturno"),
    
    // ========== AZUL ==========
    ColorPaletteItem("#E3F2FD", "Azul Claro"),
    ColorPaletteItem("#BBDEFB", "Azul Bebê"),
    ColorPaletteItem("#90CAF9", "Azul Médio"),
    ColorPaletteItem("#64B5F6", "Azul Vibrante"),
    ColorPaletteItem("#42A5F5", "Azul"),
    ColorPaletteItem("#2196F3", "Azul Vivo"),
    ColorPaletteItem("#1E88E5", "Azul Escuro"),
    ColorPaletteItem("#1565C0", "Azul Profundo"),
    ColorPaletteItem("#0D47A1", "Azul Marinho"),
    
    // ========== AZUL CIANO / TEAL ==========
    ColorPaletteItem("#E0F7FA", "Ciano Claro"),
    ColorPaletteItem("#B2EBF2", "Ciano Pastel"),
    ColorPaletteItem("#80DEEA", "Ciano Médio"),
    ColorPaletteItem("#4DD0E1", "Ciano Vibrante"),
    ColorPaletteItem("#26C6DA", "Ciano"),
    ColorPaletteItem("#00BCD4", "Ciano Vivo"),
    ColorPaletteItem("#0097A7", "Ciano Escuro"),
    ColorPaletteItem("#00838F", "Teal"),
    ColorPaletteItem("#006064", "Teal Escuro"),
    
    // ========== VERDE ==========
    ColorPaletteItem("#E8F5E9", "Verde Claro"),
    ColorPaletteItem("#C8E6C9", "Verde Pastel"),
    ColorPaletteItem("#A5D6A7", "Verde Médio"),
    ColorPaletteItem("#81C784", "Verde Vibrante"),
    ColorPaletteItem("#66BB6A", "Verde"),
    ColorPaletteItem("#4CAF50", "Verde Vivo"),
    ColorPaletteItem("#43A047", "Verde Escuro"),
    ColorPaletteItem("#2E7D32", "Verde Floresta"),
    ColorPaletteItem("#1B5E20", "Verde Profundo"),
    
    // ========== VERDE MENTA ==========
    ColorPaletteItem("#E0F2F1", "Menta Claro"),
    ColorPaletteItem("#B2DFDB", "Menta Pastel"),
    ColorPaletteItem("#80CBC4", "Menta"),
    ColorPaletteItem("#4DB6AC", "Menta Vibrante"),
    ColorPaletteItem("#26A69A", "Menta Escura"),
    ColorPaletteItem("#009688", "Menta Profunda"),
    ColorPaletteItem("#00897B", "Menta Intensa"),
    ColorPaletteItem("#00695C", "Menta Floresta"),
    ColorPaletteItem("#004D40", "Menta Noturna"),
    
    // ========== VERDE LIMÃO ==========
    ColorPaletteItem("#F1F8E9", "Limão Claro"),
    ColorPaletteItem("#DCEDC8", "Limão Pastel"),
    ColorPaletteItem("#C5E1A5", "Limão Médio"),
    ColorPaletteItem("#AED581", "Limão Vibrante"),
    ColorPaletteItem("#9CCC65", "Limão"),
    ColorPaletteItem("#8BC34A", "Limão Vivo"),
    ColorPaletteItem("#7CB342", "Limão Escuro"),
    ColorPaletteItem("#558B2F", "Limão Profundo"),
    ColorPaletteItem("#33691E", "Limão Intenso"),
    
    // ========== AMARELO ==========
    ColorPaletteItem("#FFFDE7", "Amarelo Claro"),
    ColorPaletteItem("#FFF9C4", "Amarelo Pastel"),
    ColorPaletteItem("#FFF59D", "Amarelo Médio"),
    ColorPaletteItem("#FFF176", "Amarelo Vibrante"),
    ColorPaletteItem("#FFEE58", "Amarelo"),
    ColorPaletteItem("#FFEB3B", "Amarelo Vivo"),
    ColorPaletteItem("#FDD835", "Amarelo Escuro"),
    ColorPaletteItem("#F9A825", "Amarelo Ouro"),
    ColorPaletteItem("#F57F17", "Amarelo Mostarda"),
    
    // ========== LARANJA ==========
    ColorPaletteItem("#FFF3E0", "Laranja Claro"),
    ColorPaletteItem("#FFE0B2", "Laranja Pastel"),
    ColorPaletteItem("#FFCC80", "Laranja Médio"),
    ColorPaletteItem("#FFB74D", "Laranja Vibrante"),
    ColorPaletteItem("#FFA726", "Laranja"),
    ColorPaletteItem("#FF9800", "Laranja Vivo"),
    ColorPaletteItem("#FB8C00", "Laranja Escuro"),
    ColorPaletteItem("#F57C00", "Laranja Profundo"),
    ColorPaletteItem("#E65100", "Laranja Queimado"),
    
    // ========== MARROM / TERRA ==========
    ColorPaletteItem("#EFEBE9", "Marrom Claro"),
    ColorPaletteItem("#D7CCC8", "Marrom Pastel"),
    ColorPaletteItem("#BCAAA4", "Marrom Médio"),
    ColorPaletteItem("#A1887F", "Marrom"),
    ColorPaletteItem("#8D6E63", "Marrom Vibrante"),
    ColorPaletteItem("#795548", "Marrom Escuro"),
    ColorPaletteItem("#6D4C41", "Marrom Profundo"),
    ColorPaletteItem("#5D4037", "Marrom Café"),
    ColorPaletteItem("#3E2723", "Marrom Intenso"),
    
    // ========== CINZA ==========
    ColorPaletteItem("#FAFAFA", "Branco"),
    ColorPaletteItem("#F5F5F5", "Cinza Claro"),
    ColorPaletteItem("#EEEEEE", "Cinza Pastel"),
    ColorPaletteItem("#E0E0E0", "Cinza Médio"),
    ColorPaletteItem("#BDBDBD", "Cinza"),
    ColorPaletteItem("#9E9E9E", "Cinza Escuro"),
    ColorPaletteItem("#757575", "Cinza Profundo"),
    ColorPaletteItem("#616161", "Cinza Intenso"),
    ColorPaletteItem("#424242", "Cinza Grafite"),
    ColorPaletteItem("#212121", "Preto"),
    ColorPaletteItem("#000000", "Preto Absoluto"),
    
    // ========== CORES ESPECIAIS ==========
    ColorPaletteItem("#1A1A2E", "Azul Noturno"),
    ColorPaletteItem("#16213E", "Azul Profundo"),
    ColorPaletteItem("#0F3460", "Azul Marinho"),
    ColorPaletteItem("#533483", "Roxo Profundo"),
    ColorPaletteItem("#E94560", "Vermelho Coral"),
    ColorPaletteItem("#F5A623", "Dourado"),
    ColorPaletteItem("#4A90D9", "Azul Royal"),
    ColorPaletteItem("#50E3C2", "Turquesa"),
    ColorPaletteItem("#B8E986", "Verde Maçã"),
    ColorPaletteItem("#F8E71C", "Amarelo Sol"),
    ColorPaletteItem("#FF6B6B", "Salmão"),
    ColorPaletteItem("#FF9F43", "Pêssego"),
    ColorPaletteItem("#F368E0", "Magenta"),
    ColorPaletteItem("#0ABDE3", "Azul Elétrico"),
    ColorPaletteItem("#10AC84", "Verde Esmeralda"),
    ColorPaletteItem("#EE5A24", "Vermelho Tijolo"),
    ColorPaletteItem("#2C3E50", "Azul Aço"),
    ColorPaletteItem("#A29BFE", "Lavanda"),
    ColorPaletteItem("#FD79A8", "Rosa Coral"),
    ColorPaletteItem("#00CEC9", "Ciano Turquesa")
)