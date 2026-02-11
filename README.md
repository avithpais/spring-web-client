# spring-web-client

A reusable Spring Boot library that wraps Spring `WebClient` (reactive) and `RestClient` (synchronous) with production-ready defaults: connection pooling, mutual TLS, automatic retry with exponential backoff, retriable/non-retriable exception classification, per-request filter/interceptor selection, per-request timeout/retry overrides, and request correlation/logging.

Both clients share the same underlying Reactor Netty `HttpClient`, so SSL configuration and connection pooling are configured once and used by both.

## Requirements

- Java 21+
- Spring Boot 4.0+

## Installation

Build the library JAR:

```bash
mvn clean install
```

Add the dependency to your application's `pom.xml`:

```xml
<dependency>
    <groupId>com.webclient</groupId>
    <artifactId>spring-web-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

The library uses Spring Boot auto-configuration. Adding it to the classpath is enough — all beans are registered automatically.

## Quick Start (WebClient - Reactive)

Inject `WebServiceClient` and the filters you need, then build a `WebServiceRequest`:

```java
@Service
public class MyService {

    @Autowired private WebServiceClient webServiceClient;
    @Autowired private CorrelationIdFilterFunction correlationIdFilter;
    @Autowired private BearerTokenFilterFunction bearerTokenFilter;

    public Mono<MyResponse> callDownstream() {
        WebServiceRequest<MyResponse> request = WebServiceRequest.<MyResponse>builder()
                .url("https://api.example.com/data")
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .responseType(MyResponse.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .build();

        return serviceClient.execute(request);
    }
}
```

## Quick Start (RestClient - Synchronous)

For traditional blocking calls, inject `RestServiceClient` and the interceptors you need:

```java
@Service
public class MyBlockingService {

    @Autowired private RestServiceClient restServiceClient;
    @Autowired private CorrelationIdInterceptor correlationIdInterceptor;
    @Autowired private BearerTokenInterceptor bearerTokenInterceptor;

    public MyResponse callDownstream() {
        RestServiceRequest<MyResponse> request = RestServiceRequest.<MyResponse>builder()
                .url("https://api.example.com/data")
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .responseType(MyResponse.class)
                .interceptor(correlationIdInterceptor)
                .interceptor(bearerTokenInterceptor)
                .build();

        return restServiceClient.execute(request);  // Returns T directly, not Mono<T>
    }
}
```

## Choosing WebClient vs RestClient

| Use Case | Recommended Client |
|----------|-------------------|
| WebFlux applications | `WebServiceClient` |
| High-throughput async I/O | `WebServiceClient` |
| API aggregation with `Mono.zip()` | `WebServiceClient` |
| Traditional Spring MVC / Servlet apps | `RestServiceClient` |
| Simple synchronous calls | `RestServiceClient` |
| Easier debugging (stack traces) | `RestServiceClient` |

Both clients share the same connection pool, SSL configuration, and retry exception classification.

## Using Raw WebClient / RestClient Directly

If you don't need the library's per-request retry, timeout management, or the request builder pattern, you can inject the raw `WebClient` or `RestClient` beans directly. They share the same Netty `HttpClient`, connection pool, and TLS context as the wrapper clients.

### Raw RestClient (Synchronous)

```java
@Service
public class MyService {

    @Autowired private RestClient restClient;

