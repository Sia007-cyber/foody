package com.foody.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(0)
class DemoAccountBootstrapRunner implements ApplicationRunner {
    private final DemoAccountBootstrapService bootstrap;
    DemoAccountBootstrapRunner(DemoAccountBootstrapService bootstrap) { this.bootstrap = bootstrap; }
    @Override public void run(ApplicationArguments args) { bootstrap.apply(); }
}
