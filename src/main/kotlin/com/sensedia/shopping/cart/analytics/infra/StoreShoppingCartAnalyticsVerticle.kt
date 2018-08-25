package com.sensedia.shopping.cart.analytics.infra

import com.sensedia.shopping.cart.analytics.domain.ShoppingCart
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.mongo.MongoClient
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart-analytics
 */
class StoreShoppingCartAnalyticsVerticle : AbstractVerticle() {

    private val LOGGER = LoggerFactory.getLogger(StoreShoppingCartAnalyticsVerticle::class.java)

    override fun start(startFuture: Future<Void>) {
        val host = System.getenv("MONGO_HOST") ?: "127.0.0.1"
        val database = System.getenv("MONGO_DB") ?: "shopping-cart-analytics"
        val config = json {
            obj(
                    "connection_string" to host,
                    "db_name" to database
            )
        }
        val mongoClient = MongoClient.createShared(vertx, config, "ShoppingCartAnalyticsData")
        val consumer = vertx.eventBus().consumer<String>("shopping.cart.analytics.new")
        consumer.handler { it ->
            val message = it
            val cart = Json.decodeValue(it.body(), ShoppingCart::class.java)
            cart.productAnalytics().forEach {
                mongoClient.insert("product-analytics", JsonObject(Json.encode(it))) { handler ->
                    if (handler.failed()) {
                        LOGGER.error("Error to persist product analytics")
                    }else{
                        LOGGER.info("Products analytics saved successfully")
                    }
                }
            }
        }
    }

}