package restaurant

import de.unisaarland.cs.se.selab.food.IngredientPackage
import de.unisaarland.cs.se.selab.food.Stock
import de.unisaarland.cs.se.selab.food.Supplier

class Pantry(stock: Stock) {
    private val inventory: MutableList<IngredientPackage> = mutableListOf()
    private val supplier: Supplier = Supplier(stock)
}
