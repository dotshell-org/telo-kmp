package com.pelotcl.app.generic.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pelotcl.app.R
import com.pelotcl.app.generic.data.config.AboutSectionData
import com.pelotcl.app.generic.data.config.ConsentConfigData
import com.pelotcl.app.generic.ui.theme.PrimaryColor
import com.pelotcl.app.generic.ui.theme.SecondaryColor

@Composable
fun TermsConsentScreen(
    consent: ConsentConfigData,
    legalSections: List<AboutSectionData>,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    var showPrivacyDetails by remember { mutableStateOf(false) }
    var hasAcknowledgedTerms by remember { mutableStateOf(false) }
    var hasAcknowledgedPrivacy by remember { mutableStateOf(false) }
    val acknowledgementText = consent.acknowledgementLabel
    val acknowledgementLinkText = consent.acknowledgementLinkText
    val privacyAcknowledgementText = consent.privacyAcknowledgementLabel
    val privacyAcknowledgementLinkText = consent.privacyAcknowledgementLinkText
    val acknowledgementAnnotated = remember(acknowledgementText, acknowledgementLinkText) {
        buildConsentAnnotated(acknowledgementText, acknowledgementLinkText, "TERMS")
    }
    val privacyAcknowledgementAnnotated = remember(
        privacyAcknowledgementText,
        privacyAcknowledgementLinkText
    ) {
        buildConsentAnnotated(
            privacyAcknowledgementText,
            privacyAcknowledgementLinkText,
            "PRIVACY"
        )
    }

    if (showDetails) {
        val privacySections = legalSections.filter { it.title == consent.privacySectionTitle }
        val detailsSections = if (showPrivacyDetails && privacySections.isNotEmpty()) {
            privacySections
        } else {
            legalSections
        }
        val detailsTitle = if (showPrivacyDetails) {
            consent.privacyDetailsTitle
        } else {
            consent.detailsTitle
        }
        TermsConsentDetailsScreen(
            title = detailsTitle,
            sections = detailsSections,
            onBackClick = {
                showDetails = false
                showPrivacyDetails = false
            },
            modifier = modifier
        )
        return
    }

    Scaffold(containerColor = PrimaryColor) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top section: Title and Paragraph (Scrollable, takes up remaining space)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo Pelo",
                    modifier = Modifier
                        .size(160.dp)
                        .padding(bottom = 20.dp)
                        .align(Alignment.CenterHorizontally) // Centers the logo horizontally
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = SecondaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = consent.title,
                        color = SecondaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = consent.intro,
                    color = SecondaryColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            // Bottom section: Checkboxes and Accept Button (Fixed at the bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                ConsentCheckboxRow(
                    checked = hasAcknowledgedTerms,
                    onCheckedChange = { hasAcknowledgedTerms = it },
                    annotatedText = acknowledgementAnnotated,
                    tag = "TERMS",
                    onLinkClick = {
                        showPrivacyDetails = false
                        showDetails = true
                    }
                )

                Spacer(Modifier.height(12.dp))

                ConsentCheckboxRow(
                    checked = hasAcknowledgedPrivacy,
                    onCheckedChange = { hasAcknowledgedPrivacy = it },
                    annotatedText = privacyAcknowledgementAnnotated,
                    tag = "PRIVACY",
                    onLinkClick = {
                        showPrivacyDetails = true
                        showDetails = true
                    }
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasAcknowledgedTerms && hasAcknowledgedPrivacy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryColor,
                        contentColor = PrimaryColor,
                        disabledContainerColor = SecondaryColor.copy(alpha = 0.4f),
                        disabledContentColor = PrimaryColor.copy(alpha = 0.7f)
                    )
                ) {
                    Text(consent.acceptLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ConsentCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    annotatedText: AnnotatedString,
    tag: String,
    onLinkClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = SecondaryColor,
                checkmarkColor = PrimaryColor,
                uncheckedColor = SecondaryColor.copy(alpha = 0.7f)
            )
        )
        Spacer(Modifier.size(8.dp))
        ClickableText(
            text = annotatedText,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                color = SecondaryColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            onClick = { offset ->
                annotatedText.getStringAnnotations(
                    tag = tag,
                    start = offset,
                    end = offset
                ).firstOrNull()?.let {
                    onLinkClick()
                }
            }
        )
    }
}

private fun buildConsentAnnotated(
    text: String,
    linkText: String,
    tag: String
): AnnotatedString {
    val linkStart = text.indexOf(linkText)
    return if (linkStart < 0) {
        buildAnnotatedString { append(text) }
    } else {
        buildAnnotatedString {
            append(text.substring(0, linkStart))
            pushStringAnnotation(tag = tag, annotation = "details")
            withStyle(
                SpanStyle(
                    color = SecondaryColor,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(linkText)
            }
            pop()
            append(text.substring(linkStart + linkText.length))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsConsentDetailsScreen(
    title: String,
    sections: List<AboutSectionData>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = SecondaryColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SecondaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor
                )
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
