package org.olcbox.app.data.model

import org.olcbox.app.data.repository.LocationsRepository

/**
 * XFi "YouTube direct" TEST profile: checks whether YouTube can work straight from the user's own
 * connection (their Russian residential/mobile IP), with no XFi server in the path at all.
 *
 * Why: YouTube shows no ads to Russian IPs, so today the production path detours YouTube through
 * the Moscow server (extra hop, server load, one more point of failure). If the phone itself can
 * defeat the local DPI, the detour becomes unnecessary: faster, and it works even when the XFi
 * servers are down.
 *
 * How: the raw Xray config below sends EVERYTHING out through the device's own network
 * (freedom/"direct"). YouTube domains go through a second freedom outbound with TLS ClientHello
 * fragmentation enabled ("frag"): the SNI is split across fragments, so SNI-based DPI cannot
 * classify the connection — the same idea as desktop DPI-bypass tools, within what a non-root
 * Android app can do (fake-packet desync needs raw sockets and is impossible here). YouTube QUIC
 * (UDP 443) is blocked so the app instantly falls back to TCP, where fragmentation applies.
 *
 * This is a TEST mode, not a VPN: while it is active there is no tunnel and no server protection
 * for any app. The home card says so. If the test proves the ISP lets fragmented YouTube through,
 * the same outbound can later be integrated into the generated configs of real profiles.
 */
const val BUILTIN_YOUTUBE_ID = "builtin_youtube_direct"

/** Display name of the built-in location (location list + active-location header). */
const val BUILTIN_YOUTUBE_NAME = "YouTube напрямую (тест)"

private val BUILTIN_YOUTUBE_XRAY_JSON = """
{
  "log": {"loglevel": "warning"},
  "dns": {"servers": ["1.1.1.1", "8.8.8.8"], "queryStrategy": "UseIPv4"},
  "inbounds": [{"tag":"socks-in","listen":"127.0.0.1","port":10808,"protocol":"socks","settings":{"auth":"noauth","udp":true},"sniffing":{"enabled":true,"destOverride":["http","tls","quic"]}}],
  "outbounds": [
    {"tag":"frag","protocol":"freedom","settings":{"domainStrategy":"UseIPv4","fragment":{"packets":"tlshello","length":"100-200","interval":"10-20"}}},
    {"tag":"direct","protocol":"freedom","settings":{"domainStrategy":"UseIPv4"}},
    {"tag":"blocked","protocol":"blackhole"}
  ],
  "routing": {"domainStrategy":"IPIfNonMatch","rules":[
    {"type":"field","domain":["domain:youtube.com","domain:googlevideo.com","domain:ytimg.com","domain:ggpht.com","domain:youtu.be","domain:youtube-nocookie.com","full:youtubei.googleapis.com","full:content-youtube.googleapis.com"],"network":"udp","port":"443","outboundTag":"blocked"},
    {"type":"field","domain":["domain:youtube.com","domain:googlevideo.com","domain:ytimg.com","domain:ggpht.com","domain:youtu.be","domain:youtube-nocookie.com","full:youtubei.googleapis.com","full:content-youtube.googleapis.com"],"outboundTag":"frag"},
    {"type":"field","network":"tcp,udp","outboundTag":"direct"}
  ]}
}
""".trimIndent()

/**
 * The built-in test location: a Standard location whose proxy is the verbatim config above
 * (rawXrayConfig + forced Xray core), the same shape as the Telegram rescue profile. The
 * server/port fields are placeholders — the raw config is what actually runs and it dials out
 * directly, there is no server.
 */
fun builtinYoutubeLocation(): LocationConfig = LocationConfig(
    name = BUILTIN_YOUTUBE_NAME,
    engine = EngineType.Standard,
    proxy = ProxyProfile(
        tag = "XFi YouTube Direct",
        type = ProxyProfile.TYPE_VLESS,
        server = "1.1.1.1",
        serverPort = 443,
        rawXrayConfig = BUILTIN_YOUTUBE_XRAY_JSON,
    ),
    core = ProxyCore.Xray,
).normalized()

/**
 * Idempotent seeding, mirroring [ensureBuiltinTelegramLocation]: the location exists in the
 * user's bundle under [BUILTIN_YOUTUBE_ID]; a no-op when already present, re-added when the
 * user deleted it. The previously active location is restored after seeding (saveLocation
 * activates what it saves), except on a truly empty bundle.
 */
suspend fun ensureBuiltinYoutubeLocation(repository: LocationsRepository) {
    val bundle = repository.getBundle()
    if (bundle.locations.any { it.storageId == BUILTIN_YOUTUBE_ID }) return
    val previousActiveId = bundle.activeLocationId
    repository.saveLocation(BUILTIN_YOUTUBE_ID, builtinYoutubeLocation())
    if (previousActiveId != null && previousActiveId != BUILTIN_YOUTUBE_ID) {
        repository.setActiveLocationId(previousActiveId)
    }
}
