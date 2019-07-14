package Utils;

import java.net.*;
import java.util.*;

/**
 * Скопировано с сайта http://iliachemodanov.ru/ru/blog-ru/9-java/45-java-get-local-address-ru
 */

public class NetworkUtil {

    public static InetAddress getLocalAddress(){
        try {
            List<NetworkInterface> netInts = Collections.list(NetworkInterface.getNetworkInterfaces());
            if (netInts.size() == 1) {
                return InetAddress.getLocalHost();
            }

            for (NetworkInterface net : netInts) {
                if (!net.isLoopback() && !net.isVirtual() && net.isUp()) {
                    Enumeration<InetAddress> addrEnum = net.getInetAddresses();
                    while (addrEnum.hasMoreElements()) {
                        InetAddress addr = addrEnum.nextElement();
                        if (!addr.isLoopbackAddress() && !addr.isAnyLocalAddress()
                                && !addr.isLinkLocalAddress() && !addr.isMulticastAddress()
                        ) {
                            return addr;
                        }
                    }
                }
            }
            return null;
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<InetAddress> getLocalAddresses(){
        try {
            List<NetworkInterface> netInts = Collections.list(NetworkInterface.getNetworkInterfaces());
            if (netInts.size() == 1) {
                return Collections.singletonList(InetAddress.getLocalHost());
            }

            List<InetAddress> result = new ArrayList<>();
            for (NetworkInterface net : netInts) {
                if (!net.isLoopback() && !net.isVirtual() && net.isUp()) {
                    Enumeration<InetAddress> addrEnum = net.getInetAddresses();
                    while (addrEnum.hasMoreElements()) {
                        InetAddress addr = addrEnum.nextElement();
                        if (!addr.isLoopbackAddress() && !addr.isAnyLocalAddress()
                                && !addr.isLinkLocalAddress() && !addr.isMulticastAddress()
                        ) {
                            result.add(addr);
                        }
                    }
                }
            }
            return result;
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
            return null;
        }
    }
}