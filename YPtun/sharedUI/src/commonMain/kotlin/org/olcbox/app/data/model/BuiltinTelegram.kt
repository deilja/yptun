package org.olcbox.app.data.model

import org.olcbox.app.data.repository.LocationsRepository

/**
 * XFi "Telegram rescue": a built-in service location for the bootstrap problem — a new user whose
 * Telegram is blocked cannot reach the @XFI_VPN_bot to buy a subscription, because getting a
 * personal proxy from the bot requires Telegram to work in the first place. This profile breaks
 * the circle: it needs no subscription and no Telegram, and it can carry ONLY Telegram traffic.
 *
 * The fence is two-layered:
 * - the raw Xray config below routes Telegram domains / DC IP ranges to the bootstrap server and
 *   BLACKHOLES everything else, so even a rooted client cannot turn it into a free general VPN;
 * - on Android the VPN is additionally restricted to the Telegram apps themselves (split tunnel,
 *   see AndroidVpnManager), so not even Telegram-destined traffic from other apps gets in.
 *
 * The bootstrap server is a dedicated panel inbound on the XFi Finland host whose only job is
 * this: the uuid below is a SHARED service credential, published inside the APK by design. It
 * grants nothing beyond the fenced tunnel above; the user's real subscription is a different,
 * personal key issued by the bot.
 */
const val BUILTIN_TELEGRAM_ID = "builtin_telegram"

/** Display name of the built-in location (location list + active-location header). */
const val BUILTIN_TELEGRAM_NAME = "Telegram (служебный)"

private const val BOOTSTRAP_SERVER = "2.27.52.105"
private const val BOOTSTRAP_PORT = 443
private const val BOOTSTRAP_UUID = "c7025f9e-2386-4b3b-9cf9-32840cfbea9b"
private const val BOOTSTRAP_PUBLIC_KEY = "opE0oca0g9nA-gbA0J5G4AZXyNzP1H9-Og8D6tQ2J00"
private const val BOOTSTRAP_SHORT_ID = "eedf7739"
private const val BOOTSTRAP_SNI = "developers.cloudflare.com"

/**
 * Packages routed into the tunnel while the rescue profile is active (Android split tunnel,
 * ProxySelected). The two official Telegram clients plus Telegram X (org.thunderdog.challegram).
 */
val TELEGRAM_RESCUE_PACKAGES: List<String> = listOf(
    "org.telegram.messenger",
    "org.telegram.messenger.web",
    "org.thunderdog.challegram",
)

/**
 * The verbatim Xray config of the rescue profile. The endpoint is an IP literal on purpose: the
 * tunnel must come up without any DNS. DNS inside the tunnel goes to the XFi AdGuard Home
 * (2.27.52.105 — also [org.olcbox.app.data.model.TrafficSettings] remoteDns default), reached
 * THROUGH the tunnel via the dedicated port-53 rule; every non-Telegram destination is dropped
 * by the final blackhole rule. Kept free of geosite:/geoip: selectors so it always loads.
 */
private val BUILTIN_TELEGRAM_XRAY_JSON = """
{
  "log": {"loglevel": "warning"},
  "dns": {"servers": ["$BOOTSTRAP_SERVER"]},
  "inbounds": [{"tag":"socks-in","listen":"127.0.0.1","port":10808,"protocol":"socks","settings":{"auth":"noauth","udp":true},"sniffing":{"enabled":true,"destOverride":["http","tls","quic"]}}],
  "outbounds": [
    {"tag":"proxy","protocol":"vless","settings":{"vnext":[{"address":"$BOOTSTRAP_SERVER","port":$BOOTSTRAP_PORT,"users":[{"id":"$BOOTSTRAP_UUID","encryption":"none"}]}]},"streamSettings":{"network":"tcp","security":"reality","realitySettings":{"fingerprint":"chrome","serverName":"$BOOTSTRAP_SNI","publicKey":"$BOOTSTRAP_PUBLIC_KEY","shortId":"$BOOTSTRAP_SHORT_ID","spiderX":"/"}}},
    {"tag":"blocked","protocol":"blackhole"}
  ],
  "routing": {"domainStrategy":"IPIfNonMatch","rules":[
    {"type":"field","domain":["domain:telegram.org","domain:t.me","domain:telegram.me","domain:telesco.pe","domain:telegra.ph"],"outboundTag":"proxy"},
    {"type":"field","ip":["149.154.160.0/20","91.108.4.0/22","91.108.8.0/22","91.108.12.0/22","91.108.16.0/22","91.108.20.0/22","91.108.56.0/22","109.239.140.0/24","67.198.55.0/24","2001:67c:4e8::/48","2001:b28:f23d::/48","2001:b28:f23f::/48"],"outboundTag":"proxy"},
    {"type":"field","ip":["$BOOTSTRAP_SERVER/32"],"port":"53","outboundTag":"proxy"},
    {"type":"field","network":"tcp,udp","outboundTag":"blocked"}
  ]}
}
""".trimIndent()

/**
 * The built-in rescue location: a Standard location whose proxy is the verbatim config above —
 * the same shape the importer produces for a JSON subscription with its own routing
 * (rawXrayConfig + forced Xray core), so the runtime honors the embedded fence as-is.
 */
fun builtinTelegramLocation(): LocationConfig = LocationConfig(
    name = BUILTIN_TELEGRAM_NAME,
    engine = EngineType.Standard,
    proxy = ProxyProfile(
        tag = "XFi Telegram",
        type = ProxyProfile.TYPE_VLESS,
        server = BOOTSTRAP_SERVER,
        serverPort = BOOTSTRAP_PORT,
        rawXrayConfig = BUILTIN_TELEGRAM_XRAY_JSON,
    ),
    core = ProxyCore.Xray,
).normalized()

/**
 * Idempotent seeding: makes sure the rescue location exists in the user's bundle under
 * [BUILTIN_TELEGRAM_ID]. A no-op when it is already there and current; a stale seeded copy
 * (old service endpoint) is refreshed in place — the location is a published service profile,
 * not user data;
 * re-adds it when missing (first run, or the user deleted it). Seeding goes through
 * [LocationsRepository.saveLocation] — which also activates the saved location — so the
 * previously active location is restored right after, except on a truly empty bundle where
 * the rescue profile stays active (it is the only location anyway).
 */
suspend fun ensureBuiltinTelegramLocation(repository: LocationsRepository) {
    val bundle = repository.getBundle()
    val existing = bundle.locations.firstOrNull { it.storageId == BUILTIN_TELEGRAM_ID }
    if (existing != null) {
        // Refresh a stale seeded copy when the service endpoint moves (e.g. the
        // 2026-10-22 cutover: direct port 1444 -> nginx SNI routing on 443 with a
        // dedicated SNI). Without this, existing installs would keep dialing the
        // retired endpoint forever, because seeding alone never overwrites.
        val stale = existing.proxy?.serverPort != BOOTSTRAP_PORT ||
            existing.proxy?.rawXrayConfig?.contains(BOOTSTRAP_SNI) != true
        if (!stale) return
    }
    val previousActiveId = bundle.activeLocationId
    repository.saveLocation(BUILTIN_TELEGRAM_ID, builtinTelegramLocation())
    if (previousActiveId != null && previousActiveId != BUILTIN_TELEGRAM_ID) {
        repository.setActiveLocationId(previousActiveId)
    }
}
