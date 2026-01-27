package indi.lcdzzz.zdclTest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MonoFluxDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Mono和Flux使用演示 ===\n");

        // 1. Mono基本操作演示
        demonstrateMonoBasics();

        // 2. Flux基本操作演示
        demonstrateFluxBasics();

        // 3. 在项目中的实际应用场景演示
        demonstrateRealWorldUsage();

//        // 4. 错误处理演示
//        demonstrateErrorHandling();

        Thread.sleep(2000); // 等待异步操作完成
    }

    /**
     * Mono基本操作演示
     * Mono代表0或1个元素的异步序列
     */
    private static void demonstrateMonoBasics() {
        System.out.println("1. Mono基本操作演示:");
        
        // 创建Mono - 从值创建
        Mono<String> monoFromValue = Mono.just("Hello, Mono!");
        monoFromValue.subscribe(value -> System.out.println("  Mono值: " + value));

        // 创建Mono - 空值
        Mono<String> emptyMono = Mono.empty();
        emptyMono.subscribe(
            value -> System.out.println("  这不会被打印"),
            error -> System.out.println("  错误: " + error),
            () -> System.out.println("  Mono为空，完成回调执行")
        );

        // 创建Mono - 从Supplier创建（延迟执行）
        Mono<String> lazyMono = Mono.fromSupplier(() -> "Lazy Hello");
        lazyMono.subscribe(value -> System.out.println("  Lazy Mono值: " + value));

        // map操作 - 转换值
        Mono<Integer> lengthMono = Mono.just("Hello World")
                .map(String::length);
        lengthMono.subscribe(length -> System.out.println("  字符串长度: " + length));

        // flatMap操作 - 链式调用其他Mono
        Mono<String> flatMappedMono = Mono.just("Hello")
                .flatMap(value -> Mono.just(value + " FlatMap"));
        flatMappedMono.subscribe(value -> System.out.println("  FlatMap结果: " + value));

        System.out.println();
    }

    /**
     * Flux基本操作演示
     * Flux代表0到N个元素的异步序列
     */
    private static void demonstrateFluxBasics() {
        System.out.println("2. Flux基本操作演示:");

        // 从集合创建Flux
        List<String> items = Arrays.asList("Apple", "Banana", "Cherry");
        Flux<String> fluxFromList = Flux.fromIterable(items);
        System.out.println("  从列表创建Flux:");
        fluxFromList.subscribe(item -> System.out.println("    " + item));

        // 从数组创建Flux
        Flux<String> fluxFromArray = Flux.just("One", "Two", "Three");
        System.out.println("  从数组创建Flux:");
        fluxFromArray.subscribe(item -> System.out.println("    " + item));

        // map操作 - 转换每个元素
        Flux<Integer> lengthFlux = Flux.just("Hello", "World", "Reactor")
                .map(String::length);
        System.out.println("  长度转换:");
        lengthFlux.subscribe(length -> System.out.println("    长度: " + length));

        // filter操作 - 过滤元素
        Flux<String> filteredFlux = Flux.just("Apple", "Banana", "Cherry", "Date")
                .filter(fruit -> fruit.length() > 5);
        System.out.println("  过滤长度大于5的水果:");
        filteredFlux.subscribe(fruit -> System.out.println("    " + fruit));

        // flatMap操作 - 将每个元素转换为Flux然后合并
        Flux<String> flatMappedFlux = Flux.just("Hello", "World")
                .flatMap(word -> Flux.fromArray(word.split("")));
        System.out.println("  FlatMap拆分单词:");
        flatMappedFlux.subscribe(letter -> System.out.println("    字母: " + letter));

        // take操作 - 只取前几个元素
        Flux<Integer> takeFlux = Flux.range(1, 10).take(3);
        System.out.println("  取前3个数字:");
        takeFlux.subscribe(num -> System.out.println("    数字: " + num));

        System.out.println();
    }

    /**
     * 项目中的实际应用场景演示
     * 模拟项目中Redis操作、API调用等场景
     */
    private static void demonstrateRealWorldUsage() {
        System.out.println("3. 项目实际应用场景演示:");

        // 场景1: 从Redis获取单个配置项（对应项目中的getCurrentVersion方法）
        simulateGetConfigFromRedis();
        
        // 场景2: 从Redis获取多个路由定义（对应项目中的路由列表获取）
        simulateGetRoutesFromRedis();
        
        // 场景3: 并发执行多个异步任务（对应项目中的updateAll方法）
        simulateConcurrentTasks();
    }

    /**
     * 模拟从Redis获取单个配置项
     */
    private static void simulateGetConfigFromRedis() {
        System.out.println("  3.1 模拟从Redis获取配置:");
        
        // 模拟Redis操作返回Mono
        Mono<String> configMono = Mono.fromCallable(() -> {
            // 模拟Redis查询延迟
            Thread.sleep(100);
            return "config_value_123";
        }).subscribeOn(Schedulers.boundedElastic());

        configMono
            .map(config -> "配置值: " + config)
            .subscribe(
                value -> System.out.println("    " + value),
                error -> System.out.println("    错误: " + error.getMessage())
            );
    }

    /**
     * 模拟从Redis获取多个路由定义
     */
    private static void simulateGetRoutesFromRedis() {
        System.out.println("  3.2 模拟从Redis获取多个路由:");
        
        // 模拟从Redis获取路由列表
        Flux<String> routesFlux = Flux.fromIterable(Arrays.asList(
            "route-1: /api/users/* -> http://user-service",
            "route-2: /api/orders/* -> http://order-service",
            "route-3: /api/products/* -> http://product-service"
        )).delayElements(Duration.ofMillis(50)); // 添加延迟模拟网络请求

        routesFlux
            .filter(route -> route.contains("user")) // 过滤包含"user"的路由
            .map(route -> "[处理后] " + route)
            .subscribe(
                route -> System.out.println("    " + route),
                error -> System.out.println("    错误: " + error.getMessage()),
                () -> System.out.println("    所有路由处理完成")
            );
    }

    /**
     * 模拟并发执行多个任务
     */
    private static void simulateConcurrentTasks() {
        System.out.println("  3.3 模拟并发执行任务:");

        // 模拟两个并行的任务：刷新路由和刷新限流规则
        Mono<String> refreshRoutesTask = Mono.fromCallable(() -> {
            Thread.sleep(200); // 模拟刷新路由耗时
            return "路由刷新完成";
        }).subscribeOn(Schedulers.boundedElastic());

        Mono<String> refreshLimitsTask = Mono.fromCallable(() -> {
            Thread.sleep(150); // 模拟刷新限流规则耗时
            return "限流规则刷新完成";
        }).subscribeOn(Schedulers.boundedElastic());

        // 使用and操作符等待两个任务都完成
        refreshRoutesTask
            .and(refreshLimitsTask)
            .then(Mono.fromCallable(() -> "两个任务都已完成"))
            .subscribe(result -> System.out.println("    " + result));
    }

