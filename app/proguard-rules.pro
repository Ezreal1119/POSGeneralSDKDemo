# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn io.netty.channel.epoll.Epoll
-dontwarn io.netty.channel.epoll.EpollEventLoopGroup
-dontwarn io.netty.channel.epoll.EpollSocketChannel
-dontwarn io.netty.handler.codec.http.FullHttpResponse
-dontwarn io.netty.handler.codec.http.HttpClientCodec
-dontwarn io.netty.handler.codec.http.HttpHeaders
-dontwarn io.netty.handler.codec.http.HttpObjectAggregator
-dontwarn io.netty.handler.codec.http.HttpRequest
-dontwarn io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
-dontwarn io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
-dontwarn io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame
-dontwarn io.netty.handler.codec.http.websocketx.PingWebSocketFrame
-dontwarn io.netty.handler.codec.http.websocketx.PongWebSocketFrame
-dontwarn io.netty.handler.codec.http.websocketx.TextWebSocketFrame
-dontwarn io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
-dontwarn io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
-dontwarn io.netty.handler.codec.http.websocketx.WebSocketFrame
-dontwarn io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
-dontwarn io.netty.handler.codec.http.websocketx.WebSocketVersion
-dontwarn io.netty.handler.proxy.HttpProxyHandler
-dontwarn io.netty.handler.proxy.ProxyHandler
-dontwarn io.netty.handler.proxy.Socks4ProxyHandler
-dontwarn io.netty.handler.proxy.Socks5ProxyHandler
-dontwarn io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.AsyncTask
-dontwarn io.netty.internal.tcnative.Buffer
-dontwarn io.netty.internal.tcnative.CertificateCallback
-dontwarn io.netty.internal.tcnative.CertificateCompressionAlgo
-dontwarn io.netty.internal.tcnative.CertificateVerifier
-dontwarn io.netty.internal.tcnative.Library
-dontwarn io.netty.internal.tcnative.SSL
-dontwarn io.netty.internal.tcnative.SSLContext
-dontwarn io.netty.internal.tcnative.SSLPrivateKeyMethod
-dontwarn io.netty.internal.tcnative.SSLSessionCache
-dontwarn io.netty.internal.tcnative.SessionTicketKey
-dontwarn io.netty.internal.tcnative.SniHostNameMatcher
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.apache.logging.log4j.Level
-dontwarn org.apache.logging.log4j.LogManager
-dontwarn org.apache.logging.log4j.Logger
-dontwarn org.apache.logging.log4j.message.MessageFactory
-dontwarn org.apache.logging.log4j.spi.ExtendedLogger
-dontwarn org.apache.logging.log4j.spi.ExtendedLoggerWrapper
-dontwarn org.eclipse.jetty.alpn.ALPN$ClientProvider
-dontwarn org.eclipse.jetty.alpn.ALPN$Provider
-dontwarn org.eclipse.jetty.alpn.ALPN$ServerProvider
-dontwarn org.eclipse.jetty.alpn.ALPN
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ClientProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$Provider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ServerProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego

# UROVO SDK proguard-rules
-ignorewarnings

-dontwarn com.google.zxing.**
-keep class com.google.zxing.**{*;}
-keep interface com.google.zxing.**{*;}

-dontwarn com.imagealgorithm.**
-keep class com.imagealgorithm.**{*;}
-keep interface com.imagealgorithm.**{*;}

-dontwarn com.imagealgorithmlab.barcode.**
-keep class com.imagealgorithmlab.barcode.**{*;}
-keep interface com.imagealgorithmlab.barcode.**{*;}

-dontwarn com.jniexport.**
-keep class com.jniexport.**{*;}
-keep interface com.jniexport.**{*;}

-dontwarn com.urovo.**
-keep class com.urovo.**{*;}
-keep interface com.urovo.**{*;}

-dontwarn android.content.pm.**
-keep class android.content.pm.**{*;}
-keep interface android.content.pm.**{*;}

-dontwarn android.device.**
-keep class android.device.**{*;}
-keep interface android.device.**{*;}

-dontwarn android.os.**
-keep class android.os.**{*;}
-keep interface android.os.**{*;}

-dontwarn com.android.device.**
-keep class com.android.device.**{*;}
-keep interface com.android.device.**{*;}

-dontwarn android.udroid.content.pm.**
-keep class android.udroid.content.pm.**{*;}
-keep interface android.udroid.content.pm.**{*;}

-dontwarn com.ubx.**
-keep class com.ubx.**{*;}
-keep interface com.ubx.**{*;}

-dontwarn com.android.hw.**
-keep class com.android.hw.**{*;}
-keep interface com.android.hw.**{*;}