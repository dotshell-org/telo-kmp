package com.pelotcl.app.generic.ui.screens.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pelotcl.app.generic.data.config.AboutData
import com.pelotcl.app.generic.ui.theme.PrimaryColor
import com.pelotcl.app.generic.ui.theme.SecondaryColor

class GenericAboutScreen(private val data: AboutData) : AboutScreenContract {

    override val screenTitle: String = data.screenTitle

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun AboutScreenContent(
        onBackClick: () -> Unit,
        onCreditsClick: () -> Unit,
        onLegalClick: () -> Unit,
        onContactClick: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            color = SecondaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Retour",
                                tint = SecondaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
                )
            },
            containerColor = PrimaryColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                AboutMenuItem("Crédits", onCreditsClick)
                AboutMenuItem("Mentions légales / CGU", onLegalClick)
                AboutMenuItem("Contact", onContactClick)
            }
        }
    }

    @Composable
    private fun AboutMenuItem(text: String, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = SecondaryColor, fontSize = 18.sp)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, "Ouvrir", tint = SecondaryColor)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun CreditsScreenContent(
        onBackClick: () -> Unit,
        onDataSourceClick: () -> Unit,
        onApiSourceClick: () -> Unit
    ) {
        val sections = getCreditSections()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Crédits",
                            color = SecondaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Retour",
                                tint = SecondaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
                )
            },
            containerColor = PrimaryColor
        ) { paddingValues ->
            val uriHandler = LocalUriHandler.current

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                sections.forEach { section ->
                    Text(
                        text = section.title,
                        color = SecondaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = section.content,
                        color = SecondaryColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    section.links.forEach { link ->
                        ClickableLink(link.label, link.url, uriHandler)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun ClickableLink(label: String, url: String, uriHandler: UriHandler) {
        Row(
            modifier = Modifier
                .clickable { uriHandler.openUri(url) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color(0xFF4285F4), fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                "Ouvrir",
                tint = Color(0xFF4285F4),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun LegalScreenContent(onBackClick: () -> Unit) {
        val sections = getLegalSections()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Mentions légales / CGU",
                            color = SecondaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Retour",
                                tint = SecondaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
                )
            },
            containerColor = PrimaryColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                sections.forEach { section ->
                    Text(
                        text = section.title,
                        color = SecondaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = section.content,
                        color = SecondaryColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ContactScreenContent(onBackClick: () -> Unit) {
        val contactInfo = getContactInfo()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Contact",
                            color = SecondaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Retour",
                                tint = SecondaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
                )
            },
            containerColor = PrimaryColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                contactInfo.email?.let { email ->
                    ContactItem("Email", email, "mailto:$email")
                }

                contactInfo.website?.let { website ->
                    ContactItem("Site web", website, website)
                }

                if (contactInfo.socialMedia.isNotEmpty()) {
                    Text(
                        text = "Réseaux sociaux",
                        color = SecondaryColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    contactInfo.socialMedia.forEach { social ->
                        ContactItem(social.platform, "@${social.username}", social.url)
                    }
                }
            }
        }
    }

    @Composable
    private fun ContactItem(label: String, value: String, url: String) {
        val uriHandler = LocalUriHandler.current

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = label, color = SecondaryColor, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .clickable { uriHandler.openUri(url) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    color = Color(0xFF4285F4),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, "Ouvrir", tint = Color(0xFF4285F4))
            }
        }
    }

    override fun getCreditSections(): List<AboutScreenContract.CreditSection> {
        return data.sections.map { section ->
            AboutScreenContract.CreditSection(
                title = section.title,
                content = section.content,
                links = section.links.map { AboutScreenContract.CreditLink(it.label, it.url) }
            )
        }
    }

    override fun getLegalSections(): List<AboutScreenContract.LegalSection> {
        return data.legalSections.map { section ->
            AboutScreenContract.LegalSection(
                title = section.title,
                content = section.content
            )
        }
    }

    override fun getContactInfo(): AboutScreenContract.ContactInfo {
        return AboutScreenContract.ContactInfo(
            email = data.contact.email,
            website = data.contact.website,
            socialMedia = data.contact.socialMedia.map {
                AboutScreenContract.SocialMediaLink(it.platform, it.url, it.username)
            }
        )
    }
}
