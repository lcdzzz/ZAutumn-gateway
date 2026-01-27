package indi.lcdzzz.gateway.utils;

import org.springframework.beans.factory.annotation.Value;


public class Applications {
    private static final InnerUtil INNER_UTIL = new InnerUtil();

    public Applications() {
    }

    public static String getName() {
        return INNER_UTIL.name;
    }

    public static String getGroup() {
        return INNER_UTIL.group;
    }


    private static class InnerUtil {
        @Value("${zautumn.application.name:}")
        public String name;
        @Value("${zautumn.application.group:}")
        public String group;

        private InnerUtil() {
        }

    }
}
