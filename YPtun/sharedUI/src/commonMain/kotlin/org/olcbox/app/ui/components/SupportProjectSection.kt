package org.olcbox.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.olcbox.app.DonationInfo
import org.olcbox.app.ui.i18n.LocalStrings

private val VpnBlue = Color(0xFF2AABEE)
private val GithubStarGold = Color(0xFFE3B341)

/**
 * "Support" block for the settings screen: the XFi bot (subscriptions and support) and the
 * project repository. Platform-neutral — the host passes how to open links. The [onCopyAddress]
 * callback is kept for API compatibility with the platform settings screens.
 */
@Composable
fun SupportProjectSection(
    onCopyAddress: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val s = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text(s.donate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        s.supportProjectThanks,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(2.dp))



            SupportOption(
                icon = Icons.Rounded.VpnKey,
                accent = VpnBlue,
                title = s.supportVpnSub,
                subtitle = s.supportVpnSubHint,
                trailing = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = { onOpenUrl(DonationInfo.VPN_BOT_URL) }
            )

            SupportOption(
                icon = Icons.Rounded.Star,
                accent = GithubStarGold,
                title = s.supportGithubStar,
                subtitle = s.supportGithubStarHint,
                trailing = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = { onOpenUrl(DonationInfo.GITHUB_URL) }
            )
        }
    }
}

@Composable
private fun SupportOption(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    trailing: ImageVector,
    onClick: () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                extra()
            }
            Icon(trailing, null, tint = accent, modifier = Modifier.size(20.dp))
        }
    }
}
