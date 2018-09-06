package com.sensedia.shopping.cart.analytics.infra

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.sensedia.shopping.cart.analytics.domain.ShoppingCart
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.HealthChecks
import io.vertx.ext.healthchecks.Status
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.kotlin.core.eventbus.DeliveryOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import java.util.*


/**
 * @author claudioed on 06/08/18.
 * Project shopping-cart
 */
class MainVerticle : AbstractVerticle() {

    private val LOGGER = LoggerFactory.getLogger(MainVerticle::class.java)

    override fun start(startFuture: Future<Void>) {
        Json.mapper.registerModule(KotlinModule())
        vertx.deployVerticle(StoreShoppingCartAnalyticsVerticle())
        val router = router()
        vertx.createHttpServer()
                .requestHandler { router.accept(it) }
                .listen(config().getInteger("http.port", 8081)) { result ->
                    if (result.succeeded()) {
                        startFuture.complete()
                    } else {
                        startFuture.fail(result.cause())
                    }
                }
    }

    private fun router() = Router.router(vertx).apply {
        val healthCheckHandler = HealthCheckHandler.createWithHealthChecks(HealthChecks.create(vertx))
        val host = System.getenv("MONGO_HOST") ?: "127.0.0.1"
        val database = System.getenv("MONGO_DB") ?: "shopping-cart-analytics"
        val config = json {
            obj(
                    "connection_string" to host,
                    "db_name" to database
            )
        }
        val mongoClient = MongoClient.createShared(vertx, config, "ShoppingCartAnalyticsData")
        healthCheckHandler.register("mongodb"
        ) { future ->
            mongoClient.getCollections {
                if(it.failed()){
                    LOGGER.error(it.cause().message)
                    future.fail("database connection failed")
                }else{
                    future.complete(Status.OK())
                }
            }
        }
        get("/analytics/healthcheck").handler(healthCheckHandler)
        route().handler(BodyHandler.create())
        route().handler(LoggerHandler.create())
        post("/analytics").handler(registerAnalytics)
    }

    private val registerAnalytics = Handler<RoutingContext> { req ->
        val cart = Json.decodeValue(req.bodyAsString, ShoppingCart::class.java)
        val shoppingCart = cart.copy(id = UUID.randomUUID().toString())
        vertx.eventBus().publish("shopping.cart.analytics.new", Json.encode(shoppingCart),DeliveryOptions(headers = traceHeaders(req.request())))
        req.response().accepted().end()
    }

    private fun traceHeaders(req: HttpServerRequest): Map<String, String> {
        if(req.headers().contains("x-request-id")){
            LOGGER.info("OpenTracing headers are fully configured")
            return listOf("x-request-id", "x-b3-traceid", "x-b3-spanid", "x-b3-parentspanid", "x-b3-sampled","x-b3-flags", "x-ot-span-context")
                    .filter { req.getHeader(it) != null }
                    .map { it to req.getHeader(it)}.toMap()
        }
        return mapOf()
    }

    private fun HttpServerResponse.accepted():HttpServerResponse {
        this.statusCode = 202
        return this
    }

}