package indi.lcdzzz.gateway.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class Applications {
    private static String name;
    private static String group;

    @Value("${zautumn.application.name:}")
    private String instanceName;

    @Value("${zautumn.application.group:}")
    private String instanceGroup;

    @PostConstruct
    public void init() {
        name = instanceName;
        group = instanceGroup;
    }

    public static String getName() {
        return name;
    }

    public static String getGroup() {
        return group;
    }
}
