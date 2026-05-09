package test;

import java.math.BigInteger;
import java.util.List;
import utils.NetworkUtil;

public class IPv6Test {

    public static void run() {
        TestRunner.section("IPv6 support in NetworkUtil");

        TestRunner.test("isIPv6 detecta correctament", () -> {
            TestRunner.assertTrue(NetworkUtil.isIPv6("::1"), "loopback");
            TestRunner.assertTrue(NetworkUtil.isIPv6("2001:db8::1"), "global unicast");
            TestRunner.assertTrue(NetworkUtil.isIPv6("fe80::1"), "link-local");
            TestRunner.assertTrue(!NetworkUtil.isIPv6("192.168.1.1"), "IPv4 no és IPv6");
        });

        TestRunner.test("ipv6ToBigInt + bigIntToIpv6 round-trip", () -> {
            try {
                String ip = "2001:db8::1";
                BigInteger n = NetworkUtil.ipv6ToBigInt(ip);
                TestRunner.assertTrue(n != null, "no null");
                String back = NetworkUtil.bigIntToIpv6(n);
                TestRunner.assertEquals(
                    java.net.InetAddress.getByName(ip),
                    java.net.InetAddress.getByName(back),
                    "round-trip"
                );
            } catch (java.net.UnknownHostException e) {
                throw new AssertionError(e.getMessage());
            }
        });

        TestRunner.test("rangIpsV6: rang petit", () -> {
            List<String> ips = NetworkUtil.rangIpsV6("::1", "::5");
            TestRunner.assertEquals(5, ips.size(), "5 IPs");
        });

        TestRunner.test("rangIpsV6: límit de seguretat (>4096)", () -> {
            List<String> ips = NetworkUtil.rangIpsV6("::1", "::1:0:0");
            TestRunner.assertTrue(ips.isEmpty(), "rebutja rangs grans");
        });

        TestRunner.test("smartRange auto-detecta família", () -> {
            List<String> v4 = NetworkUtil.smartRange("10.0.0.1", "10.0.0.3");
            List<String> v6 = NetworkUtil.smartRange("::1", "::3");
            TestRunner.assertEquals(3, v4.size(), "v4");
            TestRunner.assertEquals(3, v6.size(), "v6");
        });
    }

    public static void main(String[] args) {
        run();
        TestRunner.summary();
    }
}
