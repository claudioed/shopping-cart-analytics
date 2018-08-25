package com.sensedia.shopping.cart.analytics.domain

import java.math.BigDecimal
import java.util.*

/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart
 */
data class Item(val number:String, val product: Product, val price:BigDecimal)

data class ShoppingCart(val id:String, val user: User, val shippingAddress: Address, val billingAddress: Address, val items:List<Item>){

    fun productAnalytics():List<ProductAnalytics>{
        return this.items.map {
            ProductAnalytics(id = UUID.randomUUID().toString(), product = it.product, cartIdentifier = this.id, cartAddress = this.shippingAddress)
        }
    }

}

data class User(val id: String,val email:String)

data class Address(val title:String,val street:String,val city:String,val zipCode:String,val country:String)

data class Product(val id:String,val name:String,val description:String)

data class ProductAnalytics(val id:String, val product: Product, val cartIdentifier:String, val cartAddress: Address)