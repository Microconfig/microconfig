package io.microconfig.core.properties;

import io.microconfig.core.environments.Component;
import io.microconfig.core.properties.resolver.EnvComponent;
import io.microconfig.core.properties.resolver.PropertyResolveException;
import io.microconfig.core.properties.resolver.PropertyResolver;
import io.microconfig.core.properties.sources.SpecialSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.core.environments.Component.byNameAndType;
import static io.microconfig.core.environments.Component.byType;
import static io.microconfig.core.properties.Property.asStringMap;
import static io.microconfig.core.properties.Property.property;
import static io.microconfig.testutils.MicronconfigTestFactory.getConfigProvider;
import static io.microconfig.testutils.MicronconfigTestFactory.getPropertyResolver;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigProviderTest {
    private final ConfigProvider provider = getConfigProvider();
    private final PropertyResolver resolver = getPropertyResolver();

    @Test
    void testLoadsProperties() {
        Map<String, Property> props = provider.getProperties(byType("th-client"), "uat");
        assertEquals(15, props.size());
        assertEquals("th-common-value", props.get("th-client.property.common").getValue());
        assertEquals("th-common-value", props.get("th-client.defaultValue").getValue());
        assertEquals("th-uat-value", props.get("th-client.property.from.uat").getValue());
        assertEquals("uat-override", props.get("th-client.property.override").getValue());
        assertEquals("uat-overridev2", props.get("th-client.several-placeholders").getValue());
        assertEquals("uat-overrideuat-overrideuat-override", props.get("th-client.several-placeholders2").getValue());
        assertEquals("110", props.get("th-client.some.int.property1").getValue());
        assertEquals("200", props.get("th-client.spel").getValue());
        assertEquals("172.30.162.3", props.get("th-client.th-server-ip").getValue());
        assertEquals("100.10.20.1", props.get("th-client.th-server-demo-ip").getValue());
    }

    @Test
    void testPlaceholderAsDefaultValue() {
        Map<String, Property> props = provider.getProperties(byType("placeholderAsDefaultValue"), "placeholderAsDefaultValue");
        assertEquals("simple", props.get("k0").getValue());
        assertEquals("v1", props.get("k1").getValue());
        assertEquals("v2", props.get("k2").getValue());
        assertEquals("v1", props.get("k3").getValue());
        assertEquals("v1", props.get("k4").getValue());
        assertEquals("v2", props.get("k5").getValue());
        assertEquals("v1", props.get("k6").getValue());
        assertEquals("v1", props.get("k7").getValue());
        assertEquals("v1", props.get("k8").getValue());

        assertEquals("v1", props.get("k9").getValue());
        assertEquals("v1", props.get("k09").getValue());
        assertEquals("v1", props.get("k09+").getValue());
        assertEquals("v1", props.get("k009").getValue());
        assertEquals("", props.get("k0009").getValue());

        assertEquals("v1", props.get("k66").getValue());
        assertEquals("v1}", props.get("k67").getValue());

        assertEquals("v1", props.get("k010").getValue());
        assertEquals("v1", props.get("k0010").getValue());
        assertEquals("1", props.get("k00010").getValue());
        assertEquals("21", props.get("k20").getValue());
        assertEquals("v1", props.get("k22").getValue());
        assertEquals("v2", props.get("k23").getValue());
        assertEquals("144", props.get("k24").getValue());
        assertEquals("v2", props.get("k25").getValue());
        assertEquals("v2", props.get("k26").getValue());
        assertEquals("21", props.get("k27").getValue());
        assertEquals("v1hello", props.get("k28").getValue());
    }

    @Test
    void testVar() {
        Map<String, Property> props = provider.getProperties(byType("var"), "var");
        assertEquals(1, props.values().stream()
                .filter(p -> !p.isTemp())
                .count());

        assertEquals("3", props.get("c").getValue());
    }

    @Test
    void testIpParam() {
        Map<String, Property> properties = provider.getProperties(byType("ip1"), "uat");
        assertEquals("1.1.1.1", properties.get("ip1.some-ip").getValue());
    }

    @Test
    void testOrderParam() {
        doTestOrder("ip1", 0);
        doTestOrder("ip2", 1);
        doTestOrder("th-client", 3);
    }

    private void doTestOrder(String compName, int order) {
        String orderValue = resolveValue("uat", byType(compName), "order");
        assertEquals(String.valueOf(order), orderValue);
    }

    @Test
    void testComponentReceivesThisIpPropertyFromEnv() {
        assertEquals("172.30.162.3", resolveValue("uat", byType("th-cache-node3"), "ip"));
    }

    @Test
    void testComponentOverridesThisIpPropertyFromEnv() {
        assertEquals("1.1.1.1", resolveValue("uat", byType("ip3"), "ip"));
    }

    @Test
    void testCyclicDetect() {
        assertThrows(PropertyResolveException.class, () -> provider.getProperties(byType("cyclicDetectTest"), "uat"));
    }

    @Test
    void testSimpleInclude() {
        Map<String, Property> properties = new TreeMap<>(provider.getProperties(byType("i1"), "uat"));
        assertEquals(asList("i1.prop", "i2.prop", "i3.prop"), new ArrayList<>(properties.keySet()));
    }

    @Test
    void testIncludeWithEnvChange() {
        Map<String, Property> props = provider.getProperties(byType("ic1"), "dev");
        assertEquals(8, props.size());
        assertEquals("ic1-dev", props.get("ic1.prop").getValue());
        assertEquals("ic2-dev", props.get("ic2.prop").getValue());
        assertEquals("ic3-dev2", props.get("ic3.prop").getValue());
        assertEquals("ic4-dev2", props.get("ic3.placeholder").getValue());
        assertEquals("ic4-dev2", props.get("ic4.prop").getValue());
        assertEquals("4.4.4.4", props.get("ic3.ic4-ip").getValue());
        assertEquals("ic2", props.get("ic3.placeholderToSelf").getValue());
    }

    @Test
    void testPlaceholderToIncludeWithEnvChange() {
        Map<String, Property> props = provider.getProperties(byType("ic5"), "dev");
        assertEquals("ic2", props.get("v").getValue());
    }

    @Test
    void testPortOffset() {
        assertEquals("1001", provider.getProperties(byType("portOffsetTest"), "dev").get("port").getValue());
        assertEquals("1002", provider.getProperties(byType("portOffsetTest"), "dev2").get("port").getValue());
    }

    @Test
    void testPlaceholderOverride() {
        assertEquals("scomp1 20", provider.getProperties(byType("scomp2"), "dev").get("compositeValue").getValue());
        assertEquals("scomp1 2", provider.getProperties(byType("scomp1"), "dev").get("compositeValue").getValue());

        assertEquals("3", provider.getProperties(byType("scomp1"), "dev").get("compositeValue2").getValue());
        assertEquals("21", provider.getProperties(byType("scomp2"), "dev").get("compositeValue2").getValue());
    }

    @Test
    void testThisOverride() {
        assertEquals("2.2.2.2", provider.getProperties(byType("tov1"), "uat").get("value").getValue());
        assertEquals("3.3.3.3", provider.getProperties(byType("tov2"), "uat").get("value").getValue());
    }

    @Test
    void testEnvProp() {
        assertEquals("uat", provider.getProperties(byType("envPropTest"), "uat").get("env.env").getValue());
        assertEquals("dev value", provider.getProperties(byType("envPropTest"), "dev").get("env.value").getValue());
    }

    @Test
    void nestedExpTest() {
        assertEquals("tcp://:5822", provider.getProperties(byType("pts"), "dev").get("test.mq.address").getValue());
        assertEquals("tcp://:5822", provider.getProperties(byType("pts"), "dev").get("test.mq.address2").getValue());
    }

    @Test
    void spelWrap() {
        assertEquals("hello world3", provider.getProperties(byType("spelWrap"), "dev").get("v1").getValue());
    }

    @Test
    void testEnvPropAliases() {
        doTestAliases("node1", "172.30.162.4");
        doTestAliases("node2", "172.30.162.4");
        doTestAliases("node3", "172.30.162.5");
        doTestAliases("node", "172.30.162.5");
    }

    @Test
    void testReferenceEnvWithSimilarName() {
        assertEquals("value-from-dev", provider.getProperties(byType("main"), "dev").get("value").getValue());
    }

    @Test
    void testPlaceholderToAliases() {
        Map<String, Property> properties = provider.getProperties(byType("placeholderToAlias"), "aliases");
        assertEquals("172.30.162.4 172.30.162.5", properties.get("ips").getValue());
        assertEquals("v1 v1", properties.get("properties").getValue());
    }

    @Test
    void testListMerge() {
        Map<String, Property> properties = provider.getProperties(byType("mergeLists"), "some");
        assertEquals("- v1" + LINES_SEPARATOR +
                "- v2" + LINES_SEPARATOR +
                "- v3" + LINES_SEPARATOR +
                "- v4" + LINES_SEPARATOR +
                "- v5" + LINES_SEPARATOR, properties.get("level_1.level_2").getValue());
    }

    @Test
    @Disabled
    void testContextChange() {
        Map<String, Property> properties = provider.getProperties(byType("context2"), "dev");
        assertEquals("prod", properties.get("prop1").getValue());
    }

    @Test
    void testConfigTypeChange() {
        assertEquals(new HashMap<String, String>() {
            {
                put("p1", "pro");
                put("p2", "app");
                put("p3", "app");
                put("p4", "app");
            }
        }, asStringMap(provider.getProperties(byType("configType"), "dev")));

        assertEquals(new HashMap<String, String>() {
            {
                put("p1", "pro3");
                put("p2", "app2");
                put("p3", "app3");
            }
        }, asStringMap(provider.getProperties(byType("appType"), "dev")));
    }

    @Test
    void testPredefinedFunction() {
        assertEquals(new HashMap<String, String>() {
            {
                put("notFound", "");
                put("xmx", "0m");
                put("xmxLine", "Xmx100m");
            }
        }, asStringMap(provider.getProperties(byType("predefinedFunctions"), "dev")));
    }

    private void doTestAliases(String componentName, String ip) {
        Component component = byNameAndType(componentName, "node");
        assertEquals(ip, resolveValue("aliases", component, "ip"));
    }

    private String resolveValue(String env, Component component, String propName) {
        return resolver.resolve(property(component.getName() + "." + propName, "${this@" + propName + "}", env,
                new SpecialSource(component, "")), new EnvComponent(component, env));
    }
}