    public Post getPost(long id) {
        return restClient.get()
                .uri("https://api.example.com/posts/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Post.class);
    }

    public Post createPost(Post post) {
        return restClient.post()
                .uri("https://api.example.com/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(post)
                .retrieve()
                .body(Post.class);
    }
}
```

### Raw WebClient (Reactive)

```java
@Service
public class MyReactiveService {

    @Autowired private WebClient webClient;

    public Mono<Post> getPost(long id) {
        return webClient.get()
                .uri("https://api.example.com/posts/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Post.class);
    }
}
```

### Adding Interceptors / Filters to Raw Clients

Use `.mutate()` to add library interceptors without modifying the shared bean:

```java
@Autowired private RestClient restClient;
@Autowired private CorrelationIdInterceptor correlationIdInterceptor;
@Autowired private BearerTokenInterceptor bearerTokenInterceptor;

public Post getAuthenticatedPost(long id) {
    RestClient authenticatedClient = restClient.mutate()
            .requestInterceptor(correlationIdInterceptor)
            .requestInterceptor(bearerTokenInterceptor)
            .build();

    return authenticatedClient.get()
            .uri("https://api.example.com/posts/{id}", id)
            .retrieve()
            .body(Post.class);
}
```

### What You Keep vs. Lose

| Feature | Raw beans | Via WebServiceRequest / RestServiceRequest |
|---|---|---|
| TLS / SSL | Yes | Yes |
| Connection pool | Yes | Yes |
| Connect timeout | Yes | Yes |
| Response timeout | Yes | Yes |
| Per-request filters / interceptors | Manual (`.mutate()`) | Built-in via `.filter()` / `.interceptor()` |
| Per-request retry with backoff | Manual (`.retryWhen()` / custom loop) | Built-in via `.maxRetries()` |
| Per-request timeout override | Manual (`.timeout()` / request factory) | Built-in via `.timeoutMs()` |
| Retriable exception classification | Not included | Built-in (5xx/429/IOException) |

## Configuration

All properties use the prefix `webclient.http` in `application.properties` or `application.yml`.

### Connection Pool

| Property | Default | Description |
|---|---|---|
| `webclient.http.pool.max-connections` | `500` | Maximum total connections in the pool |
| `webclient.http.pool.max-connections-per-route` | `50` | Maximum connections per host |

### Timeouts

| Property | Default | Description |
|---|---|---|
| `webclient.http.timeout.connect-request-timeout-ms` | `5000` | TCP connect timeout (ms) |
| `webclient.http.timeout.timeout-ms` | `30000` | Response timeout (ms) — global default |

### Retry

| Property | Default | Description |
|---|---|---|
| `webclient.http.retry.max-retries` | `3` | Maximum retry attempts (exponential backoff) |
| `webclient.http.retry.retry-interval-ms` | `1000` | Initial backoff interval (ms) |

Retry automatically classifies exceptions:
- **Retriable** (will be retried): HTTP 5xx, HTTP 429, `WebClientRequestException`, `IOException`, `ConnectException`
- **Non-retriable** (fail immediately): HTTP 4xx (except 429), `RuntimeException`, `IllegalArgumentException`, etc.

Each retry attempt is logged at WARN level with the attempt number, max retries, exception type, and message.

### SSL / mTLS

| Property | Default | Description |
|---|---|---|
| `webclient.http.ssl.enabled` | `false` | Enable SSL/TLS |
| `webclient.http.ssl.keystore-path` | | Path to PKCS12 keystore |
| `webclient.http.ssl.keystore-password` | | Keystore password |
| `webclient.http.ssl.keystore-type` | `PKCS12` | Keystore type |
| `webclient.http.ssl.truststore-path` | | Path to PKCS12 truststore |
| `webclient.http.ssl.truststore-password` | | Truststore password |
| `webclient.http.ssl.truststore-type` | `PKCS12` | Truststore type |
| `webclient.http.ssl.key-alias-name` | | Alias to select from keystore |
| `webclient.http.ssl.bypass-verification` | `false` | Skip certificate verification (non-production only) |
| `webclient.http.ssl.tls-protocol` | `TLSv1.2` | TLS protocol version |

## Per-Request Filter Selection

Filters are **not** auto-registered on the shared `WebClient`. Instead, each `WebServiceRequest` declares which filters it needs via the builder's `filter()` method. This gives callers full control — for example, bearer-token injection may only be needed for certain downstream services.

The library auto-configures three `ExchangeFilterFunction` beans as injectable Spring beans:

| Order | Bean | Description |
|---|---|---|
| 100 | `CorrelationIdFilterFunction` | Adds `X-Correlation-Id` UUID header if not already present |
| 200 | `BearerTokenFilterFunction` | Injects `Authorization: Bearer <token>` from a `BearerTokenProvider` bean |
| 300 | `RequestLoggingFilterFunction` | Logs request/response at DEBUG level with method, URL, status, and elapsed time |

All three are `@ConditionalOnMissingBean` — you can replace any by registering your own bean of the same type. You can also create additional custom `ExchangeFilterFunction` beans.

### Usage

```java
@Autowired private BearerTokenFilterFunction bearerTokenFilter;
@Autowired private CorrelationIdFilterFunction correlationIdFilter;
@Autowired private RequestLoggingFilterFunction loggingFilter;

// Authenticated call — all three filters
WebServiceRequest.<Post>builder()
        .url("https://internal-api/posts/1")
        .responseType(Post.class)
        .filter(correlationIdFilter)
        .filter(bearerTokenFilter)
        .filter(loggingFilter)
        .build();

// Public call — no bearer token needed
WebServiceRequest.<String>builder()
        .url("https://public-api/health")
        .responseType(String.class)
        .filter(correlationIdFilter)
        .filter(loggingFilter)
        .build();

// Minimal call — no filters at all
WebServiceRequest.<String>builder()
        .url("https://simple-endpoint/ping")
        .responseType(String.class)
        .build();
```

When a request has filters, `WebServiceClient` applies them via `webClient.mutate()`, creating a lightweight per-request variant. When no filters are specified, the base `WebClient` is used directly with zero overhead.

## Per-Request Interceptor Selection (RestClient)

Similar to WebClient filters, RestClient interceptors are **not** auto-registered. Each `RestServiceRequest` declares which interceptors it needs via the builder's `interceptor()` method.

The library auto-configures three `ClientHttpRequestInterceptor` beans:

| Order | Bean | Description |
|---|---|---|
| 100 | `CorrelationIdInterceptor` | Adds `X-Correlation-Id` UUID header if not already present |
| 200 | `BearerTokenInterceptor` | Injects `Authorization: Bearer <token>` from a `BearerTokenProvider` bean |
| 300 | `RequestLoggingInterceptor` | Logs request/response at DEBUG level with method, URL, status, and elapsed time |

### Usage

```java
@Autowired private BearerTokenInterceptor bearerTokenInterceptor;
@Autowired private CorrelationIdInterceptor correlationIdInterceptor;
@Autowired private RequestLoggingInterceptor loggingInterceptor;

// Authenticated call — all three interceptors
RestServiceRequest.<Post>builder()
        .url("https://internal-api/posts/1")
        .responseType(Post.class)
        .interceptor(correlationIdInterceptor)
        .interceptor(bearerTokenInterceptor)
        .interceptor(loggingInterceptor)
        .build();

// Public call — no bearer token needed
RestServiceRequest.<String>builder()
        .url("https://public-api/health")
        .responseType(String.class)
        .interceptor(correlationIdInterceptor)
        .interceptor(loggingInterceptor)
        .build();
```

When a request has interceptors, `RestServiceClient` applies them via `restClient.mutate()`.

## Per-Request Timeout and Retry Overrides

Global timeout/retry settings serve as defaults. Individual requests can override them:

```java
WebServiceRequest<MyResponse> request = WebServiceRequest.<MyResponse>builder()
        .url("https://slow-service.example.com/heavy")
        .method(HttpMethod.GET)
        .responseType(MyResponse.class)
        .timeoutMs(60000)        // 60s total deadline (global default: 30s)
        .maxRetries(5)           // 5 retries (global default: 3)
        .retryIntervalMs(2000)   // 2s initial backoff (global default: 1s)
        .build();
```

| Field | Type | Default | Behavior |
|---|---|---|---|
| `timeoutMs` | `Integer` | `null` (use global) | Total deadline for the entire operation including retries |
| `maxRetries` | `Integer` | `null` (use global) | Max retry attempts. `0` = no retry at all |
| `retryIntervalMs` | `Long` | `null` (use global) | Initial backoff interval for exponential retry |

Setting `maxRetries(0)` skips the retry operator entirely — useful for non-idempotent calls or when the caller handles retries externally.

The timeout is applied **after** the retry operator, so it acts as a total deadline for the entire operation (all attempts combined), not per-attempt.

### Override Combinations

| `maxRetries` | `retryIntervalMs` | Behavior |
|---|---|---|
| `null` | `null` | Global retry spec used |
| `null` | set | Global `maxRetries` + per-request interval |
| set (> 0) | `null` | Per-request retries + global interval |
| set (> 0) | set | Fully per-request retry spec |
| `0` | any | No retry — `.retryWhen()` is skipped |

## Bearer Token Injection

The `BearerTokenFilterFunction` injects an `Authorization: Bearer <token>` header when attached to a request. It calls `BearerTokenProvider.getToken()` synchronously — implement thread-safe caching in your provider (e.g., using `StampedLock`).

Register a provider bean in your application:

```java
@Bean
public BearerTokenProvider bearerTokenProvider() {
    return () -> myTokenCache.getOrRefreshToken();
}
```

The filter only runs on requests that include it via `.filter(bearerTokenFilter)`. If the request already contains an `Authorization` header, the filter preserves it. If no `BearerTokenProvider` bean is registered, the filter is a no-op.

## Consuming Mono Responses

When `WebServiceClient.execute()` returns a `Mono<T>`, clients have several options for handling the response:

### Reactive (Recommended for WebFlux)

Return the `Mono` directly from your controller — Spring WebFlux subscribes automatically:

```java
@GetMapping("/{id}")
public Mono<Post> getPost(@PathVariable long id) {
    return postService.getPost(id);  // Returns Mono<Post> from WebServiceClient
}
```

### Transform with Operators

Use `map()` for synchronous transformations:

```java
public Mono<String> getPostTitle(long id) {
    return serviceClient.execute(request)
            .map(Post::getTitle);
}
```

Use `flatMap()` for chaining async operations:

```java
public Mono<Post> getAndUpdate(long id) {
    return serviceClient.execute(getRequest)
            .flatMap(post -> {
                post.setTitle("Updated");
                return serviceClient.execute(updateRequest);
            });
}
```

### Subscribe with Callbacks

For fire-and-forget scenarios or side effects:

```java
serviceClient.execute(request)
    .subscribe(
        result -> log.info("Success: {}", result),
        error -> log.error("Failed", error),
        () -> log.info("Completed")
    );
```

### Blocking (Servlet/MVC Only)

In traditional Spring MVC applications, call `.block()` to wait for the result:

```java
Post post = serviceClient.execute(request).block();
```

**WARNING:** Never use `.block()` inside a reactive pipeline or on Netty event-loop threads — it will throw `IllegalStateException`.

### Error Handling

```java
serviceClient.execute(request)
    .onErrorReturn(defaultPost)                    // Fallback value
    .onErrorResume(e -> fetchFromCache(id))        // Fallback Mono
    .timeout(Duration.ofSeconds(5))                // Timeout
    .retryWhen(Retry.backoff(3, Duration.ofMillis(100)));  // Retry
```

## API Aggregator Pattern

The library is designed for services that fan out to multiple downstream APIs, aggregate responses, and return a single result. Use `Mono.zip()` for parallel calls with independent timeout budgets and filter sets:

```java
public Mono<AggregatedResponse> aggregate(long id) {
    Mono<UserProfile> user = serviceClient.execute(
            WebServiceRequest.<UserProfile>builder()
                    .url("https://user-service/users/" + id)
                    .responseType(UserProfile.class)
                    .filter(correlationIdFilter)
                    .filter(bearerTokenFilter)
                    .timeoutMs(3000)
                    .maxRetries(1)
                    .build());

    Mono<OrderHistory> orders = serviceClient.execute(
            WebServiceRequest.<OrderHistory>builder()
                    .url("https://order-service/orders?userId=" + id)
                    .responseType(OrderHistory.class)
                    .filter(correlationIdFilter)
                    .filter(bearerTokenFilter)
                    .timeoutMs(10000)
                    .maxRetries(3)
                    .retryIntervalMs(500)
                    .build());

    return Mono.zip(user, orders)
            .map(tuple -> new AggregatedResponse(tuple.getT1(), tuple.getT2()));
}
```

For chained calls where one response feeds the next:

```java
public Mono<EnrichedOrder> getEnrichedOrder(long orderId) {
    return serviceClient.execute(
            WebServiceRequest.<Order>builder()
                    .url("https://order-service/orders/" + orderId)
                    .responseType(Order.class)
                    .filter(correlationIdFilter)
                    .filter(bearerTokenFilter)
                    .timeoutMs(5000)
                    .build())
            .flatMap(order -> serviceClient.execute(
                    WebServiceRequest.<Product>builder()
                            .url("https://product-service/products/" + order.getProductId())
                            .responseType(Product.class)
                            .filter(correlationIdFilter)
                            .timeoutMs(3000)
                            .maxRetries(2)
                            .build())
                    .map(product -> new EnrichedOrder(order, product)));
}
```

## Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Your Application                                                         │
│                                                                          │
│  REACTIVE                              SYNCHRONOUS                        │
│  ────────                              ───────────                        │
│  @Autowired WebServiceClient           @Autowired RestServiceClient       │
│  WebServiceRequest.builder()           RestServiceRequest.builder()       │
│      .filter(correlationIdFilter)          .interceptor(correlationIdInt) │
│      .filter(bearerTokenFilter)            .interceptor(bearerTokenInt)   │
│  webServiceClient.execute() → Mono<T>  restServiceClient.execute() → T    │
└─────────────────────────┬────────────────────────────┬───────────────────┘
                          │                            │
┌─────────────────────────▼────────────────────────────▼───────────────────┐
│  spring-web-client (auto-configured)                                      │
│                                                                          │
│  WebServiceClient                      RestServiceClient                  │
│    ├── resolveWebClient()              ├── resolveRestClient()           │
│    ├── buildRequestSpec()              ├── buildRequestSpec()            │
│    ├── applyRetry() (reactive)         └── executeWithRetry() (sync)     │
│    └── applyTimeout()                                                    │
│                                                                          │
│  WebClient filters:                    RestClient interceptors:           │
│    ├── CorrelationIdFilterFunction     ├── CorrelationIdInterceptor      │
│    ├── BearerTokenFilterFunction       ├── BearerTokenInterceptor        │
│    └── RequestLoggingFilterFunction    └── RequestLoggingInterceptor     │
│                                                                          │
│  Shared Infrastructure:                                                   │
│    ├── reactor.netty.http.client.HttpClient (connection pool, SSL)       │
│    ├── SslConnectionFactoryInitializer (mTLS, truststore, alias)         │
│    ├── HttpClientProperties (webclient.http.* configuration)             │
│    └── BearerTokenProvider (token retrieval interface)                   │
│                                                                          │
│  WebClient ←──────────────────────────────→ RestClient                   │
│  (ReactorClientHttpConnector)              (ReactorClientHttpRequestFactory)
│         │                                           │                    │
│         └──────────── SHARED HttpClient ────────────┘                    │
└──────────────────────────────────────────────────────────────────────────┘
```

## How It All Comes Together

This section traces the complete path from configuration to an HTTP call, layer by layer.

### Layer 1: Configuration — `HttpClientProperties`

Everything starts with `application.properties` under the `webclient.http.*` prefix:

```properties
webclient.http.ssl.enabled=true
webclient.http.ssl.keystore-path=/path/to/keystore.p12
webclient.http.ssl.keystore-password=secret
webclient.http.ssl.truststore-path=/path/to/truststore.p12
webclient.http.ssl.truststore-password=secret
webclient.http.ssl.tls-protocol=TLSv1.2

webclient.http.pool.max-connections=200
webclient.http.timeout.connect-request-timeout-ms=5000
webclient.http.timeout.timeout-ms=10000

webclient.http.retry.max-retries=3
webclient.http.retry.retry-interval-ms=500
```

Spring Boot binds these into `HttpClientProperties` — a POJO with nested `Ssl`, `Pool`, `Timeout`, and `Retry` inner classes. Every downstream bean reads from this single source of truth.

### Layer 2: SSL/TLS — `SslConnectionFactoryInitializer`

This bean takes the `Ssl` properties and builds a Netty `SslContext`:

```
HttpClientProperties.Ssl
        |
        v
+----------------------------------+
| SslConnectionFactoryInitializer  |
|                                  |
|  ssl.enabled == false?           |---> return null (no TLS)
|                                  |
|  ssl.bypassVerification?        |---> InsecureTrustManagerFactory (dev only)
|                                  |
|  Otherwise:                      |
|   1. Load keystore -> KMF        |
|   2. Load truststore -> TMF      |
|   3. If keyAliasName set,        |
|      wrap in AliasSelectingKM    |
|   4. Build JDK SSLContext        |
|   5. Wrap in Netty JdkSslContext |
+----------------------------------+
        |
        v
   SslContext (or null)
```

The `AliasSelectingX509KeyManager` wrapper handles mTLS — when a keystore has multiple client certificates, it forces a specific alias to be used.

### Layer 3: Connection Pool + Netty HttpClient — `WebClientAutoConfiguration`

Two beans create the shared infrastructure:

**`webClientConnectionProvider`** (Reactor Netty `ConnectionProvider`):
```java
ConnectionProvider.builder("webclient-pool")
    .maxConnections(200)              // from pool.maxConnections
    .pendingAcquireTimeout(5000ms)    // from timeout.connectRequestTimeoutMs
    .maxIdleTime(60s)                 // hardcoded
    .evictInBackground(120s)          // hardcoded
    .build();
```

**`webClientHttpClient`** (Reactor Netty `HttpClient`):
```java
HttpClient.create(connectionProvider)           // uses the pool above
    .option(CONNECT_TIMEOUT_MILLIS, 5000)       // TCP connect timeout
    .responseTimeout(10000ms)                   // overall response timeout
    .secure(spec -> spec.sslContext(sslContext)) // TLS from Layer 2 (if non-null)
```

This single `HttpClient` is the foundation for everything. It owns the connection pool, the TLS handshake, and the base timeouts.

### Layer 4: `RestClient` and `WebClient` Beans

Both are created from the same Netty `HttpClient`:

```java
// RestClient — synchronous
ReactorClientHttpRequestFactory requestFactory =
    new ReactorClientHttpRequestFactory(webClientHttpClient);  // <-- same Netty HttpClient
RestClient.builder()
    .requestFactory(requestFactory)
    .build();

// WebClient — reactive
WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(webClientHttpClient))  // <-- same one
    .build();
```

Both beans are created **bare — no filters, no interceptors**. This is intentional. The library doesn't force any cross-cutting concerns globally.

**This is the bean you inject when using the raw approach:**
```java
@Autowired
private RestClient restClient;  // gets TLS + pool + timeouts for free
```

### Layer 5a: Interceptors + Token Provider (optional)

Three interceptor beans are registered but **not attached** to the RestClient:

| Order | Interceptor | What it does |
|---|---|---|
| 100 | `CorrelationIdInterceptor` | Adds `X-Correlation-Id: <UUID>` if not already present |
| 200 | `BearerTokenInterceptor` | Calls `BearerTokenProvider.getToken()` and sets `Authorization: Bearer <token>` |
| 300 | `RequestLoggingInterceptor` | DEBUG-level logging of request/response with timing |

The `BearerTokenInterceptor` depends on `BearerTokenProvider` (`@Autowired(required = false)`). Applications implement this interface to provide token caching and refresh logic. A typical implementation uses `StampedLock` three-tier locking:

```
BearerTokenInterceptor
        |
        | calls getToken()
        v
YourTokenService (implements BearerTokenProvider)
        |
        | StampedLock 3-tier locking:
        |   1. Optimistic read -> check cache -> return if valid
        |   2. Read lock -> recheck cache -> return if valid
        |   3. Promote to write lock -> fetch new token -> update cache
        |
        v
   "Bearer eyJhbG..."
```

On the fast path (token valid), it's a single lock-free optimistic read — essentially a volatile read. Only when the token expires does a write lock get acquired, and only one thread fetches while others wait.

The token fetcher should use JDK `HttpClient` (not this library) to avoid a circular dependency — the library needs a token to make calls, so the token fetcher can't use the library.

### Layer 5b: `RestServiceClient` + `RestServiceRequest` (the wrapper layer)

When you use the library's wrapper, the flow is:

```
RestServiceRequest.builder()
    .url("https://api.example.com/posts/1")
    .method(GET)
    .responseType(Post.class)
    .interceptor(correlationIdInterceptor)    // opt-in per request
    .interceptor(bearerTokenInterceptor)      // opt-in per request
    .maxRetries(5)                            // override global default
    .timeoutMs(5000)                          // override global default
    .build()
        |
        v
RestServiceClient.execute(request)
        |
        +---> resolveRestClient(request)
        |       restClient.mutate()
        |         .requestInterceptor(correlationId)    // applied here
        |         .requestInterceptor(bearerToken)      // applied here
        |         .build()                              // new RestClient instance
        |                                               // (shares same HttpClient/pool)
        |
        +---> buildRequestSpec(effectiveClient, request)
        |       client.method(GET).uri(...).headers(...).accept(...).body(...)
        |
        +---> executeWithRetry(operation, request)
                SyncRetryExecutor:
                  for attempt 0..maxRetries:
                    try: return operation.get()
                    catch: if retriable (5xx/429/IOException) -> sleep(interval * 2^attempt)
                           if non-retriable (4xx/RuntimeException) -> throw immediately
```

The `.mutate()` call creates a **new RestClient instance** that inherits the same underlying `HttpClient` (same pool, same TLS) but adds the request-specific interceptors. The base `RestClient` bean is never modified.

### Layer 5c: Raw `RestClient` Usage (no wrapper)

When you skip the wrapper and use the raw `RestClient`:

```java
@Autowired
private RestClient restClient;   // from Layer 4

public Post getPost(long id) {
    return restClient.get()
        .uri("https://api.example.com/posts/{id}", id)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(Post.class);
}
```

You keep everything from Layers 1-4 (TLS, connection pool, timeouts). You manage interceptors, retries, and per-request timeouts yourself if needed.

### Full Dependency Graph

```
application.properties (webclient.http.*)
        |
        v
  HttpClientProperties
        |
   +----+----------------------+
   v                           v
SslConnectionFactory    ConnectionProvider
Initializer              (pool config)
   |                           |
   v                           v
SslContext ---------> Netty HttpClient <--- timeouts
                          |
                +---------+---------+
                v                   v
           WebClient           RestClient           <-- Layer 4 (raw beans, no filters)
                |                   |
                v                   v
         WebServiceClient    RestServiceClient      <-- Layer 5 (wrapper, adds retry +
                |                   |                    per-request interceptors via .mutate())
                v                   v
         ExchangeFilter      Interceptor beans      <-- opt-in per request
         Function beans       (CorrelationId,
         (CorrelationId,       BearerToken,
          BearerToken,         RequestLogging)
          RequestLogging)
                                    |
                                    v
                            BearerTokenProvider
                            (your implementation)
                                    |
                                    v
                             Token cache
                            (StampedLock 3-tier)
```

## Testing

```bash
mvn clean test
```

127 tests covering:
- `WebServiceRequest` and `RestServiceRequest` builder validation, immutability, and per-request filter/interceptor list
- `RetryStrategyFactory` and `SyncRetryExecutor` retry counting with global and explicit parameters
- `RetriableExceptionPredicate` and `SyncRetriableExceptionPredicate` classification of retriable vs non-retriable exceptions
- `WebServiceClient` and `RestServiceClient` request execution, retry behavior, per-request timeout/retry overrides
- `BearerTokenFilterFunction` and `BearerTokenInterceptor` token injection and skip-when-present logic
- `CorrelationIdFilterFunction` and `CorrelationIdInterceptor` header injection and preservation
- `RequestLoggingFilterFunction` and `RequestLoggingInterceptor` passthrough and error propagation
- `HttpClientProperties` defaults and binding
- `AliasSelectingX509KeyManager` and `SslConnectionFactoryInitializer` SSL configuration

## Project Structure

```
src/main/java/com/webclient/lib/
├── auth/
│   ├── BearerTokenFilterFunction.java      # @Order(200) WebClient filter for token injection
│   ├── BearerTokenInterceptor.java         # @Order(200) RestClient interceptor for token injection
│   └── BearerTokenProvider.java            # Functional interface for token retrieval
├── client/
│   ├── WebServiceClient.java               # Reactive HTTP client (WebClient-based)
│   └── RestServiceClient.java              # Synchronous HTTP client (RestClient-based)
├── config/
│   ├── HttpClientProperties.java           # @ConfigurationProperties binding
│   ├── WebClientAutoConfiguration.java     # Auto-config for WebClient + shared HttpClient + RestClient
│   └── RestClientAutoConfiguration.java    # Auto-config for RestClient interceptors
├── filter/
│   ├── CorrelationIdFilterFunction.java    # @Order(100) WebClient filter for X-Correlation-Id
│   └── RequestLoggingFilterFunction.java   # @Order(300) WebClient filter for DEBUG logging
├── interceptor/
│   ├── CorrelationIdInterceptor.java       # @Order(100) RestClient interceptor for X-Correlation-Id
│   └── RequestLoggingInterceptor.java      # @Order(300) RestClient interceptor for DEBUG logging
├── model/
│   ├── WebServiceRequest.java              # Immutable request spec for WebClient (filters)
│   └── RestServiceRequest.java             # Immutable request spec for RestClient (interceptors)
├── retry/
│   ├── RetriableExceptionPredicate.java    # Classifies retriable exceptions (WebClient)
│   ├── RetryStrategyFactory.java           # Creates Reactor Retry specs with logging
│   ├── SyncRetriableExceptionPredicate.java # Classifies retriable exceptions (RestClient)
│   └── SyncRetryExecutor.java              # Synchronous retry with exponential backoff
└── ssl/
    ├── AliasSelectingX509KeyManager.java   # Selects a specific key alias from keystore
    └── SslConnectionFactoryInitializer.java # Builds Netty SslContext
```

## License

Internal library — see your organization's licensing policy.