//    /**
//     * 错误处理演示
//     */
//    private static void demonstrateErrorHandling() {
//        System.out.println("\n4. 错误处理演示:");
//
//        // onErrorReturn - 发生错误时返回默认值
//        Mono<String> errorMono = Mono.error(new RuntimeException("模拟错误"))
//                .onErrorReturn("默认值");
//        errorMono.subscribe(
//            value -> System.out.println("  onErrorReturn结果: " + value),
//            error -> System.out.println("  错误被捕获: " + error.getMessage())
//        );
//
//        // onErrorResume - 发生错误时执行备用逻辑
//        Mono<String> resumeMono = Mono.error(new RuntimeException("模拟错误"))
//                .onErrorResume(error -> Mono.just("备用值"));
//        resumeMono.subscribe(
//            value -> System.out.println("  onErrorResume结果: " + value)
//        );
//
//        // doOnError - 错误时执行副作用操作
//        Flux.range(1, 5)
//            .map(i -> {
//                if (i == 3) throw new RuntimeException("第3个元素出错");
//                return i * 2;
//            })
//            .doOnError(error -> System.out.println("  检测到错误: " + error.getMessage()))
//            .onErrorContinue((error, item) -> System.out.println("  继续处理，跳过错误项: " + item))
//            .subscribe(
//                value -> System.out.println("  成功值: " + value),
//                error -> System.out.println("  最终错误: " + error.getMessage())
//            );
//    }
}